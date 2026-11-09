param(
    [string]$AvdName = "Medium_Phone_API_37.0"
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$sdkRoot = $env:ANDROID_SDK_ROOT
if (-not $sdkRoot) {
    $sdkRoot = $env:ANDROID_HOME
}
if (-not $sdkRoot) {
    $sdkRoot = Join-Path $env:LOCALAPPDATA "Android\Sdk"
}

$adb = Join-Path $sdkRoot "platform-tools\adb.exe"
$emulator = Join-Path $sdkRoot "emulator\emulator.exe"
if (-not (Test-Path $adb) -or -not (Test-Path $emulator)) {
    throw "No encuentro Android SDK en '$sdkRoot'. Define ANDROID_SDK_ROOT con la ruta del SDK."
}

$availableAvds = @(& $emulator -list-avds)
if ($LASTEXITCODE -ne 0 -or $AvdName -notin $availableAvds) {
    throw "No encuentro el AVD '$AvdName'. AVD disponibles: $($availableAvds -join ', '). Usa -AvdName para elegir otro."
}

$javaCandidates = @()
if ($env:JAVA_HOME) {
    $javaCandidates += $env:JAVA_HOME
}
$javaCandidates += @(
    "C:\Program Files\Android\Android Studio\jbr",
    "C:\Program Files\Java\jdk-17",
    "C:\Program Files\Java\jdk-21"
)
$javaHome = $javaCandidates | Where-Object {
    (Test-Path (Join-Path $_ "bin\java.exe")) -and
    (Test-Path (Join-Path $_ "bin\jlink.exe"))
} | Select-Object -First 1
if (-not $javaHome) {
    throw "No encuentro un JDK completo con java.exe y jlink.exe. Instala JDK 17 o configura JAVA_HOME."
}
$env:JAVA_HOME = $javaHome
$env:PATH = "$(Join-Path $javaHome 'bin');$env:PATH"

$deviceLines = @(& $adb devices)
$runningEmulator = $deviceLines | Where-Object { $_ -match '^emulator-\d+\s+device$' } | Select-Object -First 1
$emulatorProcess = $null
if ($runningEmulator) {
    $serial = ($runningEmulator -split '\s+')[0]
    Write-Host "Usando el emulador iniciado: $serial"
} else {
    Write-Host "Iniciando el emulador '$AvdName'..."
    $emulatorProcess = Start-Process -FilePath $emulator -ArgumentList @("-avd", $AvdName) -PassThru
    $serial = $null
    $deadline = (Get-Date).AddMinutes(5)
    while ((Get-Date) -lt $deadline -and -not $serial) {
        Start-Sleep -Seconds 2
        if ($emulatorProcess.HasExited) {
            throw "El emulador se cerró durante el arranque. Comprueba la aceleración virtual y ejecuta 'emulator -avd $AvdName' para ver el error."
        }
        $deviceLines = @(& $adb devices)
        $runningEmulator = $deviceLines | Where-Object { $_ -match '^emulator-\d+\s+device$' } | Select-Object -First 1
        if ($runningEmulator) {
            $serial = ($runningEmulator -split '\s+')[0]
        }
    }
    if (-not $serial) {
        throw "El emulador no apareció en ADB tras 5 minutos. Estado actual: $($deviceLines -join '; ')"
    }
}

Write-Host "Esperando a que Android termine de iniciar..."
$deadline = (Get-Date).AddMinutes(5)
while ((Get-Date) -lt $deadline) {
    $bootState = & $adb -s $serial shell getprop sys.boot_completed 2>$null
    if (($bootState -join "").Trim() -eq "1") {
        break
    }
    Start-Sleep -Seconds 2
}
if (($bootState -join "").Trim() -ne "1") {
    throw "Android no terminó de iniciar en '$serial' tras 5 minutos."
}

Write-Host "Compilando e instalando Filtro 400..."
& ".\gradlew.bat" --no-configuration-cache --console=plain -s ":app:installDebug"
if ($LASTEXITCODE -ne 0) {
    throw "Falló la compilación o instalación de Filtro 400."
}

& $adb -s $serial shell am force-stop com.example.filtro400
& $adb -s $serial shell am start -n com.example.filtro400/.MainActivity
if ($LASTEXITCODE -ne 0) {
    throw "La app se instaló, pero no se pudo abrir en '$serial'."
}

Write-Host "Filtro 400 está ejecutándose en el emulador."
