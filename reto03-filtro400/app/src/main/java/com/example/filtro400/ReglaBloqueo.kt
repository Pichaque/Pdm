package com.example.filtro400

// "object" = una única instancia, sin necesidad de crearla con ReglaBloqueo().
// Aquí vive la decisión de bloquear o no: sin Android, sin pantalla.
object ReglaBloqueo {

    // Prefijo de la Fase 1, se conserva como valor por defecto.
    private const val PREFIJO_BLOQUEADO = "400"

    val PREFIJOS_POR_DEFECTO = listOf(PREFIJO_BLOQUEADO)

    // Un mismo número puede llegar de varias formas:
    // "400123456", "+34400123456", "0034 400 123 456", "+34 400 123 456"...
    // Lo dejamos siempre en el mismo formato antes de comparar.
    fun normalizar(numero: String): String {
        // Nos quedamos solo con dígitos y el signo "+" (fuera espacios y guiones).
        val limpio = numero.filter { it.isDigit() || it == '+' }
        return when {
            limpio.startsWith("+34") -> limpio.removePrefix("+34")
            limpio.startsWith("0034") -> limpio.removePrefix("0034")
            else -> limpio
        }
    }

    // Comportamiento de la Fase 1: delegado en la lista por defecto.
    fun debeBloquearse(numero: String): Boolean =
        debeBloquearse(numero, PREFIJOS_POR_DEFECTO)

    // Regla de la Fase 2: bloquea si el número empieza por algún prefijo activo.
    fun debeBloquearse(numero: String, prefijos: List<String>): Boolean {
        val normalizado = normalizar(numero)
        if (normalizado.isEmpty()) return false

        return prefijos
            .map { normalizar(it) }
            .filter { it.isNotEmpty() }
            .any { normalizado.startsWith(it) }
    }
}
