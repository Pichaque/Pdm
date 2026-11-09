package com.example.filtro400.ui.centinela

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.filtro400.data.Ajustes
import com.example.filtro400.ui.vm.AjustesViewModel
import kotlinx.coroutines.launch

@Composable
fun PantallaAjustes(vm: AjustesViewModel = viewModel()) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var pendiente by remember { mutableStateOf<Ajustes?>(null) }

    val lanzadorPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        val nuevos = pendiente
        pendiente = null
        if (concedido && nuevos != null) {
            vm.guardar(nuevos)
        } else {
            alcance.launch {
                snackbar.showSnackbar(
                    "Sin permiso de notificaciones no se avisará de los bloqueos"
                )
            }
        }
    }

    fun aplicar(nuevos: Ajustes) {
        val necesitaPermiso = nuevos.notificarBloqueos &&
            !vm.ajustes.notificarBloqueos &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                contexto,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED

        if (necesitaPermiso) {
            pendiente = nuevos
            lanzadorPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            vm.guardar(nuevos)
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
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Filtrado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        FilaAjuste(
                            titulo = "No molestar global",
                            descripcion = "Bloquea llamadas desconocidas dentro de las franjas activas.",
                            checked = vm.ajustes.modoNoMolestarActivo,
                            onCheckedChange = {
                                aplicar(vm.ajustes.copy(modoNoMolestarActivo = it))
                            }
                        )
                        FilaAjuste(
                            titulo = "Desviar al buzón",
                            descripcion = "Si se desactiva, la llamada se rechaza sin desviarla.",
                            checked = vm.ajustes.desviarBuzon,
                            onCheckedChange = {
                                aplicar(vm.ajustes.copy(desviarBuzon = it))
                            }
                        )
                        FilaAjuste(
                            titulo = "Registrar llamadas permitidas",
                            descripcion = "Guarda también las llamadas que no se bloquean.",
                            checked = vm.ajustes.registrarPermitidas,
                            onCheckedChange = {
                                aplicar(vm.ajustes.copy(registrarPermitidas = it))
                            }
                        )
                        FilaAjuste(
                            titulo = "Bloquear números ocultos",
                            descripcion = "Depende de que Android entregue el identificador oculto. " +
                                "Si no lo hace, algunas llamadas sin número podrían no bloquearse.",
                            checked = vm.ajustes.bloquearOcultos,
                            onCheckedChange = {
                                aplicar(vm.ajustes.copy(bloquearOcultos = it))
                            }
                        )
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Avisos y apariencia",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        FilaAjuste(
                            titulo = "Notificar bloqueos",
                            descripcion = "Aviso silencioso sin mostrar el número completo.",
                            checked = vm.ajustes.notificarBloqueos,
                            onCheckedChange = {
                                aplicar(vm.ajustes.copy(notificarBloqueos = it))
                            }
                        )
                        FilaAjuste(
                            titulo = "Color dinámico",
                            descripcion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                "Usa los colores de tu fondo de pantalla. Desactivado por defecto."
                            } else {
                                "Disponible a partir de Android 12."
                            },
                            checked = vm.ajustes.colorDinamico,
                            enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                            onCheckedChange = {
                                aplicar(vm.ajustes.copy(colorDinamico = it))
                            }
                        )
                    }
                }
            }

            item {
                TextPrivacidad()
            }

            item {
                Text(
                    text = "Centinela no lee tus contactos ni pide permisos de almacenamiento. " +
                        "Android solo le envía las llamadas que no están en tu agenda.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FilaAjuste(
    titulo: String,
    descripcion: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = titulo, fontWeight = FontWeight.Medium)
            Text(
                text = descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

