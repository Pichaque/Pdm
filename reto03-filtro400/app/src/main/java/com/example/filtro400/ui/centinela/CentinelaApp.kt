package com.example.filtro400.ui.centinela

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private enum class Seccion(val titulo: String, val icono: ImageVector) {
    INICIO("Inicio", Icons.Filled.Home),
    REGLAS("Reglas", Icons.AutoMirrored.Filled.List),
    HORARIOS("Horarios", Icons.Filled.DateRange),
    REGISTRO("Registro", Icons.Filled.Call),
    MAS("Más", Icons.Filled.MoreVert)
}

private fun tituloDetalle(destino: DestinoMas): String = when (destino) {
    DestinoMas.ESTADISTICAS -> "Estadísticas"
    DestinoMas.SIMULADOR -> "Simulador"
    DestinoMas.IMPORTAR -> "Importar / Exportar"
    DestinoMas.AJUSTES -> "Ajustes"
    DestinoMas.ACERCA -> "Acerca de Centinela"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CentinelaApp() {
    var seccionNombre by rememberSaveable { mutableStateOf(Seccion.INICIO.name) }
    var destinoNombre by rememberSaveable { mutableStateOf<String?>(null) }
    val seccion = Seccion.valueOf(seccionNombre)
    val destino = destinoNombre?.let { DestinoMas.valueOf(it) }

    Scaffold(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            if (destino != null) {
                TopAppBar(
                    title = { Text(tituloDetalle(destino)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
                        titleContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                    ),
                    navigationIcon = {
                        IconButton(onClick = { destinoNombre = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    }
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                tonalElevation = 3.dp
            ) {
                Seccion.entries.forEach { item ->
                    NavigationBarItem(
                        selected = seccion == item,
                        onClick = {
                            seccionNombre = item.name
                            destinoNombre = null
                        },
                        icon = { Icon(item.icono, contentDescription = null) },
                        label = { Text(item.titulo) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            unselectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { relleno ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
        ) {
            when (seccion) {
                Seccion.INICIO -> PantallaInicio(
                    onIrAReglas = { seccionNombre = Seccion.REGLAS.name },
                    onIrAHorarios = { seccionNombre = Seccion.HORARIOS.name },
                    onIrARegistro = { seccionNombre = Seccion.REGISTRO.name }
                )
                Seccion.REGLAS -> PantallaReglas()
                Seccion.HORARIOS -> PantallaHorarios()
                Seccion.REGISTRO -> PantallaRegistro()
                Seccion.MAS -> {
                    if (destino == null) {
                        PantallaMas(onAbrir = { destinoNombre = it.name })
                    } else {
                        when (destino) {
                            DestinoMas.ESTADISTICAS -> PantallaEstadisticas()
                            DestinoMas.SIMULADOR -> PantallaSimulador()
                            DestinoMas.IMPORTAR -> PantallaImportExport()
                            DestinoMas.AJUSTES -> PantallaAjustes()
                            DestinoMas.ACERCA -> PantallaAcercaDe()
                        }
                    }
                }
            }
        }
    }
}
