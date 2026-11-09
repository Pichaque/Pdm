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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.filtro400.ReglaBloqueo
import com.example.filtro400.data.Accion
import com.example.filtro400.data.Filtro
import com.example.filtro400.data.NumeroLista
import com.example.filtro400.domain.Horario
import com.example.filtro400.ui.vm.FiltrosViewModel
import com.example.filtro400.ui.vm.ListaTipo
import com.example.filtro400.ui.vm.ListasViewModel
import kotlinx.coroutines.launch

private enum class PestanaReglas { PREFIJOS, BLOQUEADOS, BLANCA }

@Composable
fun PantallaReglas(
    vmFiltros: FiltrosViewModel = viewModel(),
    vmListas: ListasViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var pestanaNombre by rememberSaveable { mutableStateOf(PestanaReglas.PREFIJOS.name) }
    val pestana = PestanaReglas.valueOf(pestanaNombre)

    var mostrarPrefijo by remember { mutableStateOf(false) }
    var prefijoEditando by remember { mutableStateOf<Filtro?>(null) }
    var eliminarPrefijo by remember { mutableStateOf<Filtro?>(null) }

    var mostrarNumero by remember { mutableStateOf(false) }
    var numeroEditando by remember { mutableStateOf<NumeroLista?>(null) }
    var numeroTipo by remember { mutableStateOf(ListaTipo.BLOQUEADOS) }
    var eliminarNumero by remember { mutableStateOf<NumeroLista?>(null) }
    var busqueda by rememberSaveable { mutableStateOf("") }

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

    fun borrarPrefijo(filtro: Filtro) {
        vmFiltros.eliminar(filtro) { eliminado ->
            scope.launch {
                if (!eliminado) {
                    snackbar.showSnackbar("No se pudo eliminar el filtro")
                } else {
                    val resultado = snackbar.showSnackbar(
                        "Filtro ${filtro.prefijo} eliminado",
                        actionLabel = "Deshacer",
                        withDismissAction = true
                    )
                    if (resultado == SnackbarResult.ActionPerformed) {
                        vmFiltros.guardar(
                            null,
                            filtro.prefijo,
                            filtro.prioridad,
                            filtro.accion,
                            filtro.horario
                        ) {}
                    }
                }
            }
        }
    }

    fun borrarNumero(tipo: ListaTipo, item: NumeroLista) {
        vmListas.eliminar(tipo, item) { eliminado ->
            scope.launch {
                if (!eliminado) {
                    snackbar.showSnackbar("No se pudo eliminar el número")
                } else {
                    val resultado = snackbar.showSnackbar(
                        "${item.numero} eliminado",
                        actionLabel = "Deshacer",
                        withDismissAction = true
                    )
                    if (resultado == SnackbarResult.ActionPerformed) {
                        vmListas.guardar(
                            tipo, null, item.numero, item.etiqueta, item.activo, item.horario
                        ) {}
                    }
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                when (pestana) {
                    PestanaReglas.PREFIJOS -> {
                        prefijoEditando = null
                        mostrarPrefijo = true
                    }
                    PestanaReglas.BLOQUEADOS, PestanaReglas.BLANCA -> {
                        numeroTipo = if (pestana == PestanaReglas.BLOQUEADOS) {
                            ListaTipo.BLOQUEADOS
                        } else {
                            ListaTipo.BLANCA
                        }
                        numeroEditando = null
                        mostrarNumero = true
                    }
                }
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir regla")
            }
        }
    ) { relleno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Encabezado(
                    titulo = "Reglas",
                    subtitulo = "Prefijos, números bloqueados y lista blanca"
                )
                Spacer(Modifier.height(12.dp))
                PrimaryTabRow(selectedTabIndex = pestana.ordinal) {
                    Tab(
                        selected = pestana == PestanaReglas.PREFIJOS,
                        onClick = { pestanaNombre = PestanaReglas.PREFIJOS.name },
                        text = { Text("Prefijos") }
                    )
                    Tab(
                        selected = pestana == PestanaReglas.BLOQUEADOS,
                        onClick = { pestanaNombre = PestanaReglas.BLOQUEADOS.name },
                        text = { Text("Bloqueados") }
                    )
                    Tab(
                        selected = pestana == PestanaReglas.BLANCA,
                        onClick = { pestanaNombre = PestanaReglas.BLANCA.name },
                        text = { Text("Lista blanca") }
                    )
                }
            }

            when (pestana) {
                PestanaReglas.PREFIJOS -> ListaPrefijos(
                    cargando = vmFiltros.cargando,
                    filtros = vmFiltros.filtros,
                    onCambiarEstado = { filtro, activo ->
                        vmFiltros.cambiarEstado(filtro, activo)
                    },
                    onEditar = {
                        prefijoEditando = it
                        mostrarPrefijo = true
                    },
                    onEliminar = { eliminarPrefijo = it }
                )

                PestanaReglas.BLOQUEADOS, PestanaReglas.BLANCA -> {
                    val tipo = if (pestana == PestanaReglas.BLOQUEADOS) {
                        ListaTipo.BLOQUEADOS
                    } else {
                        ListaTipo.BLANCA
                    }
                    val todos = vmListas.numeros(tipo)
                    val consulta = ReglaBloqueo.normalizar(busqueda)
                    val visibles = if (busqueda.isBlank()) {
                        todos
                    } else {
                        todos.filter {
                            it.numero.contains(consulta) ||
                                it.etiqueta.contains(busqueda.trim(), ignoreCase = true)
                        }
                    }
                    ListaNumeros(
                        tipo = tipo,
                        cargando = vmListas.cargando,
                        numeros = visibles,
                        busqueda = busqueda,
                        onBusqueda = { busqueda = it },
                        onCambiarEstado = { item, activo ->
                            vmListas.cambiarEstado(tipo, item, activo)
                        },
                        onEditar = {
                            numeroTipo = tipo
                            numeroEditando = it
                            mostrarNumero = true
                        },
                        onEliminar = { eliminarNumero = it }
                    )
                }
            }
        }
    }

    if (mostrarPrefijo) {
        DialogoPrefijo(
            original = prefijoEditando,
            onGuardar = { prefijo, prioridad, accion, horario, cerrar, error ->
                vmFiltros.guardar(
                    prefijoEditando, prefijo, prioridad, accion, horario
                ) { mensaje ->
                    if (mensaje == null) {
                        mostrarPrefijo = false
                    } else {
                        error(mensaje)
                    }
                }
            },
            onCancelar = { mostrarPrefijo = false }
        )
    }

    if (mostrarNumero) {
        DialogoNumero(
            tipo = numeroTipo,
            original = numeroEditando,
            onGuardar = { numero, etiqueta, activo, horario, error ->
                vmListas.guardar(
                    numeroTipo, numeroEditando, numero, etiqueta, activo, horario
                ) { mensaje ->
                    if (mensaje == null) {
                        mostrarNumero = false
                    } else {
                        error(mensaje)
                    }
                }
            },
            onCancelar = { mostrarNumero = false }
        )
    }

    eliminarPrefijo?.let { filtro ->
        AlertDialog(
            onDismissRequest = { eliminarPrefijo = null },
            title = { Text("Eliminar prefijo") },
            text = { Text("¿Seguro que quieres eliminar el prefijo ${filtro.prefijo}?") },
            confirmButton = {
                TextButton(onClick = {
                    eliminarPrefijo = null
                    borrarPrefijo(filtro)
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { eliminarPrefijo = null }) { Text("Cancelar") }
            }
        )
    }

    eliminarNumero?.let { item ->
        AlertDialog(
            onDismissRequest = { eliminarNumero = null },
            title = { Text("Eliminar número") },
            text = { Text("¿Seguro que quieres eliminar ${item.numero}?") },
            confirmButton = {
                TextButton(onClick = {
                    eliminarNumero = null
                    borrarNumero(numeroTipo, item)
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { eliminarNumero = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun ListaPrefijos(
    cargando: Boolean,
    filtros: List<Filtro>,
    onCambiarEstado: (Filtro, Boolean) -> Unit,
    onEditar: (Filtro) -> Unit,
    onEliminar: (Filtro) -> Unit
) {
    if (cargando) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
    if (!cargando && filtros.isEmpty()) {
        EstadoVacio(
            icono = Icons.AutoMirrored.Filled.List,
            titulo = "Sin prefijos configurados",
            texto = "Añade un prefijo con el botón + para bloquear o permitir esas llamadas."
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(filtros, key = { it.id }) { filtro ->
            Card(
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = filtro.prefijo,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(8.dp))
                                ChipAccion(filtro.accion)
                            }
                            if (filtro.prioridad != 0) {
                                Text(
                                    text = "Prioridad ${filtro.prioridad}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextoHorario(filtro.horario)
                        }
                        Switch(
                            checked = filtro.activo,
                            onCheckedChange = { onCambiarEstado(filtro, it) }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = { onEditar(filtro) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar prefijo")
                        }
                        IconButton(onClick = { onEliminar(filtro) }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Eliminar prefijo",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListaNumeros(
    tipo: ListaTipo,
    cargando: Boolean,
    numeros: List<NumeroLista>,
    busqueda: String,
    onBusqueda: (String) -> Unit,
    onCambiarEstado: (NumeroLista, Boolean) -> Unit,
    onEditar: (NumeroLista) -> Unit,
    onEliminar: (NumeroLista) -> Unit
) {
    val esBlanca = tipo == ListaTipo.BLANCA
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = busqueda,
                onValueChange = onBusqueda,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Buscar número o etiqueta") },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null)
                },
                singleLine = true
            )
        }
        if (!esBlanca) {
            item {
                Text(
                    text = "Si un número tiene horario, solo se bloquea dentro de esa franja. " +
                        "La lista blanca siempre permite y gana a todo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (cargando) {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        }
        if (!cargando && numeros.isEmpty()) {
            item {
                EstadoVacio(
                    icono = if (esBlanca) Icons.Filled.CheckCircle else Icons.Filled.Lock,
                    titulo = if (esBlanca) "Lista blanca vacía" else "Sin números bloqueados",
                    texto = if (esBlanca) {
                        "Los números añadidos aquí siempre sonarán, aunque coincidan con una regla."
                    } else {
                        "Añade un número con el botón + para bloquearlo por completo."
                    }
                )
            }
        }
        items(numeros, key = { it.id }) { item ->
            Card(
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.numero,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (item.etiqueta.isNotBlank()) {
                                Text(
                                    text = item.etiqueta,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!esBlanca) {
                                TextoHorario(item.horario)
                            } else {
                                Text(
                                    text = "Siempre permitido",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        Switch(
                            checked = item.activo,
                            onCheckedChange = { onCambiarEstado(item, it) }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = { onEditar(item) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar número")
                        }
                        IconButton(onClick = { onEliminar(item) }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Eliminar número",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChipAccion(accion: Accion) {
    val permitir = accion == Accion.PERMITIR
    androidx.compose.material3.Surface(
        shape = RoundedCornerShape(50),
        color = if (permitir) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.errorContainer
    ) {
        Text(
            text = if (permitir) "Permitir" else "Bloquear",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (permitir) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun DialogoPrefijo(
    original: Filtro?,
    onGuardar: (
        prefijo: String,
        prioridad: Int,
        accion: Accion,
        horario: Horario?,
        cerrar: () -> Unit,
        error: (String) -> Unit
    ) -> Unit,
    onCancelar: () -> Unit
) {
    var prefijo by remember { mutableStateOf(original?.prefijo.orEmpty()) }
    var prioridad by remember { mutableStateOf(original?.prioridad?.toString() ?: "0") }
    var accion by remember { mutableStateOf(original?.accion ?: Accion.BLOQUEAR) }
    var horario by remember { mutableStateOf(original?.horario) }
    var mostrarHorario by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(if (original == null) "Nuevo prefijo" else "Editar prefijo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = prefijo,
                    onValueChange = {
                        prefijo = it
                        error = null
                    },
                    label = { Text("Prefijo") },
                    placeholder = { Text("Ej. 400") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                OutlinedTextField(
                    value = prioridad,
                    onValueChange = { valor ->
                        if (valor.isEmpty() || valor.all { it.isDigit() }) prioridad = valor
                    },
                    label = { Text("Prioridad") },
                    supportingText = { Text("Mayor número se evalúa antes") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Text("Acción", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = accion == Accion.BLOQUEAR,
                        onClick = { accion = Accion.BLOQUEAR },
                        label = { Text("Bloquear") }
                    )
                    FilterChip(
                        selected = accion == Accion.PERMITIR,
                        onClick = { accion = Accion.PERMITIR },
                        label = { Text("Permitir") }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Horario", style = MaterialTheme.typography.labelLarge)
                        TextoHorario(horario)
                    }
                    TextButton(onClick = { mostrarHorario = true }) {
                        Text(if (horario == null) "Añadir" else "Cambiar")
                    }
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (prefijo.isBlank()) {
                    error = "El prefijo no puede estar vacío"
                } else {
                    onGuardar(
                        prefijo,
                        prioridad.toIntOrNull() ?: 0,
                        accion,
                        horario,
                        { },
                        { error = it }
                    )
                }
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )

    if (mostrarHorario) {
        DialogoHorario(
            titulo = "Horario del prefijo",
            horarioInicial = horario,
            textoAyuda = "Fuera de esta franja la regla no se aplica.",
            onGuardar = {
                horario = it
                mostrarHorario = false
            },
            onCancelar = { mostrarHorario = false }
        )
    }
}

@Composable
private fun DialogoNumero(
    tipo: ListaTipo,
    original: NumeroLista?,
    onGuardar: (
        numero: String,
        etiqueta: String,
        activo: Boolean,
        horario: Horario?,
        error: (String) -> Unit
    ) -> Unit,
    onCancelar: () -> Unit
) {
    val esBlanca = tipo == ListaTipo.BLANCA
    var numero by remember { mutableStateOf(original?.numero.orEmpty()) }
    var etiqueta by remember { mutableStateOf(original?.etiqueta.orEmpty()) }
    var activo by remember { mutableStateOf(original?.activo ?: true) }
    var horario by remember { mutableStateOf(original?.horario) }
    var mostrarHorario by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = {
            Text(
                when {
                    esBlanca && original == null -> "Añadir a lista blanca"
                    esBlanca -> "Editar lista blanca"
                    original == null -> "Bloquear número"
                    else -> "Editar número bloqueado"
                }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = numero,
                    onValueChange = {
                        numero = it
                        error = null
                    },
                    label = { Text("Número") },
                    placeholder = { Text("Ej. +34 612 345 678") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                OutlinedTextField(
                    value = etiqueta,
                    onValueChange = { etiqueta = it },
                    label = { Text("Etiqueta (opcional)") },
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Activo")
                    Switch(checked = activo, onCheckedChange = { activo = it })
                }
                if (!esBlanca) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Horario", style = MaterialTheme.typography.labelLarge)
                            TextoHorario(horario)
                        }
                        TextButton(onClick = { mostrarHorario = true }) {
                            Text(if (horario == null) "Añadir" else "Cambiar")
                        }
                    }
                } else {
                    Text(
                        text = "La lista blanca siempre permite la llamada, sin horario.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (numero.isBlank()) {
                    error = "El número no puede estar vacío"
                } else {
                    onGuardar(numero, etiqueta, activo, if (esBlanca) null else horario) {
                        error = it
                    }
                }
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )

    if (mostrarHorario && !esBlanca) {
        DialogoHorario(
            titulo = "Horario del número",
            horarioInicial = horario,
            textoAyuda = "Fuera de esta franja el número no se bloquea.",
            onGuardar = {
                horario = it
                mostrarHorario = false
            },
            onCancelar = { mostrarHorario = false }
        )
    }
}
