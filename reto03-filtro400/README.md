# Centinela — filtro de llamadas

Centinela es una aplicación Android que filtra llamadas entrantes de números que Android entrega al servicio de selección de llamadas. Permite configurar prefijos y números bloqueados, excepciones permitidas, horarios y un modo No molestar. El nombre del proyecto es `filtro400`; el prefijo `400` se añade como regla inicial.

> **Alcance importante:** Centinela no identifica por sí sola quién llama ni consulta una base de datos de spam en Internet. El filtrado actual se basa en las reglas locales que configura el usuario. Las llamadas de números guardados en contactos no llegan al filtro de esta aplicación.

## Respuestas

### 1. ¿Qué es `CallScreeningService` y qué función cumple en Android?

`CallScreeningService` es una API de Android Telecom para que una aplicación seleccionada por el usuario evalúe llamadas que el sistema le entrega antes de que se presenten normalmente. Android invoca `onScreenCall()` y la aplicación puede responder si permite o restringe la llamada.

En Centinela, `FiltroLlamadasService` hereda de esa clase. Para llamadas entrantes consulta las reglas locales y responde mediante `respondToCall()`. Si la regla decide bloquear, la respuesta marca la llamada como no permitida y puede rechazarla o suprimir su notificación según los ajustes. El servicio responde antes de escribir el historial, para no esperar a la base de datos.

El servicio está declarado en `AndroidManifest.xml` con la acción `android.telecom.CallScreeningService` y el permiso de vinculación `BIND_SCREENING_SERVICE`. En esta implementación solo se evalúan llamadas entrantes.

### 2. ¿Por qué necesitamos solicitar el rol `ROLE_CALL_SCREENING`?

Android reserva el filtrado de llamadas para una aplicación a la que el usuario haya concedido el rol de filtrado. El rol `ROLE_CALL_SCREENING` permite al sistema reconocer a Centinela como la aplicación autorizada para recibir las llamadas que debe evaluar.

La app muestra la solicitud desde Inicio mediante `RoleManager`. El usuario confirma el cambio en la interfaz del sistema; instalar o abrir Centinela no concede el rol automáticamente. Sin el rol, Android no usa el servicio de Centinela para filtrar llamadas.

### 3. ¿Por qué no necesitamos un servidor externo para realizar el filtrado?

Las reglas, listas y horarios se guardan en la base de datos SQLite local (`filtros.db`), y los ajustes sencillos se guardan en preferencias locales. `MotorFiltrado` compara el número recibido con esos datos directamente en el dispositivo. El manifiesto no solicita permiso de Internet y el funcionamiento principal no depende de una conexión, una API ni un servidor.

Esto permite filtrar sin enviar números telefónicos a un servicio remoto. La importación y exportación de reglas usa archivos CSV mediante los selectores de archivos de Android; no implica por sí sola el uso de un servidor.

### 4. ¿Qué ocurre si el número telefónico está guardado en los contactos?

Android no entrega esas llamadas al servicio de selección de Centinela en la configuración actual. La aplicación no solicita permiso para leer contactos, y su motor de reglas no comprueba la agenda. Por tanto, una llamada de un contacto guardado no se bloquea por las reglas de Centinela y puede sonar normalmente, aunque su número coincida con un prefijo o número bloqueado.

Es una limitación importante que debe explicarse en la demostración. El comportamiento depende de cómo Android y el dispositivo apliquen el filtrado de llamadas.

### 5. ¿Qué diferencias existen entre comprobar un número mediante una función y bloquear realmente una llamada?

Comprobar un número es una decisión lógica. Por ejemplo, `MotorFiltrado.evaluar()` recibe un número, reglas y un momento, y devuelve una decisión. El Simulador usa esta lógica para mostrar qué ocurriría con una llamada; no inicia, rechaza ni modifica ninguna llamada telefónica.

Para bloquear una llamada real hacen falta además las condiciones del sistema: que Android entregue la llamada al servicio, que Centinela tenga el rol concedido y que `FiltroLlamadasService` responda a Android con `respondToCall()` y una `CallResponse` que no permita la llamada. En esta respuesta, la app también configura el rechazo/desvío al buzón y la omisión de la notificación según sus ajustes.

En resumen: **el motor decide; el servicio comunica esa decisión a Android; Android aplica la respuesta a la llamada real**. Si falla la evaluación de reglas, el servicio registra el error y permite la llamada para no dejarla sin respuesta.

### 6. ¿Qué mejoras incorporarías en una futura versión?

**Implementación de IA para ayudar a detectar llamadas de spam**, como mejora futura: podría analizar señales disponibles en el dispositivo y estimar si una llamada parece no deseada. La IA no forma parte de la versión actual: ahora Centinela aplica reglas locales explícitas (listas, prefijos, prioridad y horarios).

