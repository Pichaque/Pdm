package com.example.filtro400

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Tests de la regla con prefijos configurables (Fase 2).
class ReglaBloqueoPrefijosTest {

    @Test fun listaVaciaNoBloquea() =
        assertFalse(ReglaBloqueo.debeBloquearse("400123456", emptyList()))

    @Test fun bloqueaConVariosPrefijos() =
        assertTrue(ReglaBloqueo.debeBloquearse("400123456", listOf("900", "400")))

    @Test fun bloqueaPrefijoDistintoDe400() =
        assertTrue(ReglaBloqueo.debeBloquearse("900123456", listOf("900")))

    @Test fun bloqueaConMas34EnElNumero() =
        assertTrue(ReglaBloqueo.debeBloquearse("+34 900 123 456", listOf("900")))

    @Test fun bloqueaConEspaciosYGuiones() =
        assertTrue(ReglaBloqueo.debeBloquearse("+34 400-123-456", listOf("400")))

    @Test fun noBloqueaInternacionalQueContiene400() =
        assertFalse(ReglaBloqueo.debeBloquearse("+1400123456", listOf("400")))

    @Test fun noBloqueaNumeroVacio() =
        assertFalse(ReglaBloqueo.debeBloquearse("", listOf("400")))

    @Test fun normalizaPrefijoConEspaciosYGuiones() =
        assertTrue(ReglaBloqueo.debeBloquearse("400123456", listOf(" 4-0 0 ")))
}
