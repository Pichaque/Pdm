package com.example.filtro400.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import com.example.filtro400.ReglaBloqueo
import com.example.filtro400.domain.Horario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// CRUD compartido por numeros_bloqueados y lista_blanca.
open class NumeroRepository(
    private val helper: FiltroDbHelper,
    private val tabla: String
) {
    sealed class Resultado {
        data class Ok(val id: Long) : Resultado()
        data object Duplicado : Resultado()
        data class Error(val mensaje: String) : Resultado()
    }

    suspend fun insertar(
        numero: String,
        etiqueta: String = "",
        activo: Boolean = true,
        horario: Horario? = null
    ): Resultado = withContext(Dispatchers.IO) {
        val normalizado = normalizarNumero(numero)
            ?: return@withContext Resultado.Error("El numero no es valido")

        try {
            val id = helper.writableDatabase.insertOrThrow(
                tabla,
                null,
                ContentValues().apply {
                    put(FiltroDbHelper.COL_NUMERO, normalizado)
                    put(FiltroDbHelper.COL_ETIQUETA, etiqueta.trim())
                    put(FiltroDbHelper.COL_ACTIVO, if (activo) 1 else 0)
                    put(FiltroDbHelper.COL_FECHA_CREACION, System.currentTimeMillis())
                    escribirHorario(this, horario)
                }
            )
            Resultado.Ok(id)
        } catch (e: SQLiteConstraintException) {
            Resultado.Duplicado
        } catch (e: Exception) {
            Resultado.Error(e.message ?: "No se pudo guardar el numero")
        }
    }

    suspend fun actualizar(
        id: Long,
        numero: String,
        etiqueta: String,
        activo: Boolean,
        horario: Horario? = null
    ): Resultado = withContext(Dispatchers.IO) {
        val normalizado = normalizarNumero(numero)
            ?: return@withContext Resultado.Error("El numero no es valido")

        try {
            val filas = helper.writableDatabase.update(
                tabla,
                ContentValues().apply {
                    put(FiltroDbHelper.COL_NUMERO, normalizado)
                    put(FiltroDbHelper.COL_ETIQUETA, etiqueta.trim())
                    put(FiltroDbHelper.COL_ACTIVO, if (activo) 1 else 0)
                    escribirHorario(this, horario)
                },
                "${FiltroDbHelper.COL_ID} = ?",
                arrayOf(id.toString())
            )
            if (filas > 0) Resultado.Ok(id) else Resultado.Error("El numero ya no existe")
        } catch (e: SQLiteConstraintException) {
            Resultado.Duplicado
        } catch (e: Exception) {
            Resultado.Error(e.message ?: "No se pudo actualizar el numero")
        }
    }

    suspend fun eliminar(id: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            helper.writableDatabase.delete(
                tabla,
                "${FiltroDbHelper.COL_ID} = ?",
                arrayOf(id.toString())
            ) > 0
        } catch (e: Exception) {
            false
        }
    }

    suspend fun cambiarEstado(id: Long, activo: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            helper.writableDatabase.update(
                tabla,
                ContentValues().apply { put(FiltroDbHelper.COL_ACTIVO, if (activo) 1 else 0) },
                "${FiltroDbHelper.COL_ID} = ?",
                arrayOf(id.toString())
            ) > 0
        } catch (e: Exception) {
            false
        }
    }

    suspend fun obtenerTodos(): List<NumeroLista> = withContext(Dispatchers.IO) {
        consultar(null, null)
    }

    suspend fun obtenerActivos(): List<NumeroLista> = withContext(Dispatchers.IO) {
        consultar("${FiltroDbHelper.COL_ACTIVO} = ?", arrayOf("1"))
    }

    fun obtenerActivosBloqueante(): List<NumeroLista> =
        consultar("${FiltroDbHelper.COL_ACTIVO} = ?", arrayOf("1"))

    private fun consultar(seleccion: String?, argumentos: Array<String>?): List<NumeroLista> {
        val numeros = mutableListOf<NumeroLista>()
        helper.readableDatabase.query(
            tabla,
            null,
            seleccion,
            argumentos,
            null,
            null,
            "${FiltroDbHelper.COL_FECHA_CREACION} ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                numeros += leerNumero(cursor)
            }
        }
        return numeros
    }

    private fun leerNumero(cursor: Cursor) = NumeroLista(
        id = cursor.getLong(cursor.getColumnIndexOrThrow(FiltroDbHelper.COL_ID)),
        numero = cursor.getString(cursor.getColumnIndexOrThrow(FiltroDbHelper.COL_NUMERO)),
        etiqueta = cursor.getString(cursor.getColumnIndexOrThrow(FiltroDbHelper.COL_ETIQUETA)),
        activo = cursor.getInt(cursor.getColumnIndexOrThrow(FiltroDbHelper.COL_ACTIVO)) == 1,
        horario = leerHorario(cursor)
    )

    private fun normalizarNumero(numero: String): String? =
        ReglaBloqueo.normalizar(numero).takeIf { it.isNotEmpty() }
}

class NumerosBloqueadosRepository(helper: FiltroDbHelper) :
    NumeroRepository(helper, FiltroDbHelper.TABLA_NUMEROS_BLOQUEADOS)

class ListaBlancaRepository(helper: FiltroDbHelper) :
    NumeroRepository(helper, FiltroDbHelper.TABLA_LISTA_BLANCA)
