package com.example.filtro400.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

// Dia de la semana con su bit para guardarlo como mascara en SQLite/CSV.
enum class DiaSemana(val bit: Int, val etiqueta: String, val nombre: String) {
    LUNES(1, "L", "Lunes"),
    MARTES(2, "M", "Martes"),
    MIERCOLES(4, "X", "Miercoles"),
    JUEVES(8, "J", "Jueves"),
    VIERNES(16, "V", "Viernes"),
    SABADO(32, "S", "Sabado"),
    DOMINGO(64, "D", "Domingo");

    fun coincide(dia: DayOfWeek): Boolean = this == desde(dia)

    companion object {
        fun desde(dia: DayOfWeek): DiaSemana = when (dia) {
            DayOfWeek.MONDAY -> LUNES
            DayOfWeek.TUESDAY -> MARTES
            DayOfWeek.WEDNESDAY -> MIERCOLES
            DayOfWeek.THURSDAY -> JUEVES
            DayOfWeek.FRIDAY -> VIERNES
            DayOfWeek.SATURDAY -> SABADO
            DayOfWeek.SUNDAY -> DOMINGO
        }

        fun desdeMascara(mascara: Int): Set<DiaSemana> =
            entries.filter { mascara and it.bit != 0 }.toSet()

        fun mascara(dias: Set<DiaSemana>): Int =
            dias.fold(0) { acumulado, dia -> acumulado or dia.bit }

        fun todos(): Set<DiaSemana> = entries.toSet()

        fun laborables(): Set<DiaSemana> =
            setOf(LUNES, MARTES, MIERCOLES, JUEVES, VIERNES)

        fun finDeSemana(): Set<DiaSemana> = setOf(SABADO, DOMINGO)
    }
}

const val MINUTOS_DIA = 24 * 60

// Franja horaria reutilizable. Inicio inclusivo, fin exclusivo.
// Si inicio == fin se interpreta como "todo el dia".
data class Horario(
    val dias: Set<DiaSemana> = DiaSemana.todos(),
    val inicioMinuto: Int = 0,
    val finMinuto: Int = MINUTOS_DIA
) {
    val esTodoElDia: Boolean
        get() = inicioMinuto == finMinuto ||
            (inicioMinuto == 0 && finMinuto >= MINUTOS_DIA)

    val esSiempre: Boolean
        get() = dias.size == DiaSemana.entries.size && esTodoElDia

    fun contiene(momento: LocalDateTime): Boolean {
        if (dias.isEmpty()) return false
        val minuto = momento.hour * 60 + momento.minute
        if (esTodoElDia) return DiaSemana.desde(momento.dayOfWeek) in dias

        val hoy = DiaSemana.desde(momento.dayOfWeek)
        val ayer = DiaSemana.desde(momento.dayOfWeek.minus(1))

        return if (inicioMinuto < finMinuto) {
            hoy in dias && minuto >= inicioMinuto && minuto < finMinuto
        } else {
            // Franja nocturna: empieza un dia y termina al dia siguiente.
            (hoy in dias && minuto >= inicioMinuto) ||
                (ayer in dias && minuto < finMinuto)
        }
    }

    fun coincideAhora(ahora: LocalDateTime = LocalDateTime.now()): Boolean =
        contiene(ahora)

    // Siguiente inicio de la franja a partir del momento indicado.
    fun proximaOcurrencia(desde: LocalDateTime): LocalDateTime? {
        if (dias.isEmpty()) return null
        val minuto = if (esTodoElDia) 0 else inicioMinuto
        for (offset in 0..7) {
            val fecha: LocalDate = desde.toLocalDate().plusDays(offset.toLong())
            if (DiaSemana.desde(fecha.dayOfWeek) !in dias) continue
            val candidato = fecha.atStartOfDay().plusMinutes(minuto.toLong())
            if (candidato.isAfter(desde)) return candidato
        }
        return null
    }

    fun textoInicio(): String = minutoATexto(inicioMinuto)

    fun textoFin(): String = if (finMinuto >= MINUTOS_DIA) "24:00" else minutoATexto(finMinuto)

    fun resumen(): String {
        val diasTexto = if (dias.size == DiaSemana.entries.size) {
            "Todos los dias"
        } else {
            dias.sortedBy { it.ordinal }.joinToString(", ") { it.etiqueta }
        }
        val horas = if (esTodoElDia) "todo el dia" else "${textoInicio()}-${textoFin()}"
        return "$diasTexto - $horas"
    }

    companion object {
        fun minutoATexto(minuto: Int): String {
            val normalizado = minuto.coerceIn(0, MINUTOS_DIA)
            val hora = normalizado / 60
            val resto = normalizado % 60
            return "%02d:%02d".format(hora, resto)
        }

        fun textoAMinuto(texto: String): Int? {
            val partes = texto.trim().split(":")
            if (partes.size != 2) return null
            val hora = partes[0].toIntOrNull() ?: return null
            val minuto = partes[1].toIntOrNull() ?: return null
            if (hora !in 0..24 || minuto !in 0..59) return null
            if (hora == 24 && minuto != 0) return null
            return hora * 60 + minuto
        }
    }
}
