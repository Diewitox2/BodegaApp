package com.example.bodega.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.bodega.constant.TipoMovimiento
import com.example.bodega.data.remote.CatalogoDto
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {

    @Query("SELECT * FROM usuarios WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): UsuarioEntity?

    @Query("SELECT COUNT(*) FROM usuarios")
    suspend fun contar(): Int

    @Insert
    suspend fun insertar(usuario: UsuarioEntity): Long

    @Update
    suspend fun actualizar(usuario: UsuarioEntity)
}

@Dao
interface CategoriaDao {

    @Query("SELECT * FROM categorias ORDER BY nombre")
    fun observeTodas(): Flow<List<CategoriaEntity>>

    @Query("SELECT * FROM categorias WHERE activo = 1 ORDER BY nombre")
    fun observeActivas(): Flow<List<CategoriaEntity>>

    @Query("SELECT COUNT(*) FROM categorias")
    suspend fun contar(): Int

    @Query("SELECT * FROM categorias ORDER BY nombre")
    suspend fun todas(): List<CategoriaEntity>

    @Query("SELECT COUNT(*) FROM categorias WHERE LOWER(nombre) = LOWER(:nombre) AND id != :excluirId")
    suspend fun contarPorNombre(nombre: String, excluirId: Int): Int

    @Query("SELECT COUNT(*) FROM productos WHERE categoriaId = :id")
    suspend fun contarProductos(id: Int): Int

    @Insert
    suspend fun insertar(categoria: CategoriaEntity): Long

    @Update
    suspend fun actualizar(categoria: CategoriaEntity)

    @Delete
    suspend fun eliminar(categoria: CategoriaEntity)
}

@Dao
interface ProveedorDao {

    @Query("SELECT * FROM proveedores ORDER BY razonSocial")
    fun observeTodos(): Flow<List<ProveedorEntity>>

    @Query("SELECT * FROM proveedores WHERE activo = 1 ORDER BY razonSocial")
    fun observeActivos(): Flow<List<ProveedorEntity>>

    @Query("SELECT * FROM proveedores ORDER BY razonSocial")
    suspend fun todos(): List<ProveedorEntity>

    @Query("SELECT * FROM proveedores WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): ProveedorEntity?

    @Query("SELECT COUNT(*) FROM proveedores WHERE ruc = :ruc AND id != :excluirId")
    suspend fun contarPorRuc(ruc: String, excluirId: Int): Int

    @Query("SELECT COUNT(*) FROM productos WHERE proveedorId = :id")
    suspend fun contarProductos(id: Int): Int

    @Query("SELECT COUNT(*) FROM movimientos WHERE proveedorId = :id")
    suspend fun contarMovimientos(id: Int): Int

    @Insert
    suspend fun insertar(proveedor: ProveedorEntity): Long

    @Update
    suspend fun actualizar(proveedor: ProveedorEntity)

    @Delete
    suspend fun eliminar(proveedor: ProveedorEntity)
}

@Dao
interface ProductoDao {

    /** Busqueda por nombre o codigo (cadena vacia = todos). */
    @Query(
        "SELECT p.*, c.nombre AS categoriaNombre " +
            "FROM productos p INNER JOIN categorias c ON c.id = p.categoriaId " +
            "WHERE p.nombre LIKE '%' || :texto || '%' OR p.codigo LIKE '%' || :texto || '%' " +
            "ORDER BY p.nombre"
    )
    fun buscar(texto: String): Flow<List<ProductoVista>>

    @Query(
        "SELECT p.*, c.nombre AS categoriaNombre " +
            "FROM productos p INNER JOIN categorias c ON c.id = p.categoriaId " +
            "ORDER BY p.nombre"
    )
    suspend fun todosConCategoria(): List<ProductoVista>

    @Query("SELECT * FROM productos WHERE activo = 1 ORDER BY nombre")
    fun observeActivos(): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM productos WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): ProductoEntity?

    @Query("SELECT COUNT(*) FROM productos WHERE codigo = :codigo AND id != :excluirId")
    suspend fun contarPorCodigo(codigo: String, excluirId: Int): Int

    @Query("SELECT COUNT(*) FROM detalle_movimientos WHERE productoId = :id")
    suspend fun contarMovimientos(id: Int): Int

    @Insert
    suspend fun insertar(producto: ProductoEntity): Long

    @Update
    suspend fun actualizar(producto: ProductoEntity)

    @Delete
    suspend fun eliminar(producto: ProductoEntity)
}

@Dao
abstract class MovimientoDao {

    @Insert
    abstract suspend fun insertarMovimiento(movimiento: MovimientoEntity): Long

    @Insert
    abstract suspend fun insertarDetalle(detalle: DetalleMovimientoEntity): Long

    @Query("SELECT * FROM productos WHERE id = :id LIMIT 1")
    abstract suspend fun getProducto(id: Int): ProductoEntity?

    @Query("UPDATE productos SET stockActual = stockActual + :delta WHERE id = :id")
    abstract suspend fun ajustarStock(id: Int, delta: Int)

    @Query(
        "SELECT m.*, " +
            "(SELECT COUNT(*) FROM detalle_movimientos d WHERE d.movimientoId = m.id) AS items, " +
            "(SELECT COALESCE(SUM(d.cantidad * d.precioUnitario), 0.0) FROM detalle_movimientos d " +
            "WHERE d.movimientoId = m.id) AS total " +
            "FROM movimientos m ORDER BY m.fechaHora DESC"
    )
    abstract fun observeResumenes(): Flow<List<MovimientoResumen>>

    @Query(
        "SELECT p.nombre AS producto, p.codigo AS codigo, d.cantidad AS cantidad, " +
            "d.precioUnitario AS precioUnitario " +
            "FROM detalle_movimientos d INNER JOIN productos p ON p.id = d.productoId " +
            "WHERE d.movimientoId = :movimientoId"
    )
    abstract suspend fun detalles(movimientoId: Int): List<DetalleVista>

    @Query(
        "SELECT p.codigo AS codigo, d.cantidad AS cantidad, d.precioUnitario AS precioUnitario " +
            "FROM detalle_movimientos d INNER JOIN productos p ON p.id = d.productoId " +
            "WHERE d.movimientoId = :movimientoId"
    )
    abstract suspend fun detalleParaEnvio(movimientoId: Int): List<DetalleEnvio>

    @Query("SELECT * FROM movimientos WHERE syncEstado != 'ENVIADO' ORDER BY fechaHora")
    abstract suspend fun pendientes(): List<MovimientoEntity>

    @Query("SELECT COUNT(*) FROM movimientos WHERE syncEstado != 'ENVIADO'")
    abstract fun observarPendientes(): Flow<Int>

    @Query("SELECT COUNT(*) FROM movimientos WHERE syncEstado != 'ENVIADO'")
    abstract suspend fun contarPendientes(): Int

    @Query("UPDATE movimientos SET syncEstado = :estado WHERE id = :id")
    abstract suspend fun marcar(id: Int, estado: String)

    /**
     * Registra el movimiento, sus lineas y el cambio de stock en UNA sola transaccion.
     * Si alguna linea deja el stock en negativo se lanza la excepcion y se revierte todo.
     */
    @Transaction
    open suspend fun registrar(
        movimiento: MovimientoEntity,
        lineas: List<LineaMovimiento>
    ): Long {
        val movimientoId = insertarMovimiento(movimiento).toInt()
        for (linea in lineas) {
            val producto = getProducto(linea.productoId)
                ?: throw IllegalStateException("Producto no encontrado")
            val delta = if (TipoMovimiento.suma(movimiento.tipo)) linea.cantidad else -linea.cantidad
            if (producto.stockActual + delta < 0) {
                throw StockInsuficienteException(producto.nombre, producto.stockActual)
            }
            insertarDetalle(
                DetalleMovimientoEntity(
                    movimientoId = movimientoId,
                    productoId = linea.productoId,
                    cantidad = linea.cantidad,
                    precioUnitario = linea.precio
                )
            )
            ajustarStock(linea.productoId, delta)
        }
        return movimientoId.toLong()
    }
}

/**
 * Aplica en SQLite el catalogo que baja del servidor, todo en una transaccion.
 * Las filas se buscan por su clave natural: categoria por nombre, proveedor por RUC, producto por codigo.
 */
@Dao
abstract class CatalogoSyncDao {

    @Query("SELECT id FROM categorias WHERE LOWER(nombre) = LOWER(:nombre) LIMIT 1")
    abstract suspend fun idCategoria(nombre: String): Int?

    @Insert
    abstract suspend fun insertarCategoria(categoria: CategoriaEntity): Long

    @Query("UPDATE categorias SET nombre = :nombre, activo = :activo WHERE id = :id")
    abstract suspend fun actualizarCategoria(id: Int, nombre: String, activo: Boolean)

    @Query("SELECT id FROM proveedores WHERE ruc = :ruc LIMIT 1")
    abstract suspend fun idProveedor(ruc: String): Int?

    @Insert
    abstract suspend fun insertarProveedor(proveedor: ProveedorEntity): Long

    @Query(
        "UPDATE proveedores SET razonSocial = :razonSocial, telefono = :telefono, " +
            "activo = :activo WHERE id = :id"
    )
    abstract suspend fun actualizarProveedor(
        id: Int,
        razonSocial: String,
        telefono: String,
        activo: Boolean
    )

    @Query("SELECT id FROM productos WHERE codigo = :codigo LIMIT 1")
    abstract suspend fun idProducto(codigo: String): Int?

    @Insert
    abstract suspend fun insertarProducto(producto: ProductoEntity): Long

    @Query(
        "UPDATE productos SET nombre = :nombre, categoriaId = :categoriaId, " +
            "proveedorId = :proveedorId, precioCompra = :precioCompra, precioVenta = :precioVenta, " +
            "stockMinimo = :stockMinimo, activo = :activo WHERE id = :id"
    )
    abstract suspend fun actualizarProducto(
        id: Int,
        nombre: String,
        categoriaId: Int,
        proveedorId: Int?,
        precioCompra: Double,
        precioVenta: Double,
        stockMinimo: Int,
        activo: Boolean
    )

    @Query("UPDATE productos SET stockActual = :stock WHERE id = :id")
    abstract suspend fun fijarStock(id: Int, stock: Int)

    @Transaction
    open suspend fun aplicar(dto: CatalogoDto, conStock: Boolean) {
        dto.categorias.orEmpty().forEach { c ->
            val id = idCategoria(c.nombre)
            if (id == null) {
                insertarCategoria(CategoriaEntity(nombre = c.nombre, activo = c.activo))
            } else {
                actualizarCategoria(id, c.nombre, c.activo)
            }
        }

        dto.proveedores.orEmpty().forEach { p ->
            val id = idProveedor(p.ruc)
            val telefono = p.telefono.orEmpty()
            if (id == null) {
                insertarProveedor(
                    ProveedorEntity(
                        razonSocial = p.razonSocial,
                        ruc = p.ruc,
                        telefono = telefono,
                        activo = p.activo
                    )
                )
            } else {
                actualizarProveedor(id, p.razonSocial, telefono, p.activo)
            }
        }

        dto.productos.orEmpty().forEach { p ->
            val categoriaId = idCategoria(p.categoria) ?: return@forEach
            val proveedorId = p.proveedorRuc?.let { idProveedor(it) }
            val id = idProducto(p.codigo)
            if (id == null) {
                insertarProducto(
                    ProductoEntity(
                        codigo = p.codigo,
                        nombre = p.nombre,
                        categoriaId = categoriaId,
                        proveedorId = proveedorId,
                        precioCompra = p.precioCompra,
                        precioVenta = p.precioVenta,
                        stockMinimo = p.stockMinimo,
                        stockActual = p.stockActual,
                        activo = p.activo
                    )
                )
            } else {
                actualizarProducto(
                    id, p.nombre, categoriaId, proveedorId,
                    p.precioCompra, p.precioVenta, p.stockMinimo, p.activo
                )
                if (conStock) fijarStock(id, p.stockActual)
            }
        }
    }
}
