package com.example.filtro400.data

import com.example.filtro400.domain.ReglaTipo

data class ConteoRegla(
    val reglaTipo: ReglaTipo,
    val reglaValor: String?,
    val total: Int
)

data class ConteoDia(
    val fecha: Long,
    val bloqueadas: Int,
    val permitidas: Int
)

data class Estadisticas(
    val totalBloqueadas: Int,
    val totalPermitidas: Int,
    val bloqueosPorRegla: List<ConteoRegla>,
    val ultimos7Dias: List<ConteoDia>
)
