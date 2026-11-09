package com.example.filtro400.ui.centinela

import android.app.role.RoleManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.filtro400.R
import com.example.filtro400.ui.theme.AzulNoche
import com.example.filtro400.ui.theme.AzulNocheClaro
import com.example.filtro400.ui.theme.AzulNocheProfundo
import com.example.filtro400.ui.theme.Coral
import com.example.filtro400.ui.theme.VerdeMenta
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.filtro400.ui.vm.InicioViewModel
import com.example.filtro400.ui.vm.ResumenInicio

@Composable
fun PantallaInicio(
    onIrAReglas: () -> Unit,
    onIrAHorarios: () -> Unit,
    onIrARegistro: () -> Unit,
    vm: InicioViewModel = viewModel()
) {
    val contexto = LocalContext.current
    val gestorRoles = remember {
        contexto.getSystemService(RoleManager::class.java)
    }
    var rolConcedido by remember {
        mutableStateOf(
            gestorRoles?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
        )
    }
    val lanzadorRol = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        rolConcedido = gestorRoles?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
    }

    LaunchedEffect(Unit) {
        vm.recargar()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp, end = 20.dp, top = 16.dp, bottom = 112.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { TarjetaMarca(rolConcedido) }

        if (vm.cargando) {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        }

        vm.error?.let { error ->
            item {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        item {
            TarjetaRol(
                concedido = rolConcedido,
                onActivar = {
                    val intent = gestorRoles?.createRequestRoleIntent(
                        RoleManager.ROLE_CALL_SCREENING
                    )
                    if (intent != null) lanzadorRol.launch(intent)
                }
            )
        }

        item {
            TarjetaEstadoNoMolestar(
                resumen = vm.resumen,
                onAbrirHorarios = onIrAHorarios
            )
        }

        item {
            ResumenDelDia(vm.resumen)
        }

        item {
            Text(
                text = "TU CENTRO DE CONTROL",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AccesoControl(
                    titulo = "Reglas",
                    descripcion = "Prefijos, números bloqueados y lista blanca",
                    icono = Icons.AutoMirrored.Filled.List,
                    onClick = onIrAReglas
                )
                AccesoControl(
                    titulo = "Horarios",
                    descripcion = "Franjas y modo No molestar",
                    icono = Icons.Filled.DateRange,
                    onClick = onIrAHorarios
                )
                AccesoControl(
                    titulo = "Registro",
                    descripcion = "Llamadas y decisiones recientes",
                    icono = Icons.Filled.Call,
                    onClick = onIrARegistro
                )
            }
        }

        item {
            TarjetaAyuda()
        }
    }
}

@Composable
private fun AccesoControl(
    titulo: String,
    descripcion: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 84.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Abrir",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun TarjetaMarca(rolConcedido: Boolean) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = AzulNocheProfundo),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(AzulNocheProfundo, AzulNoche, AzulNocheClaro)
                    )
                )
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(R.mipmap.filtro400_logo),
                contentDescription = "Escudo Centinela",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(74.dp)
                    .clip(RoundedCornerShape(20.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "CENTINELA",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = VerdeMenta
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Tu línea, bajo control.",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = CircleShape,
                    color = if (rolConcedido) VerdeMenta.copy(alpha = 0.18f)
                    else Coral.copy(alpha = 0.18f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(7.dp),
                            shape = CircleShape,
                            color = if (rolConcedido) VerdeMenta else Coral
                        ) {}
                        Text(
                            text = if (rolConcedido) "PROTECCIÓN ACTIVA"
                            else "CONFIGURACIÓN PENDIENTE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (rolConcedido) VerdeMenta else Color(0xFFFFB7B9)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaRol(concedido: Boolean, onActivar: () -> Unit) {
    val fondo = if (concedido) MaterialTheme.colorScheme.secondaryContainer
    else MaterialTheme.colorScheme.errorContainer
    val contenido = if (concedido) MaterialTheme.colorScheme.onSecondaryContainer
    else MaterialTheme.colorScheme.onErrorContainer

    Card(
        colors = CardDefaults.cardColors(containerColor = fondo),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (concedido) Icons.Filled.CheckCircle
                    else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = contenido
                )
                Spacer(Modifier.padding(horizontal = 6.dp))
                Text(
                    text = if (concedido) "Filtro de llamadas activo"
                    else "Falta activar el filtro de llamadas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contenido
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (concedido) {
                    "Android ya puede consultar a Centinela antes de mostrar una llamada."
                } else {
                    "Sin el rol de filtrado, Android no enviará las llamadas a Centinela."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = contenido
            )
            if (!concedido) {
                Spacer(Modifier.height(10.dp))
                Button(onClick = onActivar) {
                    Text("Activar filtro de llamadas")
                }
            }
        }
    }
}

@Composable
private fun TarjetaEstadoNoMolestar(resumen: ResumenInicio, onAbrirHorarios: () -> Unit) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "No molestar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = when {
                            !resumen.noMolestarActivo -> "Desactivado"
                            resumen.noMolestarAhora -> "Activo ahora mismo"
                            else -> "Activado, fuera de franja"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (resumen.noMolestarAhora) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                PuntoEstado(activo = resumen.noMolestarAhora)
            }
            resumen.proximaFranja?.let { horario ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Próxima franja: ${horario.resumen()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(10.dp))
            FilledTonalButton(onClick = onAbrirHorarios) {
                Text("Gestionar horarios")
            }
        }
    }
}

@Composable
private fun ResumenDelDia(resumen: ResumenInicio) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Contador(
            titulo = "Bloqueadas hoy",
            valor = resumen.bloqueadasHoy,
            colorFondo = MaterialTheme.colorScheme.errorContainer,
            colorTexto = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f)
        )
        Contador(
            titulo = "Permitidas hoy",
            valor = resumen.permitidasHoy,
            colorFondo = MaterialTheme.colorScheme.secondaryContainer,
            colorTexto = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f)
        )
        Contador(
            titulo = "Reglas activas",
            valor = resumen.reglasActivas,
            colorFondo = MaterialTheme.colorScheme.primaryContainer,
            colorTexto = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun Contador(
    titulo: String,
    valor: Int,
    colorFondo: androidx.compose.ui.graphics.Color,
    colorTexto: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = colorFondo
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = valor.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colorTexto
            )
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelSmall,
                color = colorTexto
            )
        }
    }
}

@Composable
private fun TarjetaAyuda() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Cómo funciona",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Android solo envía a Centinela las llamadas de números que NO están " +
                    "en tu agenda. Los contactos guardados siempre suenan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "La lista blanca tiene prioridad sobre cualquier regla y sobre No molestar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
