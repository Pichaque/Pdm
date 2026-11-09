package com.example.filtro400.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import com.example.filtro400.ReglaBloqueo
import com.example.filtro400.domain.DiaSemana
import com.example.filtro400.domain.Horario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Acceso a la tabla de filtros. Las operaciones se ejecutan en Dispatchers.IO.
class FiltroRepository(private val helper: FiltroDbHelper) {

    sealed class Resultado {
        data class Ok(val id: Long) : Resultado()
        data object Duplicado : Resultado()
        data class Error(val mensaje: String) : Resultado()
    }

    suspend fun insertar(
        prefijo: String,
        prioridad: Int = 0,
        accion: Accion = Accion.BLOQUEAR,
        horario: Horario? = null
    ): Resultado = withContext(Dispatchers.IO) {
        val normalizado = normalizarPrefijo(prefijo)
            ?: return@withContext Resultado.Error("El prefijo no es valido")

        try {
            val id = helper.writableDatabase.insertOrThrow(
                FiltroDbHelper.TABLA,
                null,
                valores(
                    prefijo = normalizado,
                    activo = true,
                    prioridad = prioridad,
                    accion = accion,
                    horario = horario,
                    fechaCreacion = System.currentTimeMillis()
                )
            )
            Resultado.Ok(id)
        } catch (e: SQLiteConstraintException) {
            Resultado.Duplicado
        } catch (e: Exception) {
            Resultado.Error(e.message ?: "No se pudo guardar el filtro")
        }
    }

    suspend fun actualizar(
        id: Long,
        prefijo: String,
        prioridad: Int? = null,
        accion: Accion? = null,
        horario: Horario? = null
    ): Resultado = withContext(Dispatchers.IO) {
        val normalizado = normalizarPrefijo(prefijo)
            ?: return@withContext Resultado.Error("El prefijo no es valido")

        try {
            val valores = ContentValues().apply {
                put(FiltroDbHelper.COL_PREFIJO, normalizado)
                prioridad?.let { put(FiltroDbHelper.COL_PRIORIDAD, it) }
                accion?.let { put(FiltroDbHelper.COL_ACCION, it.name) }
                escribirHorario(this, horario)
            }
            val filas = helper.writableDatabase.update(
                FiltroDbHelper.TABLA,
                valores,
                "${FiltroDbHelper.COL_ID} = ?",
                arrayOf(id.toString())
            )
            if (filas > 0) Resultado.Ok(id) else Resultado.Error("El filtro ya no existe")
        } catch (e: SQLiteConstraintException) {
            Resultado.Duplicado
        } catch (e: Exception) {
            Resultado.Error(e.message ?: "No se pudo actualizar el filtro")
        }
    }

