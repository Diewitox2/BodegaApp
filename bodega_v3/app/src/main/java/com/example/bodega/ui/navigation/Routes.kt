package com.example.bodega.ui.navigation

object Routes {
    // Navegacion principal (NavHost raiz)
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAIN = "main"
    const val ARG_PRODUCTO_ID = "productoId"
    const val PRODUCTO_FORM = "producto_form/{$ARG_PRODUCTO_ID}"
    const val MOVIMIENTO_FORM = "movimiento_form"

    /** id = 0 significa "producto nuevo". */
    fun productoForm(id: Int) = "producto_form/$id"

    // Barra inferior
    const val PRODUCTOS = "productos"
    const val MOVIMIENTOS = "movimientos"
    const val CATALOGOS = "catalogos"
}
