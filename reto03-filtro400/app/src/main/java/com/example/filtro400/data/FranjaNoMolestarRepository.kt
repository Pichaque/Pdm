package com.example.filtro400.data

import android.content.ContentValues
import android.database.Cursor
import com.example.filtro400.domain.DiaSemana
import com.example.filtro400.domain.Horario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// CRUD de las franjas del modo global No molestar.
class FranjaNoMolestarRepository(private val helper: FiltroDbHelper) {

    sealed class Resultado {
        data class Ok(val id: Long) : Resultado()
        data class Error(val mensaje: String) : Resultado()
    }

    suspend fun insertar(
        horario: Horario,
        activo: Boolean = true
    ): Resultado = withContext(Dispatchers.IO) {
        if (horario.dias.isEmpty()) {
            return@withContext Resultado.Error("Selecciona al menos un dia")
        }
        try {
            val id = helper.writableDatabase.insertOrThrow(
                FiltroDbHelper.TABLA_FRANJAS_NO_MOLESTAR,
                null,
                valores(horario, activo)
            )
            Resultado.Ok(id)
        } catch (e: Exception) {
            Resultado.Error(e.message ?: "No se pudo guardar la franja")
        }
    }

    suspend fun actualizar(
        id: Long,
        horario: Horario,
        activo: Boolean
    ): Resultado = withContext(Dispatchers.IO) {
        if (horario.dias.isEmpty()) {
            return@withContext Resultado.Error("Selecciona al menos un dia")
        }
        try {
            val filas = helper.writableDatabase.update(
                FiltroDbHelper.TABLA_FRANJAS_NO_MOLESTAR,
                valores(horario, activo),
                "${FiltroDbHelper.COL_ID} = ?",
                arrayOf(id.toString())
            )
            if (filas > 0) Resultado.Ok(id) else Resultado.Error("La franja ya no existe")
        } catch (e: Exception) {
            Resultado.Error(e.message ?: "No se pudo actualizar la franja")
        }
    }

    suspend fun cambiarEstado(id: Long, activo: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            helper.writableDatabase.update(
                FiltroDbHelper.TABLA_FRANJAS_NO_MOLESTAR,
                ContentValues().apply {
                    put(FiltroDbHelper.COL_ACTIVO, if (activo) 1 else 0)
                },
                "${FiltroDbHelper.COL_ID} = ?",
                arrayOf(id.toString())
            ) > 0
        } catch (e: Exception) {
            false
        }
    }

    suspend fun eliminar(id: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            helper.writableDatabase.delete(
                FiltroDbHelper.TABLA_FRANJAS_NO_MOLESTAR,
                "${FiltroDbHelper.COL_ID} = ?",
                arrayOf(id.toString())
            ) > 0
        } catch (e: Exception) {
            false
        }
    }

    suspend fun obtenerTodas(): List<FranjaNoMolestar> = withContext(Dispatchers.IO) {
        consultar(null, null)
    }

    suspend fun obtenerActivas(): List<FranjaNoMolestar> = withContext(Dispatchers.IO) {
        consultar("${FiltroDbHelper.COL_ACTIVO} = ?", arrayOf("1"))
    }

    fun obtenerActivasBloqueante(): List<FranjaNoMolestar> =
        consultar("${FiltroDbHelper.COL_ACTIVO} = ?", arrayOf("1"))

    private fun consultar(seleccion: String?, argumentos: Array<String>?): List<FranjaNoMolestar> {
        val franjas = mutableListOf<FranjaNoMolestar>()
        helper.readableDatabase.query(
            FiltroDbHelper.TABLA_FRANJAS_NO_MOLESTAR,
            null,
            seleccion,
            argumentos,
            null,
            null,
            "${FiltroDbHelper.COL_HORA_INICIO} ASC, ${FiltroDbHelper.COL_ID} ASC"
        ).use { cursor ->
            val idxId = cursor.getColumnIndexOrThrow(FiltroDbHelper.COL_ID)
            val idxDias = cursor.getColumnIndexOrThrow(FiltroDbHelper.COL_DIAS_MASK)
            val idxInicio = cursor.getColumnIndexOrThrow(FiltroDbHelper.COL_HORA_INICIO)
            val idxFin = cursor.getColumnIndexOrThrow(FiltroDbHelper.COL_HORA_FIN)
            val idxActivo = cursor.getColumnIndexOrThrow(FiltroDbHelper.COL_ACTIVO)

            while (cursor.moveToNext()) {
                franjas += FranjaNoMolestar(
                    id = cursor.getLong(idxId),
                    horario = Horario(
                        dias = DiaSemana.desdeMascara(cursor.getInt(idxDias)),
                        inicioMinuto = cursor.getInt(idxInicio),
                        finMinuto = cursor.getInt(idxFin)
                    ),
                    activo = cursor.getInt(idxActivo) == 1
                )
            }
        }
        return franjas
    }

    private fun valores(horario: Horario, activo: Boolean) = ContentValues().apply {
        put(FiltroDbHelper.COL_DIAS_MASK, DiaSemana.mascara(horario.dias))
        put(FiltroDbHelper.COL_HORA_INICIO, horario.inicioMinuto)
        put(FiltroDbHelper.COL_HORA_FIN, horario.finMinuto)
        put(FiltroDbHelper.COL_ACTIVO, if (activo) 1 else 0)
    }
}
