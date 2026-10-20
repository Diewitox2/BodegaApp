package com.example.bodega.util

import android.util.Patterns

object Validators {
    const val MIN_PASSWORD_LENGTH = 6

    fun isValidEmail(email: String): Boolean =
        Patterns.EMAIL_ADDRESS.matcher(email).matches()

    /** RUC peruano: 11 digitos. */
    fun isValidRuc(ruc: String): Boolean =
        ruc.length == 11 && ruc.all { it.isDigit() }
}
