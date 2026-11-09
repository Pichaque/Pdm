package com.example.filtro400

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReglaBloqueoTest {

    @Test fun bloquea400SinPrefijoPais() =
        assertTrue(ReglaBloqueo.debeBloquearse("400123456"))

    @Test fun bloquea400ConMas34() =
        assertTrue(ReglaBloqueo.debeBloquearse("+34400123456"))

    @Test fun bloquea400Con0034() =
        assertTrue(ReglaBloqueo.debeBloquearse("0034400123456"))

    @Test fun bloquea400ConEspacios() =
        assertTrue(ReglaBloqueo.debeBloquearse("+34 400 123 456"))

    @Test fun noBloqueaMovil() =
        assertFalse(ReglaBloqueo.debeBloquearse("612345678"))

    @Test fun noBloqueaFijo() =
        assertFalse(ReglaBloqueo.debeBloquearse("912345678"))

    @Test fun noBloqueaInternacionalQueContiene400() =
        assertFalse(ReglaBloqueo.debeBloquearse("+1400123456"))

    @Test fun noBloqueaNumeroVacio() =
        assertFalse(ReglaBloqueo.debeBloquearse(""))

    // Tests para sobrecarga debeBloquearse(numero, prefijos)
    @Test fun listaVaciaNoBloquea() =
        assertFalse(ReglaBloqueo.debeBloquearse("400123456", emptyList()))

    @Test fun variosPrefijos() =
        assertTrue(ReglaBloqueo.debeBloquearse("900123456", listOf("400", "900")))

    @Test fun prefijoConMas34EnNumero() =
        assertTrue(ReglaBloqueo.debeBloquearse("+34900123456", listOf("900")))

    @Test fun numeroConEspaciosGuiones() =
        assertTrue(ReglaBloqueo.debeBloquearse("400 123-456", listOf("400")))

    @Test fun mas1400123456NoBloqueaCon400() =
        assertFalse(ReglaBloqueo.debeBloquearse("+1400123456", listOf("400")))

    @Test fun numeroVacioNoBloqueaConPrefijos() =
        assertFalse(ReglaBloqueo.debeBloquearse("", listOf("400")))

    @Test fun prefijoDistintoDe400() =
        assertTrue(ReglaBloqueo.debeBloquearse("900123456", listOf("900")))
}