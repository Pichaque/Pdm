package com.example.filtro400.data

import com.example.filtro400.domain.ReglaTipo

data class RegistroLlamada(
    val id: Long,
    val fecha: Long,
    val numeroEnmascarado: String,
    val decision: DecisionLlamada,
    val reglaTipo: ReglaTipo,
    val reglaValor: String?,
    val simulada: Boolean,
    // Solo disponible durante 48 h; la UI muestra siempre numeroEnmascarado.
    val numeroCompleto: String? = null
)
