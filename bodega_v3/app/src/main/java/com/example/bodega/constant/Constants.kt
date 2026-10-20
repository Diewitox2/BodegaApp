package com.example.bodega.constant

object Constants {
    const val DATABASE_NAME = "bodega.db"

    /**
     * Direccion de TU servidor (carpeta "api" y con "/" al final). Es lo unico que hay que cambiar:
     *  - Hosting:           "https://tu-dominio.com/bodega/api/"
     *  - XAMPP + emulador:  "http://10.0.2.2/bodega/api/"
     */
    const val API_BASE_URL = "https://bodegadiana.alwaysdata.net/bodega/api/"

    // Open Food Facts: consulta publica de productos por codigo de barras (sin API key).
    const val PRODUCT_INFO_BASE_URL = "https://world.openfoodfacts.org/"
    const val USER_AGENT = "BodegaApp/3.0 (proyecto academico Cibertec)"
}

object Roles {
    const val ADMIN = "ADMIN"
    const val VENDEDOR = "VENDEDOR"
}

object TipoMovimiento {
    const val ENTRADA = "ENTRADA"
    const val SALIDA = "SALIDA"
    const val AJUSTE_POS = "AJUSTE_POS"
    const val AJUSTE_NEG = "AJUSTE_NEG"

    val todos = listOf(ENTRADA, SALIDA, AJUSTE_POS, AJUSTE_NEG)

    /** Las entradas y los ajustes positivos suman stock; los demas restan. */
    fun suma(tipo: String): Boolean = tipo == ENTRADA || tipo == AJUSTE_POS

    fun etiqueta(tipo: String): String = when (tipo) {
        ENTRADA -> "Entrada"
        SALIDA -> "Salida"
        AJUSTE_POS -> "Ajuste (+)"
        AJUSTE_NEG -> "Ajuste (-)"
        else -> tipo
    }
}

object SyncEstado {
    const val PENDIENTE = "PENDIENTE"
    const val ENVIADO = "ENVIADO"
    const val ERROR = "ERROR"

    fun etiqueta(estado: String): String = when (estado) {
        PENDIENTE -> "Pendiente de envío"
        ENVIADO -> "Enviado"
        ERROR -> "Error al enviar"
        else -> estado
    }
}
