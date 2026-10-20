package com.example.bodega.data.repository

import com.example.bodega.data.local.CategoriaDao
import com.example.bodega.data.local.CategoriaEntity
import com.example.bodega.data.local.ProveedorDao
import com.example.bodega.data.local.ProveedorEntity
import com.example.bodega.util.Validators
import kotlinx.coroutines.flow.Flow

/** Categorias y proveedores. Los metodos que devuelven String? entregan un mensaje de error o null si todo salio bien. */
interface CatalogoRepository {

    fun observeCategorias(): Flow<List<CategoriaEntity>>
    fun observeCategoriasActivas(): Flow<List<CategoriaEntity>>
    fun observeProveedores(): Flow<List<ProveedorEntity>>
    fun observeProveedoresActivos(): Flow<List<ProveedorEntity>>

    /** Crea las categorias basicas la primera vez que se abre la app. */
    suspend fun sembrarSiVacio()

    suspend fun guardarCategoria(categoria: CategoriaEntity): String?
    suspend fun eliminarCategoria(categoria: CategoriaEntity): String?
    suspend fun guardarProveedor(proveedor: ProveedorEntity): String?
    suspend fun eliminarProveedor(proveedor: ProveedorEntity): String?
}

class CatalogoRepositoryImpl(
    private val categoriaDao: CategoriaDao,
    private val proveedorDao: ProveedorDao
) : CatalogoRepository {

    override fun observeCategorias(): Flow<List<CategoriaEntity>> = categoriaDao.observeTodas()

    override fun observeCategoriasActivas(): Flow<List<CategoriaEntity>> =
        categoriaDao.observeActivas()

    override fun observeProveedores(): Flow<List<ProveedorEntity>> = proveedorDao.observeTodos()

    override fun observeProveedoresActivos(): Flow<List<ProveedorEntity>> =
        proveedorDao.observeActivos()

    override suspend fun sembrarSiVacio() {
        if (categoriaDao.contar() > 0) return
        listOf("Abarrotes", "Bebidas", "Lácteos", "Limpieza", "Snacks", "Otros").forEach {
            categoriaDao.insertar(CategoriaEntity(nombre = it))
        }
    }

    override suspend fun guardarCategoria(categoria: CategoriaEntity): String? {
        val nombre = categoria.nombre.trim()
        if (nombre.isEmpty()) return "Ingresa el nombre de la categoría"
        if (categoriaDao.contarPorNombre(nombre, categoria.id) > 0) {
            return "Ya existe una categoría con ese nombre"
        }
        return try {
            val limpia = categoria.copy(nombre = nombre)
            if (limpia.id == 0) categoriaDao.insertar(limpia) else categoriaDao.actualizar(limpia)
            null
        } catch (e: Exception) {
            "No se pudo guardar la categoría"
        }
    }

    override suspend fun eliminarCategoria(categoria: CategoriaEntity): String? {
        if (categoriaDao.contarProductos(categoria.id) > 0) {
            return "No se puede eliminar: tiene productos. Puedes desactivarla."
        }
        return try {
            categoriaDao.eliminar(categoria)
            null
        } catch (e: Exception) {
            "No se pudo eliminar la categoría"
        }
    }

    override suspend fun guardarProveedor(proveedor: ProveedorEntity): String? {
        val razon = proveedor.razonSocial.trim()
        val ruc = proveedor.ruc.trim()
        if (razon.isEmpty()) return "Ingresa la razón social"
        if (!Validators.isValidRuc(ruc)) return "El RUC debe tener 11 dígitos"
        if (proveedorDao.contarPorRuc(ruc, proveedor.id) > 0) {
            return "Ya existe un proveedor con ese RUC"
        }
        return try {
            val limpio = proveedor.copy(
                razonSocial = razon,
                ruc = ruc,
                telefono = proveedor.telefono.trim()
            )
            if (limpio.id == 0) proveedorDao.insertar(limpio) else proveedorDao.actualizar(limpio)
            null
        } catch (e: Exception) {
            "No se pudo guardar el proveedor"
        }
    }

    override suspend fun eliminarProveedor(proveedor: ProveedorEntity): String? {
        if (proveedorDao.contarProductos(proveedor.id) > 0 ||
            proveedorDao.contarMovimientos(proveedor.id) > 0
        ) {
            return "No se puede eliminar: está en uso. Puedes desactivarlo."
        }
        return try {
            proveedorDao.eliminar(proveedor)
            null
        } catch (e: Exception) {
            "No se pudo eliminar el proveedor"
        }
    }
}
