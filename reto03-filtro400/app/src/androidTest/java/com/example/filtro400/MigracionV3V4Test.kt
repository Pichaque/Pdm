package com.example.filtro400

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.filtro400.data.FiltroDbHelper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigracionV3V4Test {

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private val nombreBd = "migracion_v3_v4_test.db"

    @Before
    fun preparar() {
        contexto.deleteDatabase(nombreBd)
    }

    @After
    fun limpiar() {
        contexto.deleteDatabase(nombreBd)
    }

    @Test
    fun migraV3ConservandoDatosYAnadiendoHorarios() {
        val ruta = contexto.getDatabasePath(nombreBd)
        ruta.parentFile?.mkdirs()

        val vieja = SQLiteDatabase.openOrCreateDatabase(ruta, null)
        vieja.execSQL(
            "CREATE TABLE filtros (id INTEGER PRIMARY KEY AUTOINCREMENT, prefijo TEXT NOT NULL UNIQUE, " +
                "activo INTEGER NOT NULL DEFAULT 1, fecha_creacion INTEGER NOT NULL, " +
                "prioridad INTEGER NOT NULL DEFAULT 0, accion TEXT NOT NULL DEFAULT 'BLOQUEAR')"
        )
        vieja.execSQL(
            "CREATE TABLE numeros_bloqueados (id INTEGER PRIMARY KEY AUTOINCREMENT, numero TEXT NOT NULL UNIQUE, " +
                "etiqueta TEXT NOT NULL DEFAULT '', activo INTEGER NOT NULL DEFAULT 1, fecha_creacion INTEGER NOT NULL)"
        )
        vieja.execSQL(
            "CREATE TABLE lista_blanca (id INTEGER PRIMARY KEY AUTOINCREMENT, numero TEXT NOT NULL UNIQUE, " +
                "etiqueta TEXT NOT NULL DEFAULT '', activo INTEGER NOT NULL DEFAULT 1, fecha_creacion INTEGER NOT NULL)"
        )
        vieja.execSQL(
            "CREATE TABLE registro_llamadas (id INTEGER PRIMARY KEY AUTOINCREMENT, fecha INTEGER NOT NULL, " +
                "numero_enmascarado TEXT NOT NULL, decision TEXT NOT NULL, regla_tipo TEXT NOT NULL, " +
                "regla_valor TEXT, simulada INTEGER NOT NULL DEFAULT 0)"
        )

        vieja.execSQL("INSERT INTO filtros (prefijo, activo, fecha_creacion, prioridad, accion) VALUES ('400', 1, 100, 5, 'BLOQUEAR')")
        vieja.execSQL("INSERT INTO numeros_bloqueados (numero, etiqueta, activo, fecha_creacion) VALUES ('612345678', 'Spam', 1, 100)")
        vieja.execSQL("INSERT INTO lista_blanca (numero, etiqueta, activo, fecha_creacion) VALUES ('699999999', 'Familia', 1, 100)")
        vieja.execSQL(
            "INSERT INTO registro_llamadas (fecha, numero_enmascarado, decision, regla_tipo, regla_valor, simulada) " +
                "VALUES (100, '612 ***', 'BLOQUEADA', 'PREFIJO', '400', 0)"
        )
        vieja.version = 3
        vieja.close()

        val helper = FiltroDbHelper(contexto, nombreBd)
        val db = helper.readableDatabase

        assertEquals(4, db.version)
        assertEquals(1, contar(db, "filtros"))
        assertEquals(1, contar(db, "numeros_bloqueados"))
        assertEquals(1, contar(db, "lista_blanca"))
        assertEquals(1, contar(db, "registro_llamadas"))
        assertTrue(tablaExiste(db, "franjas_no_molestar"))

        db.rawQuery("SELECT prefijo, prioridad, accion, dias_mask FROM filtros", null).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("400", cursor.getString(0))
            assertEquals(5, cursor.getInt(1))
            assertEquals("BLOQUEAR", cursor.getString(2))
            assertNull(cursor.getString(3))
        }

        db.rawQuery("SELECT numero, etiqueta, hora_inicio FROM numeros_bloqueados", null).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("612345678", cursor.getString(0))
            assertEquals("Spam", cursor.getString(1))
            assertTrue(cursor.isNull(2))
        }

        db.rawQuery("SELECT numero_enmascarado, numero_completo FROM registro_llamadas", null).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("612 ***", cursor.getString(0))
            assertNull(cursor.getString(1))
        }

        helper.close()
    }

    private fun contar(db: SQLiteDatabase, tabla: String): Int =
        db.rawQuery("SELECT COUNT(*) FROM $tabla", null).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    private fun tablaExiste(db: SQLiteDatabase, tabla: String): Boolean =
        db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf(tabla)
        ).use { cursor -> cursor.moveToFirst() }
}
