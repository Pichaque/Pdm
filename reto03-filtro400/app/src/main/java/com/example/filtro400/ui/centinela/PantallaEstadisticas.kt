package com.example.filtro400.ui.centinela

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.filtro400.data.ConteoDia
import com.example.filtro400.data.Estadisticas
import com.example.filtro400.ui.vm.HistorialViewModel
import com.example.filtro400.util.formatearDia

@Composable
fun PantallaEstadisticas(vm: HistorialViewModel = viewModel()) {
    LaunchedEffect(Unit) { vm.cargar() }

    val estadisticas = vm.estadisticas
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Resumen",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Total(
                    titulo = "Bloqueadas",
                    valor = estadisticas.totalBloqueadas,
                    fondo = MaterialTheme.colorScheme.errorContainer,
                    texto = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
                Total(
                    titulo = "Permitidas",
                    valor = estadisticas.totalPermitidas,
                    fondo = MaterialTheme.colorScheme.secondaryContainer,
                    texto = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Incluir simulaciones",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Las pruebas del simulador no cuentan por defecto.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = vm.incluirSimuladas,
                        onCheckedChange = vm::cambiarIncluirSimuladas
                    )
                }
            }
        }

        if (vm.cargando) {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        }

        if (!vm.cargando &&
            estadisticas.totalBloqueadas == 0 &&
            estadisticas.totalPermitidas == 0
        ) {
            item {
                EstadoVacio(
                    icono = Icons.Filled.DateRange,
                    titulo = "Sin datos todavía",
                    texto = "Cuando Centinela evalúe llamadas verás aquí el resumen y las " +
                        "reglas que más veces han actuado."
                )
            }
        } else {
            item {
                Text(
                    text = "Últimos 7 días",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            item { GraficaSemana(estadisticas) }

            if (estadisticas.bloqueosPorRegla.isNotEmpty()) {
                item {
                    Text(
                        text = "Reglas que más bloquean",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                estadisticas.bloqueosPorRegla.forEach { conteo ->
                    item {
                        Card(shape = RoundedCornerShape(14.dp)) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ChipRegla(conteo.reglaTipo)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = conteo.reglaValor ?: "Sin valor",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = conteo.total.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
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
                    text = "Los números se guardan enmascarados. Las estadísticas se calculan " +
                        "sobre las llamadas evaluadas, no sobre tu agenda.",
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun Total(
    titulo: String,
    valor: Int,
    fondo: androidx.compose.ui.graphics.Color,
    texto: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), color = fondo) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = valor.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = texto
            )
            Text(text = titulo, style = MaterialTheme.typography.labelMedium, color = texto)
        }
    }
}

@Composable
private fun GraficaSemana(estadisticas: Estadisticas) {
    val maximo = estadisticas.ultimos7Dias.maxOfOrNull { it.bloqueadas + it.permitidas } ?: 0
    Card(shape = RoundedCornerShape(18.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            estadisticas.ultimos7Dias.forEach { dia ->
                FilaDia(dia, maximo)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Leyenda("Bloqueadas", MaterialTheme.colorScheme.error)
                Leyenda("Permitidas", MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun FilaDia(dia: ConteoDia, maximo: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = formatearDia(dia.fecha),
            modifier = Modifier.width(48.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Barra(
                valor = dia.bloqueadas,
                maximo = maximo,
                color = MaterialTheme.colorScheme.error
            )
            Barra(
                valor = dia.permitidas,
                maximo = maximo,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${dia.bloqueadas}/${dia.permitidas}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Barra(
    valor: Int,
    maximo: Int,
    color: androidx.compose.ui.graphics.Color
) {
    if (maximo <= 0 || valor <= 0) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        return
    }
    val fraccion = (valor.toFloat() / maximo.toFloat()).coerceIn(0.04f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth(fraccion)
            .height(7.dp)
            .clip(RoundedCornerShape(50))
            .background(color)
    )
}

@Composable
private fun Leyenda(texto: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(10.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = texto,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
