# Decisiones técnicas - Fase 2

## Estructura

- `data/`: modelo `Filtro`, `FiltroDbHelper` y `FiltroRepository`.
- `ui/`: `PantallaFiltros`, `FiltroItem` y `DialogoFiltro`.
- `ReglaBloqueo` sigue siendo lógica pura, sin dependencias de Android.

## SQLiteOpenHelper en lugar de Room

El enunciado lo exige y además la app solo tiene una tabla y cinco operaciones.
Room habría añadido KSP/kapt, dependencias y tiempo de compilación sin aportar
nada en este tamaño. `FiltroDbHelper` crea el esquema y versiona la BD
(`VERSION = 1`); las consultas son SQL directo y acotado.

## Semilla del prefijo 400

Se inserta en `onCreate`, es decir, solo al crear la BD. Así el primer arranque
mantiene el comportamiento de la Fase 1, pero si el usuario borra todos los
filtros de forma intencionada no reaparece el 400 por sorpresa.

Si la BD no tiene filtros activos, el servicio permite todas las llamadas.

## UI sin ViewModel

Se usa `remember` + `rememberCoroutineScope` + repositorio. Es la opción más
sencilla: una sola pantalla, estado pequeño y sin dependencias nuevas. Las
operaciones del repositorio son `suspend` y usan `Dispatchers.IO`, así que el
hilo principal no se bloquea. Contrapartida: al rotar la pantalla se pierde el
estado efímero (diálogo abierto); la lista se recarga sola desde la BD. Si la
pantalla creciera, el siguiente paso sería un ViewModel.

## Cómo accede el servicio a los filtros

`onScreenCall` no es `suspend`, así que el repositorio expone
`obtenerActivosBloqueante()`. En cada llamada se consultan solo las filas con
`activo = 1` y se pasan a `ReglaBloqueo.debeBloquearse(numero, prefijos)`.
La consulta es diminuta y se hace en el hilo del servicio.

Como no hay caché, cualquier cambio hecho en la UI (añadir, editar, activar o
eliminar) afecta a la siguiente llamada sin reiniciar la app ni el servicio.

## Normalización y duplicados

Los prefijos se guardan normalizados con `ReglaBloqueo.normalizar` (quita
espacios/guiones y el prefijo país `+34`/`0034`). El diálogo valida antes de
guardar (no vacío, solo dígitos con `+` opcional, no duplicado), pero la
restricción `UNIQUE` de SQLite sigue siendo la garantía final. Si salta, el
repositorio devuelve `Resultado.Duplicado` en lugar de lanzar la excepción y el
diálogo muestra el error.

## Robustez y privacidad

- Número nulo, vacío u oculto: se permite la llamada.
- Cualquier excepción de BD: se registra solo el tipo de error y se permite.
- `respondToCall` se ejecuta siempre en un bloque `finally`.
- Se eliminó el `Log.d` que imprimía el número de teléfono; los logs actuales no
  contienen datos personales.

## Dificultades encontradas

- El build usaba un JRE de la extensión de Java de VS Code sin `jlink`, lo que
  rompía `JdkImageTransform`. Se resolvió ejecutando Gradle con
  `JAVA_HOME` apuntando a JDK 25 y `--no-configuration-cache`.
- No había emulador ni dispositivo conectado, así que los tests instrumentados
  se compilaron (`assembleDebugAndroidTest`) pero no se pudieron ejecutar.
- `kotlinx-coroutines` ya estaba disponible de forma transitiva a través de
  Compose/AndroidX; no fue necesario añadir dependencias al version catalog.
