package com.example.filtro400.util

import android.content.Context
import android.net.Uri

// Lectura/escritura de texto mediante Storage Access Framework (sin permisos).
fun leerTextoDeUri(context: Context, uri: Uri): String? = try {
    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
} catch (e: Exception) {
    null
}

fun escribirTextoEnUri(context: Context, uri: Uri, texto: String): Boolean = try {
    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(texto) }
    true
} catch (e: Exception) {
    false
}
