package com.example.filtro400.ui.centinela

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.filtro400.data.FranjaNoMolestar
import com.example.filtro400.domain.DiaSemana
import com.example.filtro400.domain.Horario
import com.example.filtro400.ui.vm.HorariosViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@Composable
fun PantallaHorarios(vm: HorariosViewModel = viewModel()) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var mostrarDialogo by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf<FranjaNoMolestar?>(null) }
    var eliminando by remember { mutableStateOf<FranjaNoMolestar?>(null) }
    var errorDialogo by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { vm.cargar() }
    LaunchedEffect(vm.mensaje) {
        vm.mensaje?.let {
            snackbar.showSnackbar(it)
            vm.consumirMensaje()
        }
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editando = null
                errorDialogo = null
                mostrarDialogo = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir franja")
            }
        }
    ) { relleno ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Encabezado(
                    titulo = "Horarios",
                    subtitulo = "Modo No molestar global y franjas de silencio"
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (vm.ajustes.modoNoMolestarActivo) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Modo No molestar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Bloquea todas las llamadas desconocidas dentro de las " +
                                    "franjas activas. La lista blanca siempre pasa.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = vm.ajustes.modoNoMolestarActivo,
                            onCheckedChange = vm::cambiarModoNoMolestar
                        )
                    }
                }
            }

            if (vm.franjas.isEmpty() && !vm.cargando) {
                item {
                    EstadoVacio(
                        icono = Icons.Filled.DateRange,
                        titulo = "Sin franjas de silencio",
                        texto = "Añade una franja con el botón + y activa el modo No molestar " +
                            "para bloquear llamadas desconocidas en ese horario."
                    )
                }
            }

            if (vm.cargando) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }

            items(vm.franjas, key = { it.id }) { franja ->
                val ahora = LocalDateTime.now()
                val activaAhora = franja.activo && franja.horario.contiene(ahora)
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = franja.horario.resumen(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (activaAhora) {
                                    Text(
                                        text = "Activa ahora",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            Switch(
                                checked = franja.activo,
                                onCheckedChange = { vm.cambiarEstado(franja, it) }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = {
                                editando = franja
                                errorDialogo = null
                                mostrarDialogo = true
                            }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Editar franja")
                            }
                            IconButton(onClick = { eliminando = franja }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Eliminar franja",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Orden de decisión: lista blanca, números y prefijos, después " +
                            "No molestar. Así una excepción siempre tiene la última palabra.",
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (mostrarDialogo) {
        DialogoHorario(
            titulo = if (editando == null) "Nueva franja" else "Editar franja",
            horarioInicial = editando?.horario
                ?: Horario(DiaSemana.todos(), 22 * 60, 8 * 60),
            textoAyuda = "Puedes cruzar la medianoche, por ejemplo de 22:00 a 08:00.",
            permitirSinHorario = false,
            onGuardar = { horario ->
                horario?.let {
                    vm.guardar(editando, it, editando?.activo ?: true) { mensaje ->
                        if (mensaje == null) {
                            mostrarDialogo = false
                        } else {
                            errorDialogo = mensaje
                        }
                    }
                }
            },
            onCancelar = { mostrarDialogo = false }
        )
        errorDialogo?.let {
            AlertDialog(
                onDismissRequest = { errorDialogo = null },
                title = { Text("No se pudo guardar") },
                text = { Text(it) },
                confirmButton = {
                    TextButton(onClick = { errorDialogo = null }) { Text("Cerrar") }
                }
            )
        }
    }

    eliminando?.let { franja ->
        AlertDialog(
            onDismissRequest = { eliminando = null },
            title = { Text("Eliminar franja") },
            text = { Text("¿Seguro que quieres eliminar la franja ${franja.horario.resumen()}?") },
            confirmButton = {
                TextButton(onClick = {
                    eliminando = null
                    vm.eliminar(franja) { eliminada ->
                        scope.launch {
                            if (!eliminada) {
                                snackbar.showSnackbar("No se pudo eliminar la franja")
                            } else {
                                val resultado = snackbar.showSnackbar(
                                    "Franja eliminada",
                                    actionLabel = "Deshacer",
                                    withDismissAction = true
                                )
                                if (resultado == SnackbarResult.ActionPerformed) {
                                    vm.restaurar(franja.horario, franja.activo)
                                }
                            }
                        }
                    }
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { eliminando = null }) { Text("Cancelar") }
            }
        )
    }
}

