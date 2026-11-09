package com.example.filtro400.ui.centinela

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.filtro400.data.RegistroLlamada
import com.example.filtro400.ui.vm.HistorialViewModel
import com.example.filtro400.util.formatearFechaHora

@Composable
fun PantallaRegistro(vm: HistorialViewModel = viewModel()) {
    val snackbar = remember { SnackbarHostState() }
    var seleccionado by remember { mutableStateOf<RegistroLlamada?>(null) }
    var confirmarBorrado by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.cargar() }
    LaunchedEffect(vm.mensaje) {
        vm.mensaje?.let {
            snackbar.showSnackbar(it)
            vm.consumirMensaje()
        }
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) }
    ) { relleno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Encabezado(
                        titulo = "Registro",
                        subtitulo = "Llamadas evaluadas por Centinela"
                    )
                }
                IconButton(onClick = { confirmarBorrado = true }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Borrar historial",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (vm.cargando) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (!vm.cargando && vm.registros.isEmpty()) {
                EstadoVacio(
                    icono = Icons.Filled.Call,
                    titulo = "Todavía no hay llamadas",
                    texto = "Cuando Centinela evalúe una llamada, aparecerá aquí con el número " +
                        "enmascarado y la regla que decidió."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "Toca una llamada para bloquear ese número o añadirlo a la " +
                                "lista blanca. Solo disponible durante 48 horas y nunca se " +
                                "muestra el número completo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(vm.registros, key = { it.id }) { registro ->
                        Card(
                            onClick = { seleccionado = registro },
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = registro.numeroEnmascarado,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    ChipDecision(registro.decision)
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ChipRegla(registro.reglaTipo)
                                        registro.reglaValor?.let {
                                            Text(
                                                text = it,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        text = formatearFechaHora(registro.fecha),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (registro.simulada) {
                                    Spacer(Modifier.height(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "Simulación",
                                            modifier = Modifier.padding(
                                                horizontal = 8.dp, vertical = 2.dp
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    seleccionado?.let { registro ->
        AlertDialog(
            onDismissRequest = { seleccionado = null },
            title = { Text(registro.numeroEnmascarado) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (registro.numeroCompleto == null) {
                            "El número completo ya no está disponible (retención de 48 horas)."
                        } else {
                            "¿Qué quieres hacer con este número?"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Decisión: ${textoRegla(registro.reglaTipo)} · " +
                            formatearFechaHora(registro.fecha),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.bloquearNumero(registro)
                        seleccionado = null
                    },
                    enabled = registro.numeroCompleto != null
                ) {
                    Text("Bloquear")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            vm.permitirNumero(registro)
                            seleccionado = null
                        },
                        enabled = registro.numeroCompleto != null
                    ) {
                        Text("A lista blanca")
                    }
                    TextButton(onClick = { seleccionado = null }) {
                        Text("Cerrar")
                    }
                }
            }
        )
    }

    if (confirmarBorrado) {
        AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            title = { Text("Borrar historial") },
            text = { Text("Se eliminarán todas las llamadas registradas. Esta acción no se " +
                "puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarBorrado = false
                    vm.borrarTodo()
                }) {
                    Text("Borrar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarBorrado = false }) { Text("Cancelar") }
            }
        )
    }
}

