package com.example.filtro400.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.filtro400.ReglaBloqueo
import com.example.filtro400.domain.Decision
import com.example.filtro400.domain.ReglaTipo
import com.example.filtro400.util.diasUltimos7
import com.example.filtro400.util.enmascararNumero
import com.example.filtro400.util.inicioDelDia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RegistroLlamadasRepository(private val helper: FiltroDbHelper) {

    suspend fun registrar(
        decision: Decision,
        numero: String,
        simulada: Boolean,
        fecha: Long = System.currentTimeMillis()
    ): Long = withContext(Dispatchers.IO) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            val id = db.insertOrThrow(
                FiltroDbHelper.TABLA_REGISTRO,
                null,
                ContentValues().apply {
                    put(FiltroDbHelper.COL_FECHA, fecha)
                    put("numero_enmascarado", enmascararNumero(numero))
                    put(FiltroDbHelper.COL_NUMERO_COMPLETO, numero.takeIf { it.isNotBlank() })
                    put(
                        FiltroDbHelper.COL_DECISION,
                        if (decision.bloquear) DecisionLlamada.BLOQUEADA.name
                        else DecisionLlamada.PERMITIDA.name
                    )
                    put(FiltroDbHelper.COL_REGLA_TIPO, decision.reglaTipo.name)
                    put(
                        FiltroDbHelper.COL_REGLA_VALOR,
                        valorReglaSeguro(decision.reglaValor, numero)
                    )
                    put(FiltroDbHelper.COL_SIMULADA, if (simulada) 1 else 0)
                }
            )
            purgar(db, System.currentTimeMillis())
            db.setTransactionSuccessful()
            id
        } finally {
            db.endTransaction()
        }
    }

    suspend fun obtenerTodos(): List<RegistroLlamada> = withContext(Dispatchers.IO) {
        purgar(helper.writableDatabase, System.currentTimeMillis())
        val registros = mutableListOf<RegistroLlamada>()
        helper.readableDatabase.query(
            FiltroDbHelper.TABLA_REGISTRO,
            null,
            null,
            null,
            null,
            null,
            "${FiltroDbHelper.COL_FECHA} DESC, ${FiltroDbHelper.COL_ID} DESC"
        ).use { cursor ->
            val idxId = cursor.getColumnIndexOrThrow(FiltroDbHelper.COL_ID)
            val idxFecha = cursor.getColumnIndexOrThrow(FiltroDbHelper.COL_FECHA)
            val idxNumero = cursor.getColumnIndexOrThrow("numero_enmascarado")
            val idxCompleto = cursor.getColumnIndexOrThrow(FiltroDbHelper.COL_NUMERO_COMPLETO)
            val idxDecision = cursor.getColumnIndexOrThrow(FiltroDbHelper.COL_DECISION)
            val idxTipo = cursor.getColumnIndexOrThrow(FiltroDbHelper.COL_REGLA_TIPO)
            val idxValor = cursor.getColumnIndexOrThrow(FiltroDbHelper.COL_REGLA_VALOR)
            val idxSimulada = cursor.getColumnIndexOrThrow(FiltroDbHelper.COL_SIMULADA)

            while (cursor.moveToNext()) {
                registros += RegistroLlamada(
                    id = cursor.getLong(idxId),
                    fecha = cursor.getLong(idxFecha),
                    numeroEnmascarado = cursor.getString(idxNumero),
                    decision = DecisionLlamada.desdeTexto(cursor.getString(idxDecision)),
                    reglaTipo = ReglaTipo.desdeTexto(cursor.getString(idxTipo)),
                    reglaValor = if (cursor.isNull(idxValor)) null else cursor.getString(idxValor),
                    simulada = cursor.getInt(idxSimulada) == 1,
                    numeroCompleto = if (cursor.isNull(idxCompleto)) null else cursor.getString(idxCompleto)
                )
            }
        }
        registros
    }

    suspend fun contarHoy(): Pair<Int, Int> = withContext(Dispatchers.IO) {
        purgar(helper.writableDatabase, System.currentTimeMillis())
        val desde = inicioDelDia(System.currentTimeMillis())
        var bloqueadas = 0
        var permitidas = 0
        helper.readableDatabase.rawQuery(
            "SELECT ${FiltroDbHelper.COL_DECISION}, COUNT(*) FROM ${FiltroDbHelper.TABLA_REGISTRO} " +
                "WHERE ${FiltroDbHelper.COL_FECHA} >= ? AND ${FiltroDbHelper.COL_SIMULADA} = 0 " +
                "GROUP BY ${FiltroDbHelper.COL_DECISION}",
            arrayOf(desde.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                when (DecisionLlamada.desdeTexto(cursor.getString(0))) {
                    DecisionLlamada.BLOQUEADA -> bloqueadas = cursor.getInt(1)
                    DecisionLlamada.PERMITIDA -> permitidas = cursor.getInt(1)
                }
            }
        }
        bloqueadas to permitidas
    }

    suspend fun borrarTodo(): Boolean = withContext(Dispatchers.IO) {
        try {
            helper.writableDatabase.delete(FiltroDbHelper.TABLA_REGISTRO, null, null)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun obtenerEstadisticas(
        incluirSimuladas: Boolean = false
    ): Estadisticas = withContext(Dispatchers.IO) {
        purgar(helper.writableDatabase, System.currentTimeMillis())
        val db = helper.readableDatabase
        val filtroSimuladas =
            if (incluirSimuladas) "" else " AND ${FiltroDbHelper.COL_SIMULADA} = 0"

        var bloqueadas = 0
        var permitidas = 0
        db.rawQuery(
            "SELECT ${FiltroDbHelper.COL_DECISION}, COUNT(*) FROM ${FiltroDbHelper.TABLA_REGISTRO} " +
                "WHERE 1 = 1$filtroSimuladas GROUP BY ${FiltroDbHelper.COL_DECISION}",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                when (DecisionLlamada.desdeTexto(cursor.getString(0))) {
                    DecisionLlamada.BLOQUEADA -> bloqueadas = cursor.getInt(1)
                    DecisionLlamada.PERMITIDA -> permitidas = cursor.getInt(1)
                }
            }
        }

        val topReglas = mutableListOf<ConteoRegla>()
        db.rawQuery(
            "SELECT ${FiltroDbHelper.COL_REGLA_TIPO}, ${FiltroDbHelper.COL_REGLA_VALOR}, COUNT(*) AS total " +
                "FROM ${FiltroDbHelper.TABLA_REGISTRO} " +
                "WHERE ${FiltroDbHelper.COL_DECISION} = 'BLOQUEADA'$filtroSimuladas " +
                "GROUP BY ${FiltroDbHelper.COL_REGLA_TIPO}, ${FiltroDbHelper.COL_REGLA_VALOR} " +
                "ORDER BY total DESC LIMIT 5",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                topReglas += ConteoRegla(
                    reglaTipo = ReglaTipo.desdeTexto(cursor.getString(0)),
                    reglaValor = if (cursor.isNull(1)) null else cursor.getString(1),
                    total = cursor.getInt(2)
                )
            }
        }

        val ahora = System.currentTimeMillis()
        val dias = diasUltimos7(ahora)
        val porDia = dias.associateWith { 0 to 0 }.toMutableMap()

        db.rawQuery(
            "SELECT ${FiltroDbHelper.COL_FECHA}, ${FiltroDbHelper.COL_DECISION} " +
                "FROM ${FiltroDbHelper.TABLA_REGISTRO} " +
                "WHERE ${FiltroDbHelper.COL_FECHA} >= ?$filtroSimuladas",
            arrayOf(dias.first().toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val dia = inicioDelDia(cursor.getLong(0))
                val actuales = porDia[dia] ?: continue
                porDia[dia] = if (
                    DecisionLlamada.desdeTexto(cursor.getString(1)) == DecisionLlamada.BLOQUEADA
                ) {
                    actuales.first + 1 to actuales.second
                } else {
                    actuales.first to actuales.second + 1
                }
            }
        }

        Estadisticas(
            totalBloqueadas = bloqueadas,
            totalPermitidas = permitidas,
            bloqueosPorRegla = topReglas,
            ultimos7Dias = dias.map { dia ->
                val conteo = porDia[dia] ?: (0 to 0)
                ConteoDia(dia, conteo.first, conteo.second)
            }
        )
    }

    suspend fun purgarRegistrosExpirados(ahora: Long = System.currentTimeMillis()) =
        withContext(Dispatchers.IO) {
            purgar(helper.writableDatabase, ahora)
        }

    private fun valorReglaSeguro(valor: String?, numero: String): String? {
        if (valor == null) return null
        val valorNormalizado = ReglaBloqueo.normalizar(valor)
        val numeroNormalizado = ReglaBloqueo.normalizar(numero)
        return valor.takeIf {
            valorNormalizado.isEmpty() || valorNormalizado != numeroNormalizado
        }
    }

    // Retencion: maximo 500 registros y 30 dias. El numero completo se borra a las 48 h.
    private fun purgar(db: SQLiteDatabase, ahora: Long) {
        val limiteNumero = ahora - HORAS_NUMERO_COMPLETO * 60L * 60L * 1000L
        db.execSQL(
            "UPDATE ${FiltroDbHelper.TABLA_REGISTRO} SET " +
                "${FiltroDbHelper.COL_NUMERO_COMPLETO} = NULL, ${FiltroDbHelper.COL_REGLA_VALOR} = NULL " +
                "WHERE ${FiltroDbHelper.COL_FECHA} <= ?",
            arrayOf(limiteNumero)
        )
        limpiarNumeroDeValoresRegla(db)

        val limiteFecha = ahora - DIAS_RETENCION * 24L * 60L * 60L * 1000L
        db.delete(
            FiltroDbHelper.TABLA_REGISTRO,
            "${FiltroDbHelper.COL_FECHA} < ?",
            arrayOf(limiteFecha.toString())
        )
        db.execSQL(
            "DELETE FROM ${FiltroDbHelper.TABLA_REGISTRO} " +
                "WHERE ${FiltroDbHelper.COL_ID} NOT IN (" +
                "SELECT ${FiltroDbHelper.COL_ID} FROM ${FiltroDbHelper.TABLA_REGISTRO} " +
                "ORDER BY ${FiltroDbHelper.COL_FECHA} DESC, ${FiltroDbHelper.COL_ID} DESC LIMIT $MAX_REGISTROS)"
        )
    }

    private fun limpiarNumeroDeValoresRegla(db: SQLiteDatabase) {
        val valoresPrivados = db.query(
            FiltroDbHelper.TABLA_REGISTRO,
            arrayOf(
                FiltroDbHelper.COL_ID,
                FiltroDbHelper.COL_NUMERO_COMPLETO,
                FiltroDbHelper.COL_REGLA_TIPO,
                FiltroDbHelper.COL_REGLA_VALOR
            ),
            "${FiltroDbHelper.COL_REGLA_VALOR} IS NOT NULL",
            null,
            null,
            null,
            null
        ).use { cursor ->
            val idxId = cursor.getColumnIndexOrThrow(FiltroDbHelper.COL_ID)
            val idxNumero = cursor.getColumnIndexOrThrow(FiltroDbHelper.COL_NUMERO_COMPLETO)
            val idxTipo = cursor.getColumnIndexOrThrow(FiltroDbHelper.COL_REGLA_TIPO)
            val idxValor = cursor.getColumnIndexOrThrow(FiltroDbHelper.COL_REGLA_VALOR)
            val ids = mutableListOf<Long>()

            while (cursor.moveToNext()) {
                val tipo = ReglaTipo.desdeTexto(cursor.getString(idxTipo))
                val valor = cursor.getString(idxValor)
                val numero = if (cursor.isNull(idxNumero)) null else cursor.getString(idxNumero)
                if (
                    tipo == ReglaTipo.NUMERO ||
                    tipo == ReglaTipo.LISTA_BLANCA ||
                    (numero != null &&
                        ReglaBloqueo.normalizar(valor) == ReglaBloqueo.normalizar(numero))
                ) {
                    ids += cursor.getLong(idxId)
                }
            }
            ids
        }

        valoresPrivados.forEach { id ->
            db.update(
                FiltroDbHelper.TABLA_REGISTRO,
                ContentValues().apply {
                    putNull(FiltroDbHelper.COL_REGLA_VALOR)
                },
                "${FiltroDbHelper.COL_ID} = ?",
                arrayOf(id.toString())
            )
        }
    }

    private companion object {
        const val MAX_REGISTROS = 500
        const val DIAS_RETENCION = 30L
        const val HORAS_NUMERO_COMPLETO = 48L
    }
}
