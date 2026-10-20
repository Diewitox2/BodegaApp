package com.example.bodega.data.repository

import com.example.bodega.constant.SyncEstado
import com.example.bodega.data.local.DetalleVista
import com.example.bodega.data.local.LineaMovimiento
import com.example.bodega.data.local.MovimientoDao
import com.example.bodega.data.local.MovimientoEntity
import com.example.bodega.data.local.MovimientoResumen
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface MovimientoRepository {

    fun observeResumenes(): Flow<List<MovimientoResumen>>

    fun observePendientes(): Flow<Int>

    suspend fun detalles(movimientoId: Int): List<DetalleVista>

    /**
     * Guarda el movimiento y actualiza el stock en una transaccion.
     * Lanza StockInsuficienteException si alguna salida deja el stock en negativo.
     */
    suspend fun registrar(
        tipo: String,
        proveedorId: Int?,
        observacion: String,
        usuarioId: Int,
        lineas: List<LineaMovimiento>
    ): Long
}

class MovimientoRepositoryImpl(
    private val dao: MovimientoDao
) : MovimientoRepository {

    override fun observeResumenes(): Flow<List<MovimientoResumen>> = dao.observeResumenes()

    override fun observePendientes(): Flow<Int> = dao.observarPendientes()

    override suspend fun detalles(movimientoId: Int): List<DetalleVista> =
        dao.detalles(movimientoId)

    override suspend fun registrar(
        tipo: String,
        proveedorId: Int?,
        observacion: String,
        usuarioId: Int,
        lineas: List<LineaMovimiento>
    ): Long {
        val movimiento = MovimientoEntity(
            uuid = UUID.randomUUID().toString(),
            tipo = tipo,
            fechaHora = System.currentTimeMillis(),
            usuarioId = usuarioId,
            proveedorId = proveedorId,
            observacion = observacion,
            syncEstado = SyncEstado.PENDIENTE
        )
        return dao.registrar(movimiento, lineas)
    }
}
