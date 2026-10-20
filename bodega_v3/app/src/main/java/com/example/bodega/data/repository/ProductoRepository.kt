package com.example.bodega.data.repository

import com.example.bodega.data.local.ProductoDao
import com.example.bodega.data.local.ProductoEntity
import com.example.bodega.data.local.ProductoVista
import kotlinx.coroutines.flow.Flow

interface ProductoRepository {

    /** Busqueda en SQLite por nombre o codigo; cadena vacia devuelve todos. */
    fun buscar(texto: String): Flow<List<ProductoVista>>

    fun observeActivos(): Flow<List<ProductoEntity>>

    suspend fun getById(id: Int): ProductoEntity?

    /** Inserta (id = 0) o actualiza. Devuelve un mensaje de error o null. */
    suspend fun guardar(producto: ProductoEntity): String?

    /** Devuelve un mensaje de error o null. */
    suspend fun eliminar(producto: ProductoEntity): String?
}

class ProductoRepositoryImpl(
    private val dao: ProductoDao
) : ProductoRepository {

    override fun buscar(texto: String): Flow<List<ProductoVista>> = dao.buscar(texto)

    override fun observeActivos(): Flow<List<ProductoEntity>> = dao.observeActivos()

    override suspend fun getById(id: Int): ProductoEntity? = dao.getById(id)

    override suspend fun guardar(producto: ProductoEntity): String? {
        if (dao.contarPorCodigo(producto.codigo, producto.id) > 0) {
            return "Ya existe un producto con ese código"
        }
        return try {
            if (producto.id == 0) {
                // Un producto nuevo siempre empieza con stock 0; el stock entra con movimientos.
                dao.insertar(producto.copy(stockActual = 0))
            } else {
                val actual = dao.getById(producto.id) ?: return "El producto ya no existe"
                dao.actualizar(producto.copy(stockActual = actual.stockActual))
            }
            null
        } catch (e: Exception) {
            "No se pudo guardar el producto"
        }
    }

    override suspend fun eliminar(producto: ProductoEntity): String? {
        if (dao.contarMovimientos(producto.id) > 0) {
            return "No se puede eliminar: tiene movimientos. Desactívalo desde Editar."
        }
        return try {
            dao.eliminar(producto)
            null
        } catch (e: Exception) {
            "No se pudo eliminar el producto"
        }
    }
}
