package com.example.filtro400.util

// Deja visibles como mucho los tres primeros dígitos nacionales.
// Nunca devuelve el número completo; si no hay número, devuelve "Oculto".
fun enmascararNumero(numero: String): String {
    val limpio = numero.filter { it.isDigit() || it == '+' }
    if (limpio.isEmpty()) return "Oculto"

    val esEspanol = limpio.startsWith("+34") || limpio.startsWith("0034")
    val nacional = when {
        limpio.startsWith("+34") -> limpio.removePrefix("+34")
        limpio.startsWith("0034") -> limpio.removePrefix("0034")
        else -> limpio
    }.filter { it.isDigit() }

    if (nacional.isEmpty()) return "Oculto"

    val visibles = minOf(3, maxOf(0, nacional.length - 1))
    val prefijoPais = if (esEspanol) "+34 " else ""
    return "$prefijoPais${nacional.take(visibles)} ***"
}
