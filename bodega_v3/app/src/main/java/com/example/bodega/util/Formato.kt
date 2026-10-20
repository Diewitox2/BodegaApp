package com.example.bodega.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formato {

    fun moneda(valor: Double): String = String.format(Locale.US, "S/ %.2f", valor)

    fun numero(valor: Double): String = String.format(Locale.US, "%.2f", valor)

    fun fecha(millis: Long): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date(millis))

    fun fechaIso(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date(millis))

    /** Acepta "12.50" o "12,50". Devuelve null si no es un numero. */
    fun decimal(texto: String): Double? =
        texto.trim().replace(',', '.').toDoubleOrNull()
}