La incorporaría de forma gradual y transparente: preferiblemente con inferencia en el dispositivo para proteger la privacidad; activación opcional; explicación de los factores de la clasificación; y un umbral conservador para no bloquear automáticamente llamadas legítimas. Se podría empezar mostrando una advertencia o recomendación y permitir que el usuario corrija el resultado. También serían mejoras útiles ampliar las pruebas en dispositivos reales y documentar las diferencias entre versiones y fabricantes de Android.

## Análisis del proyecto

### Cómo funciona el filtrado

El flujo principal es:

1. El usuario concede a Centinela el rol de filtrado desde la pantalla Inicio.
2. Android invoca `FiltroLlamadasService` para una llamada que corresponde evaluar.
3. El servicio lee los ajustes y las reglas activas de SQLite.
4. `MotorFiltrado` normaliza el número y aplica las reglas en este orden:
   1. Número vacío u oculto: permitir o bloquear según el ajuste; también puede aplicarse No molestar.
   2. Lista blanca exacta: permitir siempre.
   3. Número bloqueado exacto, si su horario está activo: bloquear.
   4. Prefijos activos, respetando horarios. Se prioriza la prioridad mayor; en empate, el prefijo más específico.
   5. Modo No molestar global, si está activado y coincide con una franja.
   6. Si no hay coincidencias, permitir.
5. El servicio responde a Android y después, si corresponde, guarda el registro y muestra una notificación opcional sin el número completo.

Los números se normalizan antes de compararse: se eliminan separadores y los prefijos de país `+34` y `0034`. Una regla de prefijo puede bloquear o permitir. Los horarios admiten días seleccionados, franjas que cruzan medianoche y límites con inicio inclusivo y fin exclusivo.

### Organización y tecnologías

- **Plataforma:** Android, Kotlin, Gradle y Jetpack Compose con Material 3.
- **Interfaz:** navegación entre Inicio, Reglas, Horarios, Registro y Más. En Más están Estadísticas, Simulador, Importar/Exportar, Ajustes y Acerca de.
- **Dominio:** `MotorFiltrado` y `Horario` contienen la lógica de decisión y horarios, separada de la interfaz y de Android.
- **Datos:** `SQLiteOpenHelper` gestiona una base local versionada; repositorios encapsulan filtros, números, listas, horarios y registros. Los ajustes ligeros se guardan en preferencias.
- **Servicio Android:** `FiltroLlamadasService` integra la decisión del dominio con `CallScreeningService`.
- **Pruebas:** pruebas unitarias para reglas y horarios, además de pruebas instrumentadas para migraciones SQLite y privacidad del historial. Las pruebas instrumentadas necesitan un emulador o dispositivo conectado.
- **SDK configurado:** `minSdk 29`, `targetSdk 37` y `compileSdk 37`.

### Privacidad y límites que conviene mencionar

- El filtrado funciona localmente; no hay servidor externo ni IA en la versión actual.
- No se solicita permiso de contactos. Por eso los contactos guardados no pasan por el filtro.
- La interfaz y las estadísticas muestran el número enmascarado. El número completo se conserva temporalmente para acciones rápidas del Registro y se elimina a las 48 horas; el historial se limita a 30 días o 500 registros.
- Las notificaciones de bloqueo son opcionales y no muestran el número.
- El Simulador prueba la lógica, pero no sustituye una llamada celular real. La verificación del bloqueo real debe hacerse en un dispositivo y con el rol concedido.
- Las llamadas ocultas dependen de la información que entreguen Android, el fabricante y la operadora.

## Guía para el vídeo

La guía con la propuesta de escenas y el prompt completo para generar el guion con Claude está en [GUIA_VIDEO.md](GUIA_VIDEO.md).

## Ejecutar y probar

Con Android SDK y Android Studio instalados, abre PowerShell en esta carpeta y ejecuta:

```powershell
powershell -ExecutionPolicy Bypass -File .\run-filtro400.ps1
```

El script inicia el AVD `Medium_Phone_API_37.0`, espera a que Android arranque, compila e instala la app y la abre. Si el emulador ya está iniciado, reutiliza un emulador conectado.

Para elegir otro AVD instalado:

```powershell
powershell -ExecutionPolicy Bypass -File .\run-filtro400.ps1 -AvdName "Nombre del AVD"
```

Consulta los nombres disponibles con:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -list-avds
```

El script requiere un JDK completo, Android SDK con `platform-tools` y `emulator`, y un AVD creado en Android Studio. Si el SDK no está en la ruta habitual, define `ANDROID_SDK_ROOT`.

Para compilar y ejecutar las pruebas unitarias:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest
```

Si Gradle usa un JDK sin `jlink`, añade el JBR de Android Studio:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest --console=plain --no-configuration-cache "-Dorg.gradle.java.home=C:\Program Files\Android\Android Studio\jbr"
```

Para ejecutar también las pruebas instrumentadas, hace falta un emulador o dispositivo conectado:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

El APK de depuración queda en `app/build/outputs/apk/debug/`.

Más información: [PRUEBAS.md](PRUEBAS.md), [DECISIONES_FASE2.md](DECISIONES_FASE2.md) y [DECISIONES_FASE4.md](DECISIONES_FASE4.md).
