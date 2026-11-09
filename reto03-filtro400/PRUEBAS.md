# PRUEBAS

## Tests unitarios

Se mantienen los tests de las fases anteriores y se añaden:

- `HorarioTest`: franjas normales y nocturnas, límites, días concretos, máscara de días y próxima ocurrencia.
- `MotorHorarioTest`: reglas con horario, No molestar, lista blanca, prioridad, normalización de números y llamadas ocultas.
- `MigracionV3V4Test` (instrumentado): conserva prefijos, números, lista blanca y registro al migrar de v3 a v4, y crea las columnas/tablas nuevas.
- `RegistroLlamadasPrivacidadTest` (instrumentado): comprueba que las acciones rápidas conservan el número hasta las 48 horas, lo purgan al llegar al límite y no exponen el número completo como valor de regla.

Semántica de horarios:

- Inicio inclusivo y fin exclusivo: una franja 09:00-18:00 bloquea a las 09:00 y deja de bloquear a las 18:00.
- Si inicio es mayor que fin, la franja cruza la medianoche. Ejemplo 22:00-08:00 bloquea a las 23:30 y a las 07:00, pero no a las 12:00.
- Si inicio es igual a fin, se interpreta como todo el día en los días seleccionados.

## Ejecutar tests

Desde `reto03-filtro400/`:

```bash
./gradlew testDebugUnitTest assembleDebugAndroidTest
```

Si Gradle usa un JDK sin `jlink`, en Windows:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebugAndroidTest --console=plain --no-configuration-cache "-Dorg.gradle.java.home=C:\Program Files\Android\Android Studio\jbr"
```

## Casos de prueba en el Simulador

El Simulador permite elegir fecha y hora, por lo que no hace falta esperar a una franja real.

| Caso | Configuración | Número | Fecha y hora | Resultado esperado |
|---|---|---|---|---|
| Prefijo dentro de horario | Prefijo 400, lunes a viernes 09:00-18:00 | 400123456 | Lunes 12:00 | Bloqueada, tipo Horario |
| Prefijo fuera de horario | Igual que el anterior | 400123456 | Lunes 20:00 | Permitida |
| Límite de inicio | Igual que el anterior | 400123456 | Lunes 09:00 | Bloqueada |
| Límite de fin | Igual que el anterior | 400123456 | Lunes 18:00 | Permitida |
| Franja nocturna | Prefijo o No molestar 22:00-08:00 | 400123456 | 23:30 | Bloqueada |
| Franja nocturna, madrugada | Igual que el anterior | 400123456 | 07:00 | Bloqueada |
| Franja nocturna, mediodía | Igual que el anterior | 400123456 | 12:00 | Permitida |
| Número con país | Número bloqueado 612345678 | +34 612 345 678 | Cualquiera | Bloqueada |
| Número con 0034 | Número bloqueado 612345678 | 0034612345678 | Cualquiera | Bloqueada |
| Lista blanca gana a No molestar | Lista blanca 612345678 y franja activa | 612345678 | Dentro de franja | Permitida, tipo Lista blanca |
| No molestar apagado | Franja activa, interruptor global apagado | 612345678 | Dentro de franja | Permitida |
| Franja inactiva | Franja desactivada, No molestar encendido | 612345678 | Dentro de franja | Permitida |
| Regla Permitir | Prefijo 400 con acción Permitir y No molestar activo | 400123456 | Dentro de franja | Permitida, tipo Prefijo |
| Número bloqueado gana a No molestar | Número bloqueado y franja activa | 612345678 | Dentro de franja | Bloqueada, tipo Número |
| Ocultos | Ajuste “Bloquear números ocultos” desactivado y No molestar apagado | vacío | Cualquiera | Permitida, tipo Oculto |
| Ocultos | Ajuste “Bloquear números ocultos” activado | vacío | Cualquiera | Bloqueada, tipo Oculto |
| Ocultos con No molestar | Ajuste de ocultos desactivado y No molestar activo | vacío | Dentro de franja | Bloqueada, tipo No molestar |

El simulador guarda el resultado como simulación. En Estadísticas no cuenta salvo que se active “Incluir simulaciones”.

## Prueba en dispositivo real

1. Activar Opciones de desarrollador y Depuración USB. Instalar con `./gradlew installDebug` o copiar el APK a `app/build/outputs/apk/debug/` y abrirlo en el teléfono.
2. Abrir Centinela y conceder el rol “Filtro de llamadas” desde la tarjeta de Inicio.
3. Desde otro teléfono cuyo número no esté en la agenda, llamar al teléfono de prueba:
   - número bloqueado;
   - número que coincide con un prefijo;
   - llamada dentro y fuera de un horario;
   - número de la lista blanca durante una franja de No molestar.
4. Revisar Registro y Estadísticas: decisión, tipo de regla y número enmascarado.
5. Recordar que los contactos guardados no pasan por Centinela: siempre suenan aunque coincidan con una regla.

## Instrumentado

`MigracionV3V4Test` se puede ejecutar con:

```bash
./gradlew connectedDebugAndroidTest
```

`RegistroLlamadasPrivacidadTest` también se ejecuta con ese comando. Requiere un emulador o dispositivo conectado.

La purga de números temporales se ejecuta al registrar o consultar el historial y mediante un trabajo persistente de Android cada 15 minutos. Android puede retrasar trabajos en segundo plano; por ello, cualquier lectura del registro vuelve a purgar antes de mostrar las acciones rápidas.
