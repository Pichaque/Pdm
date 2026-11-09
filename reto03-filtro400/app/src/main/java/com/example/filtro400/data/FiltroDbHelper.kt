package com.example.filtro400.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// SQLiteOpenHelper crea y versiona la base de datos local.
// Versiones: 1 = Fase 2, 2 = columnas nuevas de filtros, 3 = Fase 3,
// 4 = horarios, No molestar y numero temporal para acciones rapidas.
class FiltroDbHelper(
    context: Context,
    nombreBd: String = NOMBRE_BD
) : SQLiteOpenHelper(context, nombreBd, null, VERSION) {

    companion object {
        const val NOMBRE_BD = "filtros.db"
        const val VERSION = 4

        const val TABLA = "filtros"
        const val COL_ID = "id"
        const val COL_PREFIJO = "prefijo"
        const val COL_ACTIVO = "activo"
        const val COL_FECHA_CREACION = "fecha_creacion"
        const val COL_PRIORIDAD = "prioridad"
        const val COL_ACCION = "accion"
        const val COL_DIAS_MASK = "dias_mask"
        const val COL_HORA_INICIO = "hora_inicio"
        const val COL_HORA_FIN = "hora_fin"

        const val TABLA_NUMEROS_BLOQUEADOS = "numeros_bloqueados"
        const val TABLA_LISTA_BLANCA = "lista_blanca"
        const val TABLA_REGISTRO = "registro_llamadas"
        const val TABLA_FRANJAS_NO_MOLESTAR = "franjas_no_molestar"
        const val COL_NUMERO = "numero"
        const val COL_NUMERO_COMPLETO = "numero_completo"
        const val COL_ETIQUETA = "etiqueta"
        const val COL_FECHA = "fecha"
        const val COL_DECISION = "decision"
        const val COL_REGLA_TIPO = "regla_tipo"
        const val COL_REGLA_VALOR = "regla_valor"
        const val COL_SIMULADA = "simulada"

        // Mantiene el comportamiento de la Fase 1 en el primer arranque.
        const val PREFIJO_POR_DEFECTO = "400"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLA (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_PREFIJO TEXT NOT NULL UNIQUE,
                $COL_ACTIVO INTEGER NOT NULL DEFAULT 1,
                $COL_FECHA_CREACION INTEGER NOT NULL,
                $COL_PRIORIDAD INTEGER NOT NULL DEFAULT 0,
                $COL_ACCION TEXT NOT NULL DEFAULT 'BLOQUEAR',
                $COL_DIAS_MASK INTEGER,
                $COL_HORA_INICIO INTEGER,
                $COL_HORA_FIN INTEGER
            )
            """.trimIndent()
        )

        crearTablaNumeros(db, TABLA_NUMEROS_BLOQUEADOS)
        crearTablaNumeros(db, TABLA_LISTA_BLANCA)
        crearTablaRegistro(db)
        crearTablaFranjas(db)
        crearIndices(db)

        db.execSQL(
            "INSERT INTO $TABLA ($COL_PREFIJO, $COL_ACTIVO, $COL_FECHA_CREACION, $COL_PRIORIDAD, $COL_ACCION) " +
                "VALUES (?, 1, ?, 0, 'BLOQUEAR')",
            arrayOf<Any>(PREFIJO_POR_DEFECTO, System.currentTimeMillis())
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 1 -> 2: columnas nuevas de filtros, conservando los prefijos existentes.
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLA ADD COLUMN $COL_PRIORIDAD INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE $TABLA ADD COLUMN $COL_ACCION TEXT NOT NULL DEFAULT 'BLOQUEAR'")
        }

        // 2 -> 3: tablas nuevas de la Fase 3.
        if (oldVersion < 3) {
            crearTablaNumeros(db, TABLA_NUMEROS_BLOQUEADOS)
            crearTablaNumeros(db, TABLA_LISTA_BLANCA)
            crearTablaRegistro(db)
            crearIndices(db)
        }

        // 3 -> 4: horarios, No molestar y numero completo temporal.
        if (oldVersion < 4) {
            anadirColumnasHorario(db, TABLA)
            // Si venimos de v1/v2, las tablas de numeros y registro se acaban
            // de crear ya con el esquema nuevo y no hay que alterarlas.
            if (oldVersion >= 3) {
                anadirColumnasHorario(db, TABLA_NUMEROS_BLOQUEADOS)
                anadirColumnasHorario(db, TABLA_LISTA_BLANCA)
                db.execSQL("ALTER TABLE $TABLA_REGISTRO ADD COLUMN $COL_NUMERO_COMPLETO TEXT")
            }
            crearTablaFranjas(db)
            crearIndices(db)
        }
    }

    private fun crearTablaNumeros(db: SQLiteDatabase, tabla: String) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $tabla (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NUMERO TEXT NOT NULL UNIQUE,
                $COL_ETIQUETA TEXT NOT NULL DEFAULT '',
                $COL_ACTIVO INTEGER NOT NULL DEFAULT 1,
                $COL_FECHA_CREACION INTEGER NOT NULL,
                $COL_DIAS_MASK INTEGER,
                $COL_HORA_INICIO INTEGER,
                $COL_HORA_FIN INTEGER
            )
            """.trimIndent()
        )
    }

    private fun crearTablaRegistro(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLA_REGISTRO (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_FECHA INTEGER NOT NULL,
                numero_enmascarado TEXT NOT NULL,
                $COL_NUMERO_COMPLETO TEXT,
                $COL_DECISION TEXT NOT NULL,
                $COL_REGLA_TIPO TEXT NOT NULL,
                $COL_REGLA_VALOR TEXT,
                $COL_SIMULADA INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }

    private fun crearTablaFranjas(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLA_FRANJAS_NO_MOLESTAR (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_DIAS_MASK INTEGER NOT NULL,
                $COL_HORA_INICIO INTEGER NOT NULL,
                $COL_HORA_FIN INTEGER NOT NULL,
                $COL_ACTIVO INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )
    }

    private fun anadirColumnasHorario(db: SQLiteDatabase, tabla: String) {
        db.execSQL("ALTER TABLE $tabla ADD COLUMN $COL_DIAS_MASK INTEGER")
        db.execSQL("ALTER TABLE $tabla ADD COLUMN $COL_HORA_INICIO INTEGER")
        db.execSQL("ALTER TABLE $tabla ADD COLUMN $COL_HORA_FIN INTEGER")
    }

    private fun crearIndices(db: SQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_filtros_prefijo ON $TABLA($COL_PREFIJO)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_filtros_prioridad ON $TABLA($COL_PRIORIDAD)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_numeros_bloqueados_numero " +
                "ON $TABLA_NUMEROS_BLOQUEADOS($COL_NUMERO)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_lista_blanca_numero " +
                "ON $TABLA_LISTA_BLANCA($COL_NUMERO)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_registro_fecha ON $TABLA_REGISTRO($COL_FECHA)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_registro_decision_fecha " +
                "ON $TABLA_REGISTRO($COL_DECISION, $COL_FECHA)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_franjas_activo " +
                "ON $TABLA_FRANJAS_NO_MOLESTAR($COL_ACTIVO)"
        )
    }
}
