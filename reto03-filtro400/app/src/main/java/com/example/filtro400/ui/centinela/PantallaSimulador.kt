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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.filtro400.ui.vm.ResultadoSimulacion
import com.example.filtro400.ui.vm.SimuladorViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val formatoSimulacion = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaSimulador(vm: SimuladorViewModel = viewModel()) {
    var numero by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf(LocalDate.now()) }
    var hora by remember { mutableStateOf(LocalTime.now().withSecond(0).withNano(0)) }
    var mostrarFecha by remember { mutableStateOf(false) }
    var mostrarHora by remember { mutableStateOf(false) }
    var errorNumero by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Prueba cómo decidiría Centinela sin esperar a una llamada real. " +
                    "Puedes elegir la fecha y la hora para comprobar horarios y franjas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = numero,
                        onValueChange = {
                            numero = it
                            errorNumero = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Número") },
                        placeholder = { Text("Ej. +34 612 345 678") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = errorNumero != null
                    )
                    errorNumero?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }

                    Text("Momento de la simulación", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { mostrarFecha = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                        }
                        OutlinedButton(
                            onClick = { mostrarHora = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(hora.format(DateTimeFormatter.ofPattern("HH:mm")))
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (numero.isBlank()) {
                                    errorNumero = "Escribe un número para simular la llamada"
                                } else {
                                    vm.simular(numero, LocalDateTime.of(fecha, hora))
                                }
                            },
                            enabled = !vm.probando,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (vm.probando) "Probando..." else "Probar llamada")
                        }
                        OutlinedButton(
                            onClick = {
                                vm.limpiar()
                                errorNumero = null
                            }
                        ) {
                            Text("Limpiar")
                        }
                    }
                }
            }
        }

        vm.error?.let { error ->
            item {
                Text(error, color = MaterialTheme.colorScheme.error)
            }
        }

        vm.resultado?.let { resultado ->
            item { Resultado(resultado) }
        }

        item {
            Text(
                text = "La simulación se guarda como simulación y no cuenta en las estadísticas " +
                    "salvo que actives esa opción. No afecta a llamadas reales.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (mostrarFecha) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = fecha
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { mostrarFecha = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let {
                        fecha = Instant.ofEpochMilli(it)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                    }
                    mostrarFecha = false
                }) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarFecha = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = estado)
        }
    }

    if (mostrarHora) {
        DialogoHora(
            titulo = "Hora de la simulación",
            minutoInicial = hora.hour * 60 + hora.minute,
            onConfirmar = { minuto ->
                hora = LocalTime.of(minuto / 60, minuto % 60)
                mostrarHora = false
            },
            onCancelar = { mostrarHora = false }
        )
    }
}

@Composable
private fun Resultado(resultado: ResultadoSimulacion) {
    val bloqueada = resultado.decision.bloquear
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = if (bloqueada) "Se bloquearía" else "Pasaría",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (bloqueada) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(6.dp))
            Row {
                ChipDecision(
                    if (bloqueada) com.example.filtro400.data.DecisionLlamada.BLOQUEADA
                    else com.example.filtro400.data.DecisionLlamada.PERMITIDA
                )
                Spacer(Modifier.width(6.dp))
                ChipRegla(resultado.decision.reglaTipo)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Número: ${resultado.numeroEnmascarado}",
                style = MaterialTheme.typography.bodyMedium
            )
            resultado.decision.reglaValor?.let {
                Text(
                    text = "Regla: $it",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = "Momento: ${resultado.momento.format(formatoSimulacion)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
