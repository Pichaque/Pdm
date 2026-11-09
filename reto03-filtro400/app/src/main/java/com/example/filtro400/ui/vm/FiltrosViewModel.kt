package com.example.filtro400.ui.vm

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.filtro400.data.Accion
import com.example.filtro400.data.BaseDatos
import com.example.filtro400.data.Filtro
import com.example.filtro400.data.FiltroRepository
import com.example.filtro400.domain.Horario
import com.example.filtro400.util.CsvReglas
import kotlinx.coroutines.launch

class FiltrosViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio = FiltroRepository(BaseDatos.obtener(application))

    var filtros by mutableStateOf<List<Filtro>>(emptyList())
        private set
    var cargando by mutableStateOf(true)
        private set
    var errorCarga by mutableStateOf<String?>(null)
        private set
    var mensaje by mutableStateOf<String?>(null)
        private set

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            cargando = true
            try {
                filtros = repositorio.obtenerTodos()
                errorCarga = null
            } catch (e: Exception) {
                errorCarga = "No se pudieron cargar los filtros"
            } finally {
                cargando = false
            }
        }
    }

    fun guardar(
        filtro: Filtro?,
        prefijo: String,
        prioridad: Int,
        accion: Accion,
        horario: Horario?,
        alTerminar: (String?) -> Unit
    ) {
        viewModelScope.launch {
            val resultado = if (filtro == null) {
                repositorio.insertar(prefijo, prioridad, accion, horario)
            } else {
                repositorio.actualizar(filtro.id, prefijo, prioridad, accion, horario)
            }

            when (resultado) {
                is FiltroRepository.Resultado.Ok -> {
                    recargar()
                    mensaje = if (filtro == null) "Filtro anadido" else "Filtro actualizado"
                    alTerminar(null)
                }
                FiltroRepository.Resultado.Duplicado ->
                    alTerminar("Ese prefijo ya existe")
                is FiltroRepository.Resultado.Error ->
                    alTerminar(resultado.mensaje.ifEmpty { "Error" })
            }
        }
    }

    fun cambiarEstado(filtro: Filtro, activo: Boolean) {
        viewModelScope.launch {
            if (!repositorio.cambiarEstado(filtro.id, activo)) {
                mensaje = "No se pudo actualizar el filtro"
            }
            recargar()
        }
    }

    fun eliminar(filtro: Filtro, alTerminar: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val eliminado = repositorio.eliminar(filtro.id)
            recargar()
            alTerminar(eliminado)
        }
    }

    fun importar(texto: String) {
        viewModelScope.launch {
            val resultado = CsvReglas.importarFiltros(texto)
            var nuevos = 0
            var duplicados = 0

            resultado.filas.forEach { fila ->
                when (
                    repositorio.insertar(
                        fila.prefijo,
                        fila.prioridad,
                        fila.accion,
                        fila.horario
                    )
                ) {
                    is FiltroRepository.Resultado.Ok -> nuevos++
                    FiltroRepository.Resultado.Duplicado -> duplicados++
                    is FiltroRepository.Resultado.Error -> Unit
                }
            }

            recargar()
            mensaje = "Importados: $nuevos - duplicados: $duplicados - no validos: ${resultado.invalidas}"
        }
    }

    fun exportar(): String = CsvReglas.exportarFiltros(filtros)

    fun consumirMensaje() {
        mensaje = null
    }

    private suspend fun recargar() {
        filtros = repositorio.obtenerTodos()
        errorCarga = null
        cargando = false
    }
}

