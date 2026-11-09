package com.example.filtro400.data

import com.example.filtro400.domain.Horario

// Franja horaria del modo global "No molestar".
data class FranjaNoMolestar(
    val id: Long = 0,
    val horario: Horario,
    val activo: Boolean = true
)
