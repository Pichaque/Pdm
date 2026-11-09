package com.example.filtro400.data

import com.example.filtro400.domain.Horario

// Fila de "numeros_bloqueados" o de "lista_blanca".
data class NumeroLista(
    val id: Long,
    val numero: String,
    val etiqueta: String,
    val activo: Boolean,
    val horario: Horario? = null
)
