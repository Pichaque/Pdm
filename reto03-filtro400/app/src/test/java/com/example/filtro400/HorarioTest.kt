package com.example.filtro400

import com.example.filtro400.domain.DiaSemana
import com.example.filtro400.domain.Horario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class HorarioTest {

    private val nocturno = Horario(
        dias = DiaSemana.todos(),
        inicioMinuto = 22 * 60,
        finMinuto = 8 * 60
    )

    @Test
    fun franjaNocturnaIncluyeMedianocheYMadrugada() {
        assertTrue(nocturno.contiene(LocalDateTime.of(2026, 10, 26, 23, 30)))
        assertTrue(nocturno.contiene(LocalDateTime.of(2026, 10, 27, 7, 0)))
    }

    @Test
    fun franjaNocturnaExcluyeMediodia() {
        assertFalse(nocturno.contiene(LocalDateTime.of(2026, 10, 26, 12, 0)))
    }

    @Test
    fun inicioEsInclusivoYFinExclusivo() {
        assertTrue(nocturno.contiene(LocalDateTime.of(2026, 10, 26, 22, 0)))
        assertFalse(nocturno.contiene(LocalDateTime.of(2026, 10, 27, 8, 0)))
    }

    @Test
    fun franjaSoloLunesNoAplicaMartes() {
        val horario = Horario(
            dias = setOf(DiaSemana.LUNES),
            inicioMinuto = 9 * 60,
            finMinuto = 18 * 60
        )

        // 26/10/2026 es lunes; 27/10/2026 es martes.
        assertTrue(horario.contiene(LocalDateTime.of(2026, 10, 26, 12, 0)))
        assertFalse(horario.contiene(LocalDateTime.of(2026, 10, 27, 12, 0)))
    }

    @Test
    fun franjaNocturnaUsaElDiaEnQueEmpieza() {
        val sabadoNoche = Horario(
            dias = setOf(DiaSemana.SABADO),
            inicioMinuto = 22 * 60,
            finMinuto = 8 * 60
        )

        // Sabado 24/10/2026 a las 23:00 y domingo 25 a las 02:00.
        assertTrue(sabadoNoche.contiene(LocalDateTime.of(2026, 10, 24, 23, 0)))
        assertTrue(sabadoNoche.contiene(LocalDateTime.of(2026, 10, 25, 2, 0)))
        // Sabado de madrugada no cuenta: la franja empieza ese dia a las 22:00.
        assertFalse(sabadoNoche.contiene(LocalDateTime.of(2026, 10, 24, 2, 0)))
    }

    @Test
    fun todoElDiaSoloRespetaLosDias() {
        val finesDeSemana = Horario(
            dias = DiaSemana.finDeSemana(),
            inicioMinuto = 0,
            finMinuto = 24 * 60
        )

        assertTrue(finesDeSemana.contiene(LocalDateTime.of(2026, 10, 25, 3, 0)))
        assertFalse(finesDeSemana.contiene(LocalDateTime.of(2026, 10, 26, 3, 0)))
    }

    @Test
    fun mascaraDeDiasVaYVuelve() {
        val dias = setOf(DiaSemana.LUNES, DiaSemana.MIERCOLES, DiaSemana.DOMINGO)
        val mascara = DiaSemana.mascara(dias)

        assertEquals(dias, DiaSemana.desdeMascara(mascara))
    }

    @Test
    fun proximaOcurrenciaEncuentraElSiguienteDiaValido() {
        val horario = Horario(
            dias = setOf(DiaSemana.LUNES),
            inicioMinuto = 9 * 60,
            finMinuto = 18 * 60
        )

        val proxima = horario.proximaOcurrencia(LocalDateTime.of(2026, 10, 27, 12, 0))

        assertNotNull(proxima)
        assertEquals(LocalDateTime.of(2026, 11, 2, 9, 0), proxima)
    }
}
