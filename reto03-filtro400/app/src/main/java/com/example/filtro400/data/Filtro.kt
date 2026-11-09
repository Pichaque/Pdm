package com.example.filtro400.data

import com.example.filtro400.domain.Horario

data class Filtro(
    val id: Long = 0,
    val prefijo: String,
    val activo: Boolean = true,
    val prioridad: Int = 0,
    val accion: Accion = Accion.BLOQUEAR,
    val horario: Horario? = null
)
