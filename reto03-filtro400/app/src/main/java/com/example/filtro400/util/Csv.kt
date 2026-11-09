package com.example.filtro400.util

import com.example.filtro400.ReglaBloqueo
import com.example.filtro400.data.Accion
import com.example.filtro400.data.Filtro
import com.example.filtro400.data.NumeroLista
import com.example.filtro400.domain.DiaSemana
import com.example.filtro400.domain.Horario

// Parser/escritor CSV minimo (RFC 4180): comas, comillas dobles, saltos de
// linea dentro de campos y CRLF. No depende de Android.
object Csv {

    fun escribir(filas: List<List<String>>): String =
        filas.joinToString("\r\n") { fila -> fila.joinToString(",") { escapar(it) } } + "\r\n"

    fun parsear(texto: String): List<List<String>> {
        val limpio = texto.removePrefix("\uFEFF")
        val filas = mutableListOf<List<String>>()
        var fila = mutableListOf<String>()
        val campo = StringBuilder()
        var entreComillas = false
        var i = 0

        fun cerrarFila() {
            fila.add(campo.toString())
            campo.setLength(0)
            if (fila.size > 1 || fila[0].isNotEmpty()) filas.add(fila)
            fila = mutableListOf()
        }

        while (i < limpio.length) {
            val c = limpio[i]
            if (entreComillas) {
                if (c == '"') {
                    if (i + 1 < limpio.length && limpio[i + 1] == '"') {
                        campo.append('"')
                        i++
                    } else {
                        entreComillas = false
                    }
                } else {
                    campo.append(c)
                }
            } else {
                when (c) {
                    '"' -> if (campo.isEmpty()) entreComillas = true else campo.append(c)
                    ',' -> {
                        fila.add(campo.toString())
                        campo.setLength(0)
                    }
                    '\r' -> {
                        if (i + 1 < limpio.length && limpio[i + 1] == '\n') i++
                        cerrarFila()
                    }
                    '\n' -> cerrarFila()
                    else -> campo.append(c)
                }
            }
            i++
        }

        if (campo.isNotEmpty() || fila.isNotEmpty()) cerrarFila()
        return filas
    }

    private fun escapar(valor: String): String =
        if (valor.any { it == ',' || it == '"' || it == '\r' || it == '\n' }) {
            "\"" + valor.replace("\"", "\"\"") + "\""
        } else {
            valor
        }
}

data class NumeroImportado(
    val numero: String,
    val etiqueta: String,
    val activo: Boolean
)

data class FiltroImportado(
    val prefijo: String,
    val prioridad: Int,
    val accion: Accion,
    val activo: Boolean,
    val horario: Horario? = null
)

data class ResultadoImportacion<T>(
    val filas: List<T>,
    val invalidas: Int
)

// Formato de los CSV de la app. La cabecera es opcional al importar.
object CsvReglas {

    const val CABECERA_NUMEROS = "numero,etiqueta,activo"
    const val CABECERA_FILTROS = "prefijo,prioridad,accion,activo,dias,inicio,fin"

    fun exportarNumeros(numeros: List<NumeroLista>): String = Csv.escribir(
        listOf(listOf("numero", "etiqueta", "activo")) +
            numeros.map {
                listOf(it.numero, it.etiqueta, if (it.activo) "1" else "0")
            }
    )

    fun exportarFiltros(filtros: List<Filtro>): String = Csv.escribir(
        listOf(listOf("prefijo", "prioridad", "accion", "activo", "dias", "inicio", "fin")) +
            filtros.map {
                val horario = it.horario
                listOf(
                    it.prefijo,
                    it.prioridad.toString(),
                    it.accion.name,
                    if (it.activo) "1" else "0",
                    if (horario == null) "" else DiaSemana.mascara(horario.dias).toString(),
                    if (horario == null) "" else horario.textoInicio(),
                    if (horario == null) "" else horario.textoFin()
                )
            }
    )

