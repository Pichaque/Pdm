package com.example.filtro400

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.filtro400.data.FiltroDbHelper
import com.example.filtro400.data.RegistroLlamadasRepository
import com.example.filtro400.domain.Decision
import com.example.filtro400.domain.ReglaTipo
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RegistroLlamadasPrivacidadTest {

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private val nombreBd = "registro_privacidad_test.db"
    private lateinit var helper: FiltroDbHelper
    private lateinit var repositorio: RegistroLlamadasRepository

    @Before
    fun preparar() {
        contexto.deleteDatabase(nombreBd)
        helper = FiltroDbHelper(contexto, nombreBd)
        repositorio = RegistroLlamadasRepository(helper)
    }

    @After
    fun limpiar() {
        helper.close()
        contexto.deleteDatabase(nombreBd)
    }

    @Test
    fun conservaNumeroTemporalHasta48HorasYNoLoExponeComoRegla() = runBlocking {
        val ahora = System.currentTimeMillis()
        val numero = "+34 612 345 678"
        repositorio.registrar(
            decision = Decision(true, ReglaTipo.NUMERO, "612345678"),
            numero = numero,
            simulada = false,
            fecha = ahora
        )

        val vigente = repositorio.obtenerTodos().single()
        assertNotNull(vigente.numeroCompleto)
        assertNull(vigente.reglaValor)

        repositorio.purgarRegistrosExpirados(ahora + 48L * 60L * 60L * 1000L)

        val expirado = repositorio.obtenerTodos().single()
        assertNull(expirado.numeroCompleto)
        assertNull(expirado.reglaValor)
    }
}
