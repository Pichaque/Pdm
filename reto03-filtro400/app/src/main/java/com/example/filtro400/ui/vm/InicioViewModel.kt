package com.example.filtro400.ui.vm

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.filtro400.data.BaseDatos
import com.example.filtro400.data.FiltroRepository
import com.example.filtro400.data.FranjaNoMolestarRepository
import com.example.filtro400.data.NumerosBloqueadosRepository
import com.example.filtro400.data.Preferencias
import com.example.filtro400.data.RegistroLlamadasRepository
import com.example.filtro400.domain.Horario
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class ResumenInicio(
    val reglasActivas: Int = 0,
    val bloqueadasHoy: Int = 0,
    val permitidasHoy: Int = 0,
    val noMolestarActivo: Boolean = false,
    val noMolestarAhora: Boolean = false,
    val proximaFranja: Horario? = null
)

class InicioViewModel(application: Application) : AndroidViewModel(application) {

    private val helper = BaseDatos.obtener(application)
    private val filtros = FiltroRepository(helper)
    private val bloqueados = NumerosBloqueadosRepository(helper)
    private val franjas = FranjaNoMolestarRepository(helper)
    private val registro = RegistroLlamadasRepository(helper)

    var resumen by mutableStateOf(ResumenInicio())
        private set
    var cargando by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init {
        recargar()
    }

    fun recargar() {
        viewModelScope.launch {
            cargando = true
            try {
                val ahora = LocalDateTime.now()
                val ajustes = Preferencias.leer(getApplication())
                val franjasActivas = franjas.obtenerActivas()
                val (bloqueadas, permitidas) = registro.contarHoy()
                val actual = franjasActivas.firstOrNull { it.horario.contiene(ahora) }
                val proxima = franjasActivas
                    .mapNotNull { it.horario.proximaOcurrencia(ahora) }
                    .minOrNull()
                val proximaHorario = proxima?.let { momento ->
                    franjasActivas.firstOrNull { it.horario.proximaOcurrencia(ahora) == momento }?.horario
                }

                resumen = ResumenInicio(
                    reglasActivas = filtros.obtenerTodos().count { it.activo } +
                        bloqueados.obtenerTodos().count { it.activo },
                    bloqueadasHoy = bloqueadas,
                    permitidasHoy = permitidas,
                    noMolestarActivo = ajustes.modoNoMolestarActivo,
                    noMolestarAhora = ajustes.modoNoMolestarActivo && actual != null,
                    proximaFranja = proximaHorario
                )
                error = null
            } catch (e: Exception) {
                error = "No se pudo cargar el resumen"
            } finally {
                cargando = false
            }
        }
    }
}
