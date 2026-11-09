package com.example.filtro400.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val zona: ZoneId = ZoneId.systemDefault()
private val formatoFechaHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
private val formatoDia = DateTimeFormatter.ofPattern("dd/MM")

fun formatearFechaHora(millis: Long): String =
    formatoFechaHora.format(Instant.ofEpochMilli(millis).atZone(zona))

fun formatearDia(millis: Long): String =
    formatoDia.format(Instant.ofEpochMilli(millis).atZone(zona))

fun inicioDelDia(millis: Long): Long =
    Instant.ofEpochMilli(millis)
        .atZone(zona)
        .toLocalDate()
        .atStartOfDay(zona)
        .toInstant()
        .toEpochMilli()

// Devuelve el inicio de cada uno de los últimos 7 días, del más antiguo al actual.
fun diasUltimos7(ahora: Long): List<Long> {
    val hoy = Instant.ofEpochMilli(ahora).atZone(zona).toLocalDate()
    return (6 downTo 0).map { diasAtras ->
        hoy.minusDays(diasAtras.toLong())
            .atStartOfDay(zona)
            .toInstant()
            .toEpochMilli()
    }
}
