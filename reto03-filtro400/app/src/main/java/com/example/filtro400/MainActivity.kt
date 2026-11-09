package com.example.filtro400

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.filtro400.data.Preferencias
import com.example.filtro400.ui.centinela.CentinelaApp
import com.example.filtro400.ui.theme.CentinelaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val ajustes by Preferencias.observar(this).collectAsState()
            CentinelaTheme(dynamicColor = ajustes.colorDinamico) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CentinelaApp()
                }
            }
        }
    }
}
