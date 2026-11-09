package com.example.filtro400.ui.centinela

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.filtro400.ui.vm.FiltrosViewModel
import com.example.filtro400.ui.vm.ListaTipo
import com.example.filtro400.ui.vm.ListasViewModel
import com.example.filtro400.util.escribirTextoEnUri
import com.example.filtro400.util.leerTextoDeUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PantallaImportExport(
    vmFiltros: FiltrosViewModel = viewModel(),
    vmListas: ListasViewModel = viewModel()
) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var tipo by remember { mutableStateOf(ListaTipo.BLOQUEADOS) }

    LaunchedEffect(Unit) {
        vmFiltros.cargar()
        vmListas.cargar()
    }
    LaunchedEffect(vmFiltros.mensaje) {
        vmFiltros.mensaje?.let {
            snackbar.showSnackbar(it)
            vmFiltros.consumirMensaje()
        }
    }
    LaunchedEffect(vmListas.mensaje) {
        vmListas.mensaje?.let {
            snackbar.showSnackbar(it)
            vmListas.consumirMensaje()
        }
    }

    val exportarPrefijos = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        alcance.launch {
            val texto = vmFiltros.exportar()
            val ok = withContext(Dispatchers.IO) {
                escribirTextoEnUri(contexto, uri, texto)
            }
            snackbar.showSnackbar(
                if (ok) "Prefijos exportados" else "No se pudo exportar"
            )
        }
    }

    val importarPrefijos = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        alcance.launch {
            val texto = withContext(Dispatchers.IO) {
                leerTextoDeUri(contexto, uri)
            }
            if (texto == null) {
                snackbar.showSnackbar("No se pudo leer el archivo")
            } else {
                vmFiltros.importar(texto)
            }
        }
    }

    val exportarNumeros = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        alcance.launch {
            val texto = vmListas.exportar(tipo)
            val ok = withContext(Dispatchers.IO) {
                escribirTextoEnUri(contexto, uri, texto)
            }
            snackbar.showSnackbar(
                if (ok) "Números exportados" else "No se pudo exportar"
            )
        }
    }

    val importarNumeros = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        alcance.launch {
            val texto = withContext(Dispatchers.IO) {
                leerTextoDeUri(contexto, uri)
            }
            if (texto == null) {
                snackbar.showSnackbar("No se pudo leer el archivo")
            } else {
                vmListas.importar(tipo, texto)
            }
        }
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) }
    ) { relleno ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Los archivos CSV usan comas, codificación UTF-8 y cabecera opcional " +
                        "al importar. No se necesita ningún permiso de almacenamiento.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                SeccionDatos(
                    titulo = "Prefijos",
                    descripcion = "Formato: prefijo, prioridad, acción, activo, días, inicio, fin.",
                    onExportar = { exportarPrefijos.launch("centinela_prefijos.csv") },
                    onImportar = {
                        importarPrefijos.launch(
                            arrayOf("text/csv", "text/comma-separated-values", "text/plain")
                        )
                    }
                )
            }

            item {
                Text(
                    text = "Lista de números",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = tipo == ListaTipo.BLOQUEADOS,
                        onClick = { tipo = ListaTipo.BLOQUEADOS },
                        label = { Text("Bloqueados") }
                    )
                    FilterChip(
                        selected = tipo == ListaTipo.BLANCA,
                        onClick = { tipo = ListaTipo.BLANCA },
                        label = { Text("Lista blanca") }
                    )
                }
            }
            item {
                SeccionDatos(
                    titulo = if (tipo == ListaTipo.BLOQUEADOS) "Números bloqueados" else "Lista blanca",
                    descripcion = "Formato: numero, etiqueta, activo.",
                    onExportar = {
                        exportarNumeros.launch(
                            if (tipo == ListaTipo.BLOQUEADOS) {
                                "centinela_bloqueados.csv"
                            } else {
                                "centinela_lista_blanca.csv"
                            }
                        )
                    },
                    onImportar = {
                        importarNumeros.launch(
                            arrayOf("text/csv", "text/comma-separated-values", "text/plain")
                        )
                    }
                )
            }

            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Text(
                        text = "Al importar se ignoran las filas no válidas y los duplicados. " +
                            "La app nunca sobrescribe una regla existente sin avisar.",
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SeccionDatos(
    titulo: String,
    descripcion: String,
    onExportar: () -> Unit,
    onImportar: () -> Unit
) {
    Card(shape = RoundedCornerShape(18.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onExportar, modifier = Modifier.weight(1f)) {
                    Text("Exportar")
                }
                OutlinedButton(onClick = onImportar, modifier = Modifier.weight(1f)) {
                    Text("Importar")
                }
            }
        }
    }
}

