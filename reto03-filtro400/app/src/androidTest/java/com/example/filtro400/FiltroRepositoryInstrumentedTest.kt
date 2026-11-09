package com.example.filtro400

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.filtro400.data.FiltroDbHelper
import com.example.filtro400.data.FiltroRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// CRUD básico sobre una base de datos de prueba independiente.
@RunWith(AndroidJUnit4::class)
class FiltroRepositoryInstrumentedTest {

    private lateinit var context: Context
    private lateinit var helper: FiltroDbHelper
    private lateinit var repositorio: FiltroRepository

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(NOMBRE_BD_PRUEBA)
        helper = FiltroDbHelper(context, NOMBRE_BD_PRUEBA)
        repositorio = FiltroRepository(helper)
    }

    @After
    fun tearDown() {
        helper.close()
        context.deleteDatabase(NOMBRE_BD_PRUEBA)
    }

    @Test
    fun crudCompleto() = runBlocking {
        // Primer acceso: se crea la tabla y se siembra el 400 de la Fase 1.
        val iniciales = repositorio.obtenerTodos()
        assertEquals(1, iniciales.size)
        assertEquals("400", iniciales.first().prefijo)
        assertTrue(iniciales.first().activo)

        // Insertar.
        val insertado = repositorio.insertar("900")
        assertTrue(insertado is FiltroRepository.Resultado.Ok)
        val id900 = (insertado as FiltroRepository.Resultado.Ok).id
        assertEquals(2, repositorio.obtenerTodos().size)

        // Duplicado: devuelve resultado, no lanza excepción.
        assertEquals(FiltroRepository.Resultado.Duplicado, repositorio.insertar("900"))

        // Activar/desactivar.
        assertTrue(repositorio.cambiarEstado(id900, false))
        assertFalse(repositorio.obtenerActivos().any { it.prefijo == "900" })

        // Modificar.
        assertTrue(repositorio.actualizar(id900, "901") is FiltroRepository.Resultado.Ok)
        assertTrue(repositorio.obtenerTodos().any { it.prefijo == "901" })

        // Eliminar.
        assertTrue(repositorio.eliminar(id900))
        assertEquals(1, repositorio.obtenerTodos().size)
    }

    @Test
    fun normalizaElPrefijoAlGuardar() = runBlocking {
        assertTrue(repositorio.insertar("+34 900-123") is FiltroRepository.Resultado.Ok)
        assertTrue(repositorio.obtenerTodos().any { it.prefijo == "900123" })
    }

    private companion object {
        const val NOMBRE_BD_PRUEBA = "filtros_test.db"
    }
}
