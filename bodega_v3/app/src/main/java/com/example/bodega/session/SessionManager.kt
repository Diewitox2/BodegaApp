package com.example.bodega.session

import android.content.Context

/**
 * Guarda en SharedPreferences solo quien inicio sesion (id, nombre y rol).
 * Nunca se guarda la contrasena.
 */
class SessionManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("bodega_sesion", Context.MODE_PRIVATE)

    val usuarioId: Int
        get() = prefs.getInt(KEY_ID, 0)

    val nombre: String
        get() = prefs.getString(KEY_NOMBRE, "") ?: ""

    val rol: String
        get() = prefs.getString(KEY_ROL, "") ?: ""

    val haySesion: Boolean
        get() = usuarioId != 0

    /** Token entregado por el servidor al iniciar sesion con internet. */
    val token: String
        get() = prefs.getString(KEY_TOKEN, "") ?: ""

    /** Si token es null se conserva el anterior (inicio de sesion sin internet). */
    fun guardar(id: Int, nombre: String, rol: String, token: String?) {
        val editor = prefs.edit()
            .putInt(KEY_ID, id)
            .putString(KEY_NOMBRE, nombre)
            .putString(KEY_ROL, rol)
        if (token != null) editor.putString(KEY_TOKEN, token)
        editor.apply()
    }

    fun cerrar() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_ID = "usuario_id"
        const val KEY_NOMBRE = "usuario_nombre"
        const val KEY_ROL = "usuario_rol"
        const val KEY_TOKEN = "token"
    }
}
