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
import com.example.filtro400.data.ListaBlancaRepository
import com.example.filtro400.data.NumerosBloqueadosRepository
import com.example.filtro400.data.Preferencias
import com.example.filtro400.data.RegistroLlamadasRepository
import com.example.filtro400.domain.Decision
import com.example.filtro400.domain.MotorFiltrado
import com.example.filtro400.util.enmascararNumero
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class ResultadoSimulacion(
    val decision: Decision,
    val numeroEnmascarado: String,
    val momento: LocalDateTime
)

class SimuladorViewModel(application: Application) : AndroidViewModel(application) {

    private val helper = BaseDatos.obtener(application)
    private val filtros = FiltroRepository(helper)
    private val bloqueados = NumerosBloqueadosRepository(helper)
    private val blanca = ListaBlancaRepository(helper)
    private val franjas = FranjaNoMolestarRepository(helper)
    private val registro = RegistroLlamadasRepository(helper)

    var resultado by mutableStateOf<ResultadoSimulacion?>(null)
        private set
    var probando by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun simular(
        numero: String,
        momento: LocalDateTime = LocalDateTime.now(),
        registrar: Boolean = true
    ) {
        if (probando) return
        viewModelScope.launch {
            probando = true
            try {
                val ajustes = Preferencias.leer(getApplication())
                val decision = MotorFiltrado.evaluar(
                    numero = numero,
                    listaBlanca = blanca.obtenerActivos(),
                    numerosBloqueados = bloqueados.obtenerActivos(),
                    filtros = filtros.obtenerActivosBloqueante(),
                    franjasNoMolestar = franjas.obtenerActivasBloqueante(),
                    modoNoMolestarActivo = ajustes.modoNoMolestarActivo,
                    momento = momento,
                    bloquearOcultos = ajustes.bloquearOcultos
                )
                if (registrar) {
                    registro.registrar(decision, numero, simulada = true, fecha = System.currentTimeMillis())
                }
                resultado = ResultadoSimulacion(decision, enmascararNumero(numero), momento)
                error = null
            } catch (e: Exception) {
                error = "No se pudo simular la llamada"
            } finally {
                probando = false
            }
        }
    }

    fun limpiar() {
        resultado = null
        error = null
    }
}
