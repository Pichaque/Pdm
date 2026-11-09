package com.example.filtro400.data

import android.content.Context

// Una única instancia del helper para toda la app (UI y servicio).
object BaseDatos {
    @Volatile
    private var instancia: FiltroDbHelper? = null

    fun obtener(context: Context): FiltroDbHelper =
        instancia ?: synchronized(this) {
            instancia ?: FiltroDbHelper(context.applicationContext).also { instancia = it }
        }
}
