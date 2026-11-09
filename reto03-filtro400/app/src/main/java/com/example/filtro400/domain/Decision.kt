package com.example.filtro400.domain

// Tipo de regla que ha decidido (o no) sobre una llamada.
enum class ReglaTipo {
    LISTA_BLANCA,
    NUMERO,
    PREFIJO,
    HORARIO,
    NO_MOLESTAR,
    NINGUNA,
    OCULTO;

    companion object {
        fun desdeTexto(valor: String?): ReglaTipo =
            entries.firstOrNull { it.name.equals(valor?.trim(), ignoreCase = true) }
                ?: NINGUNA
    }
}

// Resultado del motor. El valor de regla es el prefijo/numero que coincidio.
data class Decision(
    val bloquear: Boolean,
    val reglaTipo: ReglaTipo,
    val reglaValor: String?
)
