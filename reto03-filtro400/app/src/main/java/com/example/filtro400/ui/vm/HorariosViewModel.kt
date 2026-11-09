package com.example.filtro400.ui.vm

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.filtro400.data.Ajustes
import com.example.filtro400.data.BaseDatos
import com.example.filtro400.data.FranjaNoMolestar
import com.example.filtro400.data.FranjaNoMolestarRepository
import com.example.filtro400.data.Preferencias
import com.example.filtro400.domain.Horario
import kotlinx.coroutines.launch

class HorariosViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio = FranjaNoMolestarRepository(BaseDatos.obtener(application))

    var franjas by mutableStateOf<List<FranjaNoMolestar>>(emptyList())
        private set
    var ajustes by mutableStateOf(Preferencias.leer(application))
        private set
    var cargando by mutableStateOf(true)
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
                franjas = repositorio.obtenerTodas()
                ajustes = Preferencias.leer(getApplication())
            } catch (e: Exception) {
                mensaje = "No se pudieron cargar los horarios"
            } finally {
                cargando = false
            }
        }
    }

    fun cambiarModoNoMolestar(activo: Boolean) {
        val nuevos = ajustes.copy(modoNoMolestarActivo = activo)
        Preferencias.guardar(getApplication(), nuevos)
        ajustes = nuevos
    }

    fun guardar(
        original: FranjaNoMolestar?,
        horario: Horario,
        activo: Boolean,
        alTerminar: (String?) -> Unit
    ) {
        viewModelScope.launch {
            val resultado = if (original == null) {
                repositorio.insertar(horario, activo)
            } else {
                repositorio.actualizar(original.id, horario, activo)
            }
            when (resultado) {
                is FranjaNoMolestarRepository.Resultado.Ok -> {
                    mensaje = if (original == null) "Franja anadida" else "Franja actualizada"
                    recargar()
                    alTerminar(null)
                }
                is FranjaNoMolestarRepository.Resultado.Error ->
                    alTerminar(resultado.mensaje)
            }
        }
    }

    fun cambiarEstado(franja: FranjaNoMolestar, activo: Boolean) {
        viewModelScope.launch {
            if (!repositorio.cambiarEstado(franja.id, activo)) {
                mensaje = "No se pudo actualizar la franja"
            }
            recargar()
        }
    }

    fun eliminar(franja: FranjaNoMolestar, alTerminar: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val eliminada = repositorio.eliminar(franja.id)
            recargar()
            alTerminar(eliminada)
        }
    }

    fun restaurar(horario: Horario, activo: Boolean) {
        viewModelScope.launch {
            repositorio.insertar(horario, activo)
            mensaje = "Franja restaurada"
            recargar()
        }
    }

    fun guardarAjustes(nuevos: Ajustes) {
        Preferencias.guardar(getApplication(), nuevos)
        ajustes = nuevos
    }

    fun consumirMensaje() {
        mensaje = null
    }

    private suspend fun recargar() {
        franjas = repositorio.obtenerTodas()
        ajustes = Preferencias.leer(getApplication())
        cargando = false
    }
}
