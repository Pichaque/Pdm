package com.example.filtro400.ui.centinela

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class DestinoMas {
    ESTADISTICAS,
    SIMULADOR,
    IMPORTAR,
    AJUSTES,
    ACERCA
}

@Composable
fun PantallaMas(onAbrir: (DestinoMas) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Encabezado(
                titulo = "Más",
                subtitulo = "Estadísticas, simulador, datos y ajustes"
            )
        }
        item {
            OpcionMas(
                icono = Icons.Filled.DateRange,
                titulo = "Estadísticas",
                descripcion = "Bloqueos por regla y resumen de los últimos 7 días",
                onClick = { onAbrir(DestinoMas.ESTADISTICAS) }
            )
        }
        item {
            OpcionMas(
                icono = Icons.Filled.PlayArrow,
                titulo = "Simulador",
                descripcion = "Prueba un número con una fecha y hora concretas",
                onClick = { onAbrir(DestinoMas.SIMULADOR) }
            )
        }
        item {
            OpcionMas(
                icono = Icons.AutoMirrored.Filled.Send,
                titulo = "Importar / Exportar",
                descripcion = "CSV de prefijos, números bloqueados y lista blanca",
                onClick = { onAbrir(DestinoMas.IMPORTAR) }
            )
        }
        item {
            OpcionMas(
                icono = Icons.Filled.Settings,
                titulo = "Ajustes",
                descripcion = "No molestar, notificaciones, privacidad y tema",
                onClick = { onAbrir(DestinoMas.AJUSTES) }
            )
        }
        item {
            OpcionMas(
                icono = Icons.Filled.Info,
                titulo = "Acerca de Centinela",
                descripcion = "Cómo funciona y qué datos guarda",
                onClick = { onAbrir(DestinoMas.ACERCA) }
            )
        }
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Los contactos guardados nunca pasan por Centinela: Android los " +
                            "deja sonar directamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun OpcionMas(
    icono: ImageVector,
    titulo: String,
    descripcion: String,
    onClick: () -> Unit
) {
    Card(onClick = onClick, shape = RoundedCornerShape(18.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
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
            }
        }
    }
}