    fun importarNumeros(texto: String): ResultadoImportacion<NumeroImportado> {
        val filas = quitarCabecera(Csv.parsear(texto), CABECERA_NUMEROS)
        val validas = mutableListOf<NumeroImportado>()
        var invalidas = 0

        filas.forEach { fila ->
            val numero = fila.getOrNull(0)?.trim().orEmpty()
            val etiqueta = fila.getOrNull(1)?.trim().orEmpty()
            val activoTexto = fila.getOrNull(2)?.trim().orEmpty()
            val activo = leerBooleano(activoTexto)

            if (
                fila.size != 3 ||
                ReglaBloqueo.normalizar(numero).isEmpty() ||
                activo == null
            ) {
                invalidas++
            } else {
                validas += NumeroImportado(numero, etiqueta, activo)
            }
        }

        return ResultadoImportacion(validas, invalidas)
    }

    fun importarFiltros(texto: String): ResultadoImportacion<FiltroImportado> {
        val filas = quitarCabecera(Csv.parsear(texto), CABECERA_FILTROS)
        val validas = mutableListOf<FiltroImportado>()
        var invalidas = 0

        filas.forEach { fila ->
            val prefijo = fila.getOrNull(0)?.trim().orEmpty()
            val prioridadTexto = fila.getOrNull(1)?.trim().orEmpty()
            val accionTexto = fila.getOrNull(2)?.trim().orEmpty()
            val activoTexto = fila.getOrNull(3)?.trim().orEmpty()

            val prioridad = if (prioridadTexto.isEmpty()) 0 else prioridadTexto.toIntOrNull()
            val accion = when {
                accionTexto.isEmpty() -> Accion.BLOQUEAR
                accionTexto.equals("BLOQUEAR", ignoreCase = true) -> Accion.BLOQUEAR
                accionTexto.equals("PERMITIR", ignoreCase = true) -> Accion.PERMITIR
                else -> null
            }
            val activo = leerBooleano(activoTexto)
            val horarioCsv = leerHorarioCsv(fila)

            if (
                fila.size < 4 ||
                ReglaBloqueo.normalizar(prefijo).isEmpty() ||
                prioridad == null ||
                accion == null ||
                activo == null ||
                !horarioCsv.valido
            ) {
                invalidas++
            } else {
                validas += FiltroImportado(
                    prefijo,
                    prioridad,
                    accion,
                    activo,
                    horarioCsv.horario
                )
            }
        }

        return ResultadoImportacion(validas, invalidas)
    }

    private data class HorarioCsv(val horario: Horario?, val valido: Boolean)

    private fun leerHorarioCsv(fila: List<String>): HorarioCsv {
        if (fila.size < 7) return HorarioCsv(null, true)
        if (fila.size > 7) return HorarioCsv(null, false)

        val diasTexto = fila.getOrNull(4)?.trim().orEmpty()
        val inicioTexto = fila.getOrNull(5)?.trim().orEmpty()
        val finTexto = fila.getOrNull(6)?.trim().orEmpty()

        // Exportamos celdas vacias para reglas sin horario.
        if (diasTexto.isEmpty() && inicioTexto.isEmpty() && finTexto.isEmpty()) {
            return HorarioCsv(null, true)
        }

        val dias = if (diasTexto.isEmpty()) {
            DiaSemana.todos()
        } else {
            val mascara = diasTexto.toIntOrNull() ?: return HorarioCsv(null, false)
            DiaSemana.desdeMascara(mascara).ifEmpty { return HorarioCsv(null, false) }
        }
        val inicio = if (inicioTexto.isEmpty()) 0 else Horario.textoAMinuto(inicioTexto)
            ?: return HorarioCsv(null, false)
        val fin = if (finTexto.isEmpty()) 1440 else Horario.textoAMinuto(finTexto)
            ?: return HorarioCsv(null, false)
        return HorarioCsv(Horario(dias, inicio, fin), true)
    }

    private fun quitarCabecera(
        filas: List<List<String>>,
        cabecera: String
    ): List<List<String>> {
        val esperada = cabecera.split(",")
        val primera = filas.firstOrNull()?.map { it.trim().lowercase() }
        val claves = minOf(esperada.size, 4)
        val coincide = primera != null &&
            primera.size >= claves &&
            primera.take(claves) == esperada.take(claves)
        return if (coincide) filas.drop(1) else filas
    }

    private fun leerBooleano(valor: String): Boolean? = when (valor.trim().lowercase()) {
        "", "1", "true", "si", "sí", "yes" -> true
        "0", "false", "no" -> false
        else -> null
    }
}
