package com.example.filtro400

import com.example.filtro400.data.Accion
import com.example.filtro400.data.Filtro
import com.example.filtro400.data.NumeroLista
import com.example.filtro400.domain.MotorFiltrado
import com.example.filtro400.domain.ReglaTipo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotorFiltradoTest {

    private fun prefijo(
        valor: String,
        prioridad: Int = 0,
        accion: Accion = Accion.BLOQUEAR,
        activo: Boolean = true,
        id: Long = 1
    ) = Filtro(id, valor, activo, prioridad, accion)

    private fun numero(valor: String, activo: Boolean = true, id: Long = 1) =
        NumeroLista(id, valor, "", activo)

    @Test
    fun listaBlancaGanaANumeroBloqueado() {
        val decision = MotorFiltrado.evaluar(
            numero = "612345678",
            listaBlanca = listOf(numero("612345678")),
            numerosBloqueados = listOf(numero("612345678", id = 2)),
            filtros = emptyList()
        )

        assertFalse(decision.bloquear)
        assertEquals(ReglaTipo.LISTA_BLANCA, decision.reglaTipo)
    }

    @Test
    fun listaBlancaGanaAPrefijo() {
        val decision = MotorFiltrado.evaluar(
            numero = "400123456",
            listaBlanca = listOf(numero("+34 400 123 456")),
            numerosBloqueados = emptyList(),
            filtros = listOf(prefijo("400", prioridad = 100))
        )

        assertFalse(decision.bloquear)
        assertEquals(ReglaTipo.LISTA_BLANCA, decision.reglaTipo)
    }

    @Test
    fun prioridadPermiteExcepcionarPrefijoGeneral() {
        val reglas = listOf(
            prefijo("400", prioridad = 1, id = 1),
            prefijo("400900", prioridad = 10, accion = Accion.PERMITIR, id = 2)
        )

        val excepcion = MotorFiltrado.evaluar(
            "400900123", emptyList(), emptyList(), reglas
        )
        val general = MotorFiltrado.evaluar(
            "400123456", emptyList(), emptyList(), reglas
        )

        assertFalse(excepcion.bloquear)
        assertEquals(ReglaTipo.PREFIJO, excepcion.reglaTipo)
        assertEquals("400900", excepcion.reglaValor)
        assertTrue(general.bloquear)
        assertEquals("400", general.reglaValor)
    }

    @Test
    fun numeroExactoBloqueadoGanaAPrefijoPermitir() {
        val decision = MotorFiltrado.evaluar(
            numero = "400900123",
            listaBlanca = emptyList(),
            numerosBloqueados = listOf(numero("400900123")),
            filtros = listOf(prefijo("400900", prioridad = 99, accion = Accion.PERMITIR))
        )

        assertTrue(decision.bloquear)
        assertEquals(ReglaTipo.NUMERO, decision.reglaTipo)
    }

    @Test
    fun numeroOcultoSePermite() {
        val decision = MotorFiltrado.evaluar("", emptyList(), emptyList(), emptyList())

        assertFalse(decision.bloquear)
        assertEquals(ReglaTipo.OCULTO, decision.reglaTipo)
    }

    @Test
    fun reglasInactivasSeIgnoran() {
        val decision = MotorFiltrado.evaluar(
            numero = "400123456",
            listaBlanca = listOf(numero("400123456", activo = false)),
            numerosBloqueados = listOf(numero("400123456", activo = false, id = 2)),
            filtros = listOf(prefijo("400", activo = false))
        )

        assertFalse(decision.bloquear)
        assertEquals(ReglaTipo.NINGUNA, decision.reglaTipo)
    }

    @Test
    fun sinReglasSePermite() {
        val decision = MotorFiltrado.evaluar("400123456", emptyList(), emptyList(), emptyList())

        assertFalse(decision.bloquear)
        assertEquals(ReglaTipo.NINGUNA, decision.reglaTipo)
    }

    @Test
    fun normalizaMas34CeroCeroTresCuatroEspaciosYGuiones() {
        val reglas = listOf(prefijo("400"))

        listOf("+34 400 123 456", "0034-400-123-456", "400123456").forEach { numero ->
            val decision = MotorFiltrado.evaluar(numero, emptyList(), emptyList(), reglas)
            assertTrue("Debería bloquear $numero", decision.bloquear)
            assertEquals(ReglaTipo.PREFIJO, decision.reglaTipo)
        }
    }

    @Test
    fun internacionalQueContiene400NoSeBloquea() {
        val decision = MotorFiltrado.evaluar(
            "+1400123456", emptyList(), emptyList(), listOf(prefijo("400"))
        )

        assertFalse(decision.bloquear)
        assertEquals(ReglaTipo.NINGUNA, decision.reglaTipo)
    }

    @Test
    fun empateDePrioridadGanaElPrefijoMasEspecifico() {
        val reglas = listOf(
            prefijo("400", prioridad = 5, accion = Accion.BLOQUEAR, id = 1),
            prefijo("4009", prioridad = 5, accion = Accion.PERMITIR, id = 2)
        )

        val decision = MotorFiltrado.evaluar("400912345", emptyList(), emptyList(), reglas)

        assertFalse(decision.bloquear)
        assertEquals("4009", decision.reglaValor)
    }
}
