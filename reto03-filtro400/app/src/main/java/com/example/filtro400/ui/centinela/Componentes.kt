package com.example.filtro400.ui.centinela

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.filtro400.data.DecisionLlamada
import com.example.filtro400.domain.DiaSemana
import com.example.filtro400.domain.Horario
import com.example.filtro400.domain.ReglaTipo

@Composable
fun Encabezado(titulo: String, subtitulo: String? = null) {
    Column {
        Text(
            text = "CENTINELA  /  PROTECCIÓN DE LLAMADAS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = titulo,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (subtitulo != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EstadoVacio(
    icono: ImageVector,
    titulo: String,
    texto: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(18.dp)
                        .size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = texto,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ChipDecision(decision: DecisionLlamada) {
    val bloqueada = decision == DecisionLlamada.BLOQUEADA
    val fondo = if (bloqueada) MaterialTheme.colorScheme.errorContainer
    else MaterialTheme.colorScheme.secondaryContainer
    val texto = if (bloqueada) MaterialTheme.colorScheme.onErrorContainer
    else MaterialTheme.colorScheme.onSecondaryContainer
    Surface(shape = RoundedCornerShape(50), color = fondo) {
        Text(
            text = if (bloqueada) "Bloqueada" else "Permitida",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = texto
        )
    }
}

@Composable
fun ChipRegla(tipo: ReglaTipo) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = textoRegla(tipo),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun textoRegla(tipo: ReglaTipo): String = when (tipo) {
    ReglaTipo.LISTA_BLANCA -> "Lista blanca"
    ReglaTipo.NUMERO -> "Número"
    ReglaTipo.PREFIJO -> "Prefijo"
    ReglaTipo.HORARIO -> "Horario"
    ReglaTipo.NO_MOLESTAR -> "No molestar"
    ReglaTipo.OCULTO -> "Oculto"
    ReglaTipo.NINGUNA -> "Sin regla"
}

@Composable
fun TextoHorario(horario: Horario?) {
    Text(
        text = horario?.resumen() ?: "Siempre activo",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoHorario(
    titulo: String,
    horarioInicial: Horario?,
    textoAyuda: String,
    permitirSinHorario: Boolean = true,
    onGuardar: (Horario?) -> Unit,
    onCancelar: () -> Unit
) {
    var usarHorario by remember { mutableStateOf(horarioInicial != null || !permitirSinHorario) }
    var dias by remember { mutableStateOf(horarioInicial?.dias ?: DiaSemana.todos()) }
    var inicio by remember { mutableStateOf(horarioInicial?.inicioMinuto ?: (22 * 60)) }
    var fin by remember { mutableStateOf(horarioInicial?.finMinuto ?: (8 * 60)) }
    var editandoInicio by remember { mutableStateOf<Boolean?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = textoAyuda,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (permitirSinHorario) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Limitar por horario", fontWeight = FontWeight.SemiBold)
                            Text(
                                text = if (usarHorario) {
                                    "Solo se aplica en la franja indicada"
                                } else {
                                    "Se aplica siempre"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = usarHorario,
                            onCheckedChange = {
                                usarHorario = it
                                error = null
                            }
                        )
                    }
                }

                if (usarHorario) {
                    Text("Días", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DiaSemana.entries.forEach { dia ->
                            FilterChip(
                                selected = dia in dias,
                                onClick = {
                                    dias = if (dia in dias) dias - dia else dias + dia
                                    error = null
                                },
                                label = { Text(dia.etiqueta) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { editandoInicio = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Desde ${Horario.minutoATexto(inicio)}")
                        }
                        OutlinedButton(
                            onClick = { editandoInicio = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "Hasta " + if (fin >= 1440) "24:00" else Horario.minutoATexto(fin)
                            )
                        }
                    }

                    val resumen = Horario(dias, inicio, fin)
                    Text(
                        text = if (inicio == fin || (inicio == 0 && fin >= 1440)) {
                            "Todo el día en los días elegidos"
                        } else if (inicio > fin) {
                            "Franja nocturna: termina al día siguiente"
                        } else {
                            "Inicio inclusivo, fin exclusivo"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = resumen.resumen(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (usarHorario && dias.isEmpty()) {
                    error = "Selecciona al menos un día"
                } else {
                    onGuardar(if (usarHorario) Horario(dias, inicio, fin) else null)
                }
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )

    editandoInicio?.let { esInicio ->
        DialogoHora(
            titulo = if (esInicio) "Hora de inicio" else "Hora de fin",
            minutoInicial = if (esInicio) inicio else fin,
            permitir24 = !esInicio,
            onConfirmar = { minuto ->
                if (esInicio) inicio = minuto else fin = minuto
                editandoInicio = null
            },
            onCancelar = { editandoInicio = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoHora(
    titulo: String,
    minutoInicial: Int,
    permitir24: Boolean = false,
    onConfirmar: (Int) -> Unit,
    onCancelar: () -> Unit
) {
    val estado = rememberTimePickerState(
        initialHour = if (minutoInicial >= 1440) 0 else minutoInicial / 60,
        initialMinute = if (minutoInicial >= 1440) 0 else minutoInicial % 60,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = estado)
                if (permitir24) {
                    TextButton(onClick = {
                        onConfirmar(1440)
                        onCancelar()
                    }) {
                        Text("Usar 24:00 como fin del día")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirmar(estado.hour * 60 + estado.minute) }) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}

@Composable
fun PuntoEstado(activo: Boolean, modifier: Modifier = Modifier) {
    val color = if (activo) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
    Surface(modifier = modifier.size(10.dp), shape = RoundedCornerShape(50), color = color) {}
}

@Composable
fun TextPrivacidad(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = "Centinela guarda los números enmascarados. El número completo solo se " +
                "conserva 48 horas para poder bloquearlo o permitirlo desde el registro.",
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
