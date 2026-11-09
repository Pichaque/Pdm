package com.example.filtro400.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Ajustes(
    val modoNoMolestarActivo: Boolean = false,
    val desviarBuzon: Boolean = true,
    val registrarPermitidas: Boolean = true,
    val bloquearOcultos: Boolean = false,
    val notificarBloqueos: Boolean = false,
    val colorDinamico: Boolean = false
)

// Preferencias ligeras que UI y servicio comparten.
object Preferencias {

    private const val ARCHIVO = "ajustes_filtro400"
    private const val CLAVE_NO_MOLESTAR = "modo_no_molestar_activo"
    private const val CLAVE_DESVIAR_BUZON = "desviar_buzon"
    private const val CLAVE_REGISTRAR_PERMITIDAS = "registrar_permitidas"
    private const val CLAVE_BLOQUEAR_OCULTOS = "bloquear_ocultos"
    private const val CLAVE_NOTIFICAR_BLOQUEOS = "notificar_bloqueos"
    private const val CLAVE_COLOR_DINAMICO = "color_dinamico"

    private val flujo = MutableStateFlow(Ajustes())
    @Volatile
    private var inicializado = false

    fun leer(context: Context): Ajustes {
        val prefs = context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
        return Ajustes(
            modoNoMolestarActivo = prefs.getBoolean(CLAVE_NO_MOLESTAR, false),
            desviarBuzon = prefs.getBoolean(CLAVE_DESVIAR_BUZON, true),
            registrarPermitidas = prefs.getBoolean(CLAVE_REGISTRAR_PERMITIDAS, true),
            bloquearOcultos = prefs.getBoolean(CLAVE_BLOQUEAR_OCULTOS, false),
            notificarBloqueos = prefs.getBoolean(CLAVE_NOTIFICAR_BLOQUEOS, false),
            colorDinamico = prefs.getBoolean(CLAVE_COLOR_DINAMICO, false)
        )
    }

    fun observar(context: Context): StateFlow<Ajustes> {
        if (!inicializado) {
            synchronized(this) {
                if (!inicializado) {
                    flujo.value = leer(context)
                    inicializado = true
                }
            }
        }
        return flujo.asStateFlow()
    }

    fun guardar(context: Context, ajustes: Ajustes) {
        context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(CLAVE_NO_MOLESTAR, ajustes.modoNoMolestarActivo)
            .putBoolean(CLAVE_DESVIAR_BUZON, ajustes.desviarBuzon)
            .putBoolean(CLAVE_REGISTRAR_PERMITIDAS, ajustes.registrarPermitidas)
            .putBoolean(CLAVE_BLOQUEAR_OCULTOS, ajustes.bloquearOcultos)
            .putBoolean(CLAVE_NOTIFICAR_BLOQUEOS, ajustes.notificarBloqueos)
            .putBoolean(CLAVE_COLOR_DINAMICO, ajustes.colorDinamico)
            .apply()
        flujo.value = ajustes
        inicializado = true
    }
}
