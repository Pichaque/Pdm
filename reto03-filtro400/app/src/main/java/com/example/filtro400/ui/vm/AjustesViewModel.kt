package com.example.filtro400.ui.vm

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.filtro400.data.Ajustes
import com.example.filtro400.data.Preferencias
import kotlinx.coroutines.launch

class AjustesViewModel(application: Application) : AndroidViewModel(application) {

    var ajustes by mutableStateOf(Preferencias.leer(application))
        private set

    init {
        viewModelScope.launch {
            Preferencias.observar(getApplication()).collect { ajustes = it }
        }
    }

    fun guardar(nuevos: Ajustes) {
        Preferencias.guardar(getApplication(), nuevos)
        ajustes = nuevos
    }
}
