package com.example.filtro400.ui.vm

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.filtro400.ReglaBloqueo
import com.example.filtro400.data.BaseDatos
import com.example.filtro400.data.Estadisticas
import com.example.filtro400.data.ListaBlancaRepository
import com.example.filtro400.data.NumerosBloqueadosRepository
import com.example.filtro400.data.RegistroLlamada
import com.example.filtro400.data.RegistroLlamadasRepository
import kotlinx.coroutines.launch

class HistorialViewModel(application: Application) : AndroidViewModel(application) {

    private val helper = BaseDatos.obtener(application)
    private val repositorio = RegistroLlamadasRepository(helper)
    private val bloqueados = NumerosBloqueadosRepository(helper)
    private val blanca = ListaBlancaRepository(helper)

    var registros by mutableStateOf<List<RegistroLlamada>>(emptyList())
        private set
    var estadisticas by mutableStateOf(
        Estadisticas(0, 0, emptyList(), emptyList())
    )
        private set
    var cargando by mutableStateOf(true)
        private set
    var incluirSimuladas by mutableStateOf(false)
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
                registros = repositorio.obtenerTodos()
                estadisticas = repositorio.obtenerEstadisticas(incluirSimuladas)
            } catch (e: Exception) {
                mensaje = "No se pudo cargar el historial"
            } finally {
                cargando = false
            }
        }
    }

    fun cambiarIncluirSimuladas(valor: Boolean) {
        incluirSimuladas = valor
        viewModelScope.launch {
            estadisticas = repositorio.obtenerEstadisticas(valor)
        }
    }

    fun borrarTodo() {
        viewModelScope.launch {
            mensaje = if (repositorio.borrarTodo()) {
                "Historial borrado"
            } else {
                "No se pudo borrar el historial"
            }
            registros = repositorio.obtenerTodos()
            estadisticas = repositorio.obtenerEstadisticas(incluirSimuladas)
        }
    }

    // Acciones rapidas desde el registro. Solo funcionan durante las 48 h en las
    // que se conserva numeroCompleto; la UI nunca lo muestra.
    fun bloquearNumero(registro: RegistroLlamada) {
        val numero = registro.numeroCompleto
        if (numero.isNullOrBlank()) {
            mensaje = "El numero completo ya no esta disponible"
            return
        }
        viewModelScope.launch {
            val normalizado = ReglaBloqueo.normalizar(numero)
            val enBlanca = blanca.obtenerTodos().firstOrNull {
                ReglaBloqueo.normalizar(it.numero) == normalizado
            }
            if (enBlanca != null) {
                blanca.eliminar(enBlanca.id)
            }
            mensaje = when (bloqueados.insertar(numero, "Desde el registro")) {
                is com.example.filtro400.data.NumeroRepository.Resultado.Ok ->
                    "Numero bloqueado"
                com.example.filtro400.data.NumeroRepository.Resultado.Duplicado ->
                    "Ese numero ya estaba bloqueado"
                is com.example.filtro400.data.NumeroRepository.Resultado.Error ->
                    "No se pudo bloquear el numero"
            }
            cargar()
        }
    }

    fun permitirNumero(registro: RegistroLlamada) {
        val numero = registro.numeroCompleto
        if (numero.isNullOrBlank()) {
            mensaje = "El numero completo ya no esta disponible"
            return
        }
        viewModelScope.launch {
            val normalizado = ReglaBloqueo.normalizar(numero)
            val enBloqueados = bloqueados.obtenerTodos().firstOrNull {
                ReglaBloqueo.normalizar(it.numero) == normalizado
            }
            if (enBloqueados != null) {
                bloqueados.eliminar(enBloqueados.id)
            }
            mensaje = when (blanca.insertar(numero, "Desde el registro")) {
                is com.example.filtro400.data.NumeroRepository.Resultado.Ok ->
                    "Numero en lista blanca"
                com.example.filtro400.data.NumeroRepository.Resultado.Duplicado ->
                    "Ese numero ya estaba en la lista blanca"
                is com.example.filtro400.data.NumeroRepository.Resultado.Error ->
                    "No se pudo anadir a la lista blanca"
            }
            cargar()
        }
    }

    fun consumirMensaje() {
        mensaje = null
    }
}
