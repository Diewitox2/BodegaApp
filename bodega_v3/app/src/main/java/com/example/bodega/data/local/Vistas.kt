package com.example.bodega.data.local

import androidx.room.Embedded

/** Producto + nombre de su categoria (resultado de un JOIN). */
data class ProductoVista(
    @Embedded val producto: ProductoEntity,
    val categoriaNombre: String
)

/** Movimiento + cantidad de lineas y total (calculados en SQL). */
data class MovimientoResumen(
    @Embedded val movimiento: MovimientoEntity,
    val items: Int,
    val total: Double
)

data class DetalleVista(
    val producto: String,
    val codigo: String,
    val cantidad: Int,
    val precioUnitario: Double
)

data class DetalleEnvio(
    val codigo: String,
    val cantidad: Int,
    val precioUnitario: Double
)

/** Linea que arma la pantalla de "Nuevo movimiento". */
data class LineaMovimiento(
    val productoId: Int,
    val cantidad: Int,
    val precio: Double
)

class StockInsuficienteException(
    val producto: String,
    val disponible: Int
) : Exception("Stock insuficiente de $producto (disponible: $disponible)")
