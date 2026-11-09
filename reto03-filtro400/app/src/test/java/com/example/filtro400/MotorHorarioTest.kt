package com.example.filtro400

import com.example.filtro400.data.Accion
import com.example.filtro400.data.Filtro
import com.example.filtro400.data.FranjaNoMolestar
import com.example.filtro400.data.NumeroLista
import com.example.filtro400.domain.DiaSemana
import com.example.filtro400.domain.Horario
import com.example.filtro400.domain.MotorFiltrado
import com.example.filtro400.domain.ReglaTipo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class MotorHorarioTest {

    private val lunesLaboral = Horario(
        dias = DiaSemana.laborables(),
        inicioMinuto = 9 * 60,
        finMinuto = 18 * 60
    )

    private val lunesMediodia = LocalDateTime.of(2026, 10, 26, 12, 0)
    private val lunesNoche = LocalDateTime.of(2026, 10, 26, 22, 0)

    private fun prefijo(
        valor: String,
        prioridad: Int = 0,
        accion: Accion = Accion.BLOQUEAR,
        horario: Horario? = null,
        id: Long = 1
    ) = Filtro(id, valor, true, prioridad, accion, horario)

    private fun numero(
        valor: String,
        id: Long = 1,
        horario: Horario? = null
    ) = NumeroLista(id, valor, "", true, horario)

    private fun franja(horario: Horario, activo: Boolean = true, id: Long = 1) =
        FranjaNoMolestar(id, horario, activo)

    @Test
    fun reglaConHorarioSoloBloqueaDentroDeSuFranja() {
        val regla = prefijo("400", horario = lunesLaboral)

        val dentro = MotorFiltrado.evaluar(
            numero = "400123456",
            listaBlanca = emptyList(),
            numerosBloqueados = emptyList(),
            filtros = listOf(regla),
            momento = lunesMediodia
        )
        val fuera = MotorFiltrado.evaluar(
            numero = "400123456",
            listaBlanca = emptyList(),
            numerosBloqueados = emptyList(),
            filtros = listOf(regla),
            momento = lunesNoche
        )

        assertTrue(dentro.bloquear)
        assertEquals(ReglaTipo.HORARIO, dentro.reglaTipo)
        assertFalse(fuera.bloquear)
        assertEquals(ReglaTipo.NINGUNA, fuera.reglaTipo)
    }

    @Test
    fun numeroBloqueadoConHorarioRespetaLaFranja() {
        val bloqueado = numero("400123456", horario = lunesLaboral)

        val dentro = MotorFiltrado.evaluar(
            "400123456", emptyList(), listOf(bloqueado), emptyList(), momento = lunesMediodia
        )
        val fuera = MotorFiltrado.evaluar(
            "400123456", emptyList(), listOf(bloqueado), emptyList(), momento = lunesNoche
        )

        assertTrue(dentro.bloquear)
        assertEquals(ReglaTipo.HORARIO, dentro.reglaTipo)
        assertFalse(fuera.bloquear)
    }

    @Test
    fun numeroBloqueadoSeNormalizaConPaisYEspacios() {
        val bloqueado = numero("612 345 678")

        listOf("+34 612 345 678", "0034612345678", "612345678").forEach { valor ->
            val decision = MotorFiltrado.evaluar(
                valor, emptyList(), listOf(bloqueado), emptyList(), momento = lunesMediodia
            )
            assertTrue("Deberia bloquear $valor", decision.bloquear)
            assertEquals(ReglaTipo.NUMERO, decision.reglaTipo)
        }
    }

    @Test
    fun noMolestarBloqueaDesconocidosDentroDeLaFranja() {
        val decision = MotorFiltrado.evaluar(
            numero = "612345678",
            listaBlanca = emptyList(),
            numerosBloqueados = emptyList(),
            filtros = emptyList(),
            franjasNoMolestar = listOf(franja(lunesLaboral)),
            modoNoMolestarActivo = true,
            momento = lunesMediodia
        )

        assertTrue(decision.bloquear)
        assertEquals(ReglaTipo.NO_MOLESTAR, decision.reglaTipo)
    }

    @Test
    fun noMolestarNoBloqueaFueraDeLaFranja() {
        val decision = MotorFiltrado.evaluar(
            numero = "612345678",
            listaBlanca = emptyList(),
            numerosBloqueados = emptyList(),
            filtros = emptyList(),
            franjasNoMolestar = listOf(franja(lunesLaboral)),
            modoNoMolestarActivo = true,
            momento = lunesNoche
        )

        assertFalse(decision.bloquear)
        assertEquals(ReglaTipo.NINGUNA, decision.reglaTipo)
    }

    @Test
    fun interruptorGeneralApagadoDesactivaNoMolestar() {
        val decision = MotorFiltrado.evaluar(
            numero = "612345678",
            listaBlanca = emptyList(),
            numerosBloqueados = emptyList(),
            filtros = emptyList(),
            franjasNoMolestar = listOf(franja(lunesLaboral)),
            modoNoMolestarActivo = false,
            momento = lunesMediodia
        )

        assertFalse(decision.bloquear)
    }

    @Test
    fun franjaInactivaSeIgnora() {
        val decision = MotorFiltrado.evaluar(
            numero = "612345678",
            listaBlanca = emptyList(),
            numerosBloqueados = emptyList(),
            filtros = emptyList(),
            franjasNoMolestar = listOf(franja(lunesLaboral, activo = false)),
            modoNoMolestarActivo = true,
            momento = lunesMediodia
        )

        assertFalse(decision.bloquear)
    }

    @Test
    fun listaBlancaGanaANoMolestar() {
        val decision = MotorFiltrado.evaluar(
            numero = "612345678",
            listaBlanca = listOf(numero("+34 612 345 678")),
            numerosBloqueados = emptyList(),
            filtros = emptyList(),
            franjasNoMolestar = listOf(franja(lunesLaboral)),
            modoNoMolestarActivo = true,
            momento = lunesMediodia
        )

        assertFalse(decision.bloquear)
        assertEquals(ReglaTipo.LISTA_BLANCA, decision.reglaTipo)
    }

    @Test
    fun reglaPermitirGanaANoMolestarPorOrdenDeEvaluacion() {
        val decision = MotorFiltrado.evaluar(
            numero = "400123456",
            listaBlanca = emptyList(),
            numerosBloqueados = emptyList(),
            filtros = listOf(prefijo("400", prioridad = 10, accion = Accion.PERMITIR)),
            franjasNoMolestar = listOf(franja(lunesLaboral)),
            modoNoMolestarActivo = true,
            momento = lunesMediodia
        )

        assertFalse(decision.bloquear)
        assertEquals(ReglaTipo.PREFIJO, decision.reglaTipo)
    }

    @Test
    fun numeroBloqueadoGanaANoMolestar() {
        val decision = MotorFiltrado.evaluar(
            numero = "612345678",
            listaBlanca = emptyList(),
            numerosBloqueados = listOf(numero("612345678")),
            filtros = emptyList(),
            franjasNoMolestar = listOf(franja(lunesLaboral)),
            modoNoMolestarActivo = true,
            momento = lunesMediodia
        )

        assertTrue(decision.bloquear)
        assertEquals(ReglaTipo.NUMERO, decision.reglaTipo)
    }

    @Test
    fun ocultosSeBloqueanSoloSiSeActivaElAjuste() {
        val permitido = MotorFiltrado.evaluar(
            "", emptyList(), emptyList(), emptyList(), momento = lunesMediodia
        )
        val bloqueado = MotorFiltrado.evaluar(
            "", emptyList(), emptyList(), emptyList(),
            momento = lunesMediodia, bloquearOcultos = true
        )

        assertFalse(permitido.bloquear)
        assertTrue(bloqueado.bloquear)
        assertEquals(ReglaTipo.OCULTO, bloqueado.reglaTipo)
    }

    @Test
    fun noMolestarTambienBloqueaOcultosSiNoSeBloqueanPorAjuste() {
        val decision = MotorFiltrado.evaluar(
            numero = "",
            listaBlanca = emptyList(),
            numerosBloqueados = emptyList(),
            filtros = emptyList(),
            franjasNoMolestar = listOf(franja(lunesLaboral)),
            modoNoMolestarActivo = true,
            momento = lunesMediodia
        )

        assertTrue(decision.bloquear)
        assertEquals(ReglaTipo.NO_MOLESTAR, decision.reglaTipo)
    }
}
