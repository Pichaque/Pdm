package com.example.filtro400.ui.vm

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.filtro400.data.BaseDatos
import com.example.filtro400.data.ListaBlancaRepository
import com.example.filtro400.data.NumeroLista
import com.example.filtro400.data.NumeroRepository
import com.example.filtro400.data.NumerosBloqueadosRepository
import com.example.filtro400.domain.Horario
import com.example.filtro400.util.CsvReglas
import kotlinx.coroutines.launch

enum class ListaTipo {
    BLOQUEADOS,
    BLANCA
}

class ListasViewModel(application: Application) : AndroidViewModel(application) {

    private val helper = BaseDatos.obtener(application)
    private val bloqueadosRepo = NumerosBloqueadosRepository(helper)
    private val blancaRepo = ListaBlancaRepository(helper)

    var bloqueados by mutableStateOf<List<NumeroLista>>(emptyList())
        private set
    var blanca by mutableStateOf<List<NumeroLista>>(emptyList())
        private set
    var cargando by mutableStateOf(true)
        private set
    var mensaje by mutableStateOf<String?>(null)
        private set

    init {
        cargar()
    }

    fun numeros(tipo: ListaTipo): List<NumeroLista> =
        if (tipo == ListaTipo.BLOQUEADOS) bloqueados else blanca

    fun cargar() {
        viewModelScope.launch {
            cargando = true
            try {
                bloqueados = bloqueadosRepo.obtenerTodos()
                blanca = blancaRepo.obtenerTodos()
            } catch (e: Exception) {
                mensaje = "No se pudieron cargar las listas"
            } finally {
                cargando = false
            }
        }
    }

    fun guardar(
        tipo: ListaTipo,
        original: NumeroLista?,
        numero: String,
        etiqueta: String,
        activo: Boolean,
        horario: Horario?,
        alTerminar: (String?) -> Unit
    ) {
        viewModelScope.launch {
            val repo = repositorio(tipo)
            val resultado = if (original == null) {
                repo.insertar(numero, etiqueta, activo, horario)
            } else {
                repo.actualizar(original.id, numero, etiqueta, activo, horario)
            }

            when (resultado) {
                is NumeroRepository.Resultado.Ok -> {
                    recargar(tipo)
                    mensaje = if (original == null) "Numero anadido" else "Numero actualizado"
                    alTerminar(null)
                }
                NumeroRepository.Resultado.Duplicado ->
                    alTerminar("Ese numero ya esta en esta lista")
                is NumeroRepository.Resultado.Error ->
                    alTerminar(resultado.mensaje)
            }
        }
    }

    fun cambiarEstado(tipo: ListaTipo, item: NumeroLista, activo: Boolean) {
        viewModelScope.launch {
            if (!repositorio(tipo).cambiarEstado(item.id, activo)) {
                mensaje = "No se pudo actualizar el numero"
            }
            recargar(tipo)
        }
    }

    fun eliminar(
        tipo: ListaTipo,
        item: NumeroLista,
        alTerminar: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val eliminado = repositorio(tipo).eliminar(item.id)
            recargar(tipo)
            alTerminar(eliminado)
        }
    }

    fun importar(tipo: ListaTipo, texto: String) {
        viewModelScope.launch {
            val resultado = CsvReglas.importarNumeros(texto)
            val repo = repositorio(tipo)
            var nuevos = 0
            var duplicados = 0

            resultado.filas.forEach { fila ->
                when (repo.insertar(fila.numero, fila.etiqueta, fila.activo)) {
                    is NumeroRepository.Resultado.Ok -> nuevos++
                    NumeroRepository.Resultado.Duplicado -> duplicados++
                    is NumeroRepository.Resultado.Error -> Unit
                }
            }

            recargar(tipo)
            mensaje = "Importados: $nuevos - duplicados: $duplicados - no validos: ${resultado.invalidas}"
        }
    }

    fun exportar(tipo: ListaTipo): String = CsvReglas.exportarNumeros(numeros(tipo))

    fun consumirMensaje() {
        mensaje = null
    }

    private fun repositorio(tipo: ListaTipo): NumeroRepository =
        if (tipo == ListaTipo.BLOQUEADOS) bloqueadosRepo else blancaRepo

    private suspend fun recargar(tipo: ListaTipo) {
        if (tipo == ListaTipo.BLOQUEADOS) {
            bloqueados = bloqueadosRepo.obtenerTodos()
        } else {
            blanca = blancaRepo.obtenerTodos()
        }
        cargando = false
    }
}
