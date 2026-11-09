package com.example.filtro400.data

enum class DecisionLlamada {
    BLOQUEADA,
    PERMITIDA;

    companion object {
        fun desdeTexto(valor: String?): DecisionLlamada =
            entries.firstOrNull { it.name.equals(valor?.trim(), ignoreCase = true) }
                ?: PERMITIDA
    }
}
