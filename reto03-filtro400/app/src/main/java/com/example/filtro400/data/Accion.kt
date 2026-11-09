package com.example.filtro400.data

// Acción de una regla de prefijo.
enum class Accion {
    BLOQUEAR,
    PERMITIR;

    companion object {
        fun desdeTexto(valor: String?): Accion =
            entries.firstOrNull { it.name.equals(valor?.trim(), ignoreCase = true) }
                ?: BLOQUEAR
    }
}