    suspend fun eliminar(id: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            helper.writableDatabase.delete(
                FiltroDbHelper.TABLA,
                "${FiltroDbHelper.COL_ID} = ?",
                arrayOf(id.toString())
            ) > 0
        } catch (e: Exception) {
            false
        }
    }

    suspend fun cambiarEstado(id: Long, activo: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val filas = helper.writableDatabase.update(
                FiltroDbHelper.TABLA,
                ContentValues().apply {
                    put(FiltroDbHelper.COL_ACTIVO, if (activo) 1 else 0)
                },
                "${FiltroDbHelper.COL_ID} = ?",
                arrayOf(id.toString())
            )
            filas > 0
        } catch (e: Exception) {
            false
        }
    }

    suspend fun obtenerTodos(): List<Filtro> = withContext(Dispatchers.IO) {
        consultar(null, null)
    }

    suspend fun obtenerActivos(): List<Filtro> = withContext(Dispatchers.IO) {
        consultarActivos()
    }

    // El servicio la usa de forma sincrona: consulta diminuta e indexada.
    fun obtenerActivosBloqueante(): List<Filtro> = consultarActivos()

    private fun consultarActivos(): List<Filtro> =
        consultar("${FiltroDbHelper.COL_ACTIVO} = ?", arrayOf("1"))

    private fun consultar(seleccion: String?, argumentos: Array<String>?): List<Filtro> {
        val filtros = mutableListOf<Filtro>()
        helper.readableDatabase.query(
            FiltroDbHelper.TABLA,
            null,
            seleccion,
            argumentos,
            null,
            null,
            "${FiltroDbHelper.COL_PRIORIDAD} DESC, ${FiltroDbHelper.COL_FECHA_CREACION} ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                filtros += leerFiltro(cursor)
            }
        }
        return filtros
    }

    private fun leerFiltro(cursor: Cursor) = Filtro(
        id = cursor.getLong(cursor.getColumnIndexOrThrow(FiltroDbHelper.COL_ID)),
        prefijo = cursor.getString(cursor.getColumnIndexOrThrow(FiltroDbHelper.COL_PREFIJO)),
        activo = cursor.getInt(cursor.getColumnIndexOrThrow(FiltroDbHelper.COL_ACTIVO)) == 1,
        prioridad = cursor.getInt(cursor.getColumnIndexOrThrow(FiltroDbHelper.COL_PRIORIDAD)),
        accion = Accion.desdeTexto(
            cursor.getString(cursor.getColumnIndexOrThrow(FiltroDbHelper.COL_ACCION))
        ),
        horario = leerHorario(cursor)
    )

    private fun valores(
        prefijo: String,
        activo: Boolean,
        prioridad: Int,
        accion: Accion,
        horario: Horario?,
        fechaCreacion: Long
    ) = ContentValues().apply {
        put(FiltroDbHelper.COL_PREFIJO, prefijo)
        put(FiltroDbHelper.COL_ACTIVO, if (activo) 1 else 0)
        put(FiltroDbHelper.COL_PRIORIDAD, prioridad)
        put(FiltroDbHelper.COL_ACCION, accion.name)
        put(FiltroDbHelper.COL_FECHA_CREACION, fechaCreacion)
        escribirHorario(this, horario)
    }

    // Quitamos espacios/guiones y el prefijo pais; si queda vacio no vale.
    private fun normalizarPrefijo(prefijo: String): String? =
        ReglaBloqueo.normalizar(prefijo).takeIf { it.isNotEmpty() }
}

// NULL en las tres columnas = regla sin horario (siempre activa).
internal fun leerHorario(cursor: Cursor): Horario? {
    val idxDias = cursor.getColumnIndexOrThrow(FiltroDbHelper.COL_DIAS_MASK)
    val idxInicio = cursor.getColumnIndexOrThrow(FiltroDbHelper.COL_HORA_INICIO)
    val idxFin = cursor.getColumnIndexOrThrow(FiltroDbHelper.COL_HORA_FIN)
    if (cursor.isNull(idxDias) || cursor.isNull(idxInicio) || cursor.isNull(idxFin)) return null

    val dias = DiaSemana.desdeMascara(cursor.getInt(idxDias))
    if (dias.isEmpty()) return null
    return Horario(
        dias = dias,
        inicioMinuto = cursor.getInt(idxInicio),
        finMinuto = cursor.getInt(idxFin)
    )
}

internal fun escribirHorario(valores: ContentValues, horario: Horario?) {
    if (horario == null || horario.dias.isEmpty()) {
        valores.putNull(FiltroDbHelper.COL_DIAS_MASK)
        valores.putNull(FiltroDbHelper.COL_HORA_INICIO)
        valores.putNull(FiltroDbHelper.COL_HORA_FIN)
    } else {
        valores.put(FiltroDbHelper.COL_DIAS_MASK, DiaSemana.mascara(horario.dias))
        valores.put(FiltroDbHelper.COL_HORA_INICIO, horario.inicioMinuto)
        valores.put(FiltroDbHelper.COL_HORA_FIN, horario.finMinuto)
    }
}
