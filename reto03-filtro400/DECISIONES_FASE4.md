# Decisiones de la Fase 4

## Horarios

- Se reutiliza un único modelo `Horario` para reglas, números bloqueados y franjas de No molestar.
- Los días se guardan como máscara de bits (`dias_mask`) y las horas como minutos desde medianoche (`hora_inicio`, `hora_fin`).
- Inicio inclusivo, fin exclusivo. Así una franja 09:00-18:00 empieza a las 09:00 y termina justo a las 18:00.
- Si inicio es mayor que fin, la franja cruza la medianoche. Para una franja nocturna, el día seleccionado es el día en que empieza.
- Si inicio es igual a fin, se interpreta como todo el día.
- Sin horario, las tres columnas son `NULL` y la regla se aplica siempre. En No molestar la franja es obligatoria.

## Orden de evaluación

`MotorFiltrado` mantiene este orden:

1. Números ocultos. Si el ajuste está activado se bloquean; si no, pueden seguir cayendo en No molestar, pero nunca coinciden con listas ni prefijos.
2. Lista blanca, que siempre permite.
3. Números bloqueados y prefijos, ordenados por prioridad, prefijo más largo e id. Cada regla respeta su horario.
4. Modo No molestar global, si está activado y alguna franja activa contiene el momento.
5. Permitir.

La lista blanca gana siempre. Las reglas de prefijo con acción Permitir también ganan a No molestar por el orden anterior. El motor recibe `LocalDateTime` como parámetro y nunca lee el reloj internamente, lo que permite probar cualquier fecha y hora. El servicio captura el momento con la zona horaria local del dispositivo y se lo pasa al motor.

## Base de datos y migración

- Versión 4 de SQLite. Se añaden `dias_mask`, `hora_inicio` y `hora_fin` a `filtros`, `numeros_bloqueados` y `lista_blanca`.
- Se crea `franjas_no_molestar` y la columna `registro_llamadas.numero_completo`.
- La migración conserva todos los datos de v3 y añade las columnas con `NULL`. Si se viene de v1 o v2, las tablas nuevas se crean ya con el esquema definitivo para no duplicar columnas.
- El interruptor global de No molestar se guarda en SharedPreferences (`ajustes_filtro400`), compartido por UI y servicio. No se creó una tabla de ajustes porque el dato es una preferencia simple.

## Privacidad del número completo

- La UI y las estadísticas usan siempre el número enmascarado.
- Los valores de regla que coinciden con el número completo tampoco se guardan en `regla_valor`; el acceso rápido usa exclusivamente `numero_completo`.
- `numero_completo` se guarda solo para poder ofrecer “Bloquear este número” o “Añadir a lista blanca” desde Registro.
- Se borra al cumplir 48 horas al registrar o consultar el historial y mediante un trabajo persistente de Android programado cada 15 minutos. Android puede retrasar trabajos en segundo plano, así que la lectura vuelve a purgar antes de ofrecer las acciones rápidas.
- Además, el historial se limita a 30 días o 500 registros.
- Los números completos no se escriben en logs ni en notificaciones.

## Rediseño

- Nombre visible “Centinela”, sin cambiar `applicationId`, paquete ni versiones de AGP, Kotlin o SDK.
- Tema propio Material 3: azul noche, verde menta para permitir y coral para bloquear. Claro y oscuro siguen al sistema.
- Material You es opcional y está desactivado por defecto.
- Navegación inferior simple con estado, sin `navigation-compose`.
- Se usan solo iconos del paquete `material-icons-core`, no `material-icons-extended`.
- Se conserva `logo2.png` como icono del launcher, por petición del usuario; se prioriza ese logo explícito frente al icono vectorial sugerido originalmente.
- El bloqueo opcional de números ocultos se mantiene activado solo si el usuario lo selecciona; Android y la operadora pueden no entregar el identificador.
- Pantallas nuevas: Inicio, Reglas con pestañas, Horarios, Registro y Más (Estadísticas, Simulador, Importar/Exportar, Ajustes, Acerca de).
- Acciones destructivas con confirmación y Snackbar con “Deshacer”.

## Servicio y notificaciones

- El servicio responde a la llamada antes de registrar, con try/catch global. Ante un error permite la llamada.
- La notificación de bloqueo es opcional, silenciosa, no incluye el número y solo pide `POST_NOTIFICATIONS` en Android 13 o superior cuando el usuario la activa.
- No se solicitan permisos de contactos ni de almacenamiento. La importación/exportación usa Storage Access Framework.

## Dificultades encontradas

- El JDK por defecto del entorno no incluía `jlink`; se compiló usando el JBR de Android Studio con `-Dorg.gradle.java.home`.
- Algunos archivos antiguos tenían codificación inconsistente; los archivos nuevos se guardan en UTF-8.
- Android no garantiza que el filtro reciba el identificador de todas las llamadas ocultas. El ajuste “Bloquear números ocultos” depende del fabricante y la operadora.
- Las llamadas de contactos guardados no se entregan al filtro por diseño de Android.
