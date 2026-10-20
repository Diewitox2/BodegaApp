package com.example.bodega.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bodega.data.local.CategoriaEntity
import com.example.bodega.data.local.ProductoEntity
import com.example.bodega.data.local.ProveedorEntity
import com.example.bodega.data.repository.CatalogoRepository
import com.example.bodega.data.repository.ProductoInfoRepository
import com.example.bodega.data.repository.ProductoRepository
import com.example.bodega.util.Formato
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductoFormUiState(
    val producto: ProductoEntity? = null,
    val guardando: Boolean = false,
    val buscandoNombre: Boolean = false,
    val nombreSugerido: String? = null,
    val aviso: String? = null,
    val error: String? = null,
    val completado: Boolean = false
)

/** Sirve a "Nuevo producto" y "Editar producto". */
class ProductoFormViewModel(
    private val productoRepo: ProductoRepository,
    catalogoRepo: CatalogoRepository,
    private val infoRepo: ProductoInfoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductoFormUiState())
    val uiState: StateFlow<ProductoFormUiState> = _uiState

    val categorias: StateFlow<List<CategoriaEntity>> = catalogoRepo.observeCategoriasActivas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val proveedores: StateFlow<List<ProveedorEntity>> = catalogoRepo.observeProveedoresActivos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun cargar(id: Int) {
        if (_uiState.value.producto != null) return
        viewModelScope.launch {
            val producto = productoRepo.getById(id)
            _uiState.update {
                if (producto != null) it.copy(producto = producto)
                else it.copy(error = "Producto no encontrado")
            }
        }
    }

    fun guardar(
        id: Int,
        codigo: String,
        nombre: String,
        categoriaId: Int?,
        proveedorId: Int?,
        precioCompra: String,
        precioVenta: String,
        stockMinimo: String,
        activo: Boolean
    ) {
        val cod = codigo.trim()
        val nom = nombre.trim()
        val compra = Formato.decimal(precioCompra)
        val venta = Formato.decimal(precioVenta)
        val minimo = stockMinimo.trim().toIntOrNull()

        if (cod.isEmpty() || nom.isEmpty()) return fail("Completa el código y el nombre")
        if (categoriaId == null) return fail("Selecciona una categoría")
        if (compra == null || venta == null || compra < 0 || venta < 0) {
            return fail("Ingresa precios válidos")
        }
        if (minimo == null || minimo < 0) return fail("Ingresa un stock mínimo válido")

        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, error = null) }
            try {
                val entidad = ProductoEntity(
                    id = id,
                    codigo = cod,
                    nombre = nom,
                    categoriaId = categoriaId,
                    proveedorId = proveedorId,
                    precioCompra = compra,
                    precioVenta = venta,
                    stockMinimo = minimo,
                    activo = activo
                )
                val error = productoRepo.guardar(entidad)
                if (error != null) {
                    fail(error)
                } else {
                    _uiState.update { it.copy(guardando = false, completado = true) }
                }
            } catch (e: Exception) {
                fail("No se pudo guardar el producto")
            }
        }
    }

    /** Servicio REST: consulta el nombre en Open Food Facts a partir del codigo. */
    fun buscarNombre(codigo: String) {
        val cod = codigo.trim()
        if (cod.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(buscandoNombre = true, aviso = null) }
            try {
                val nombre = infoRepo.nombreDesdeCodigo(cod)
                _uiState.update {
                    if (nombre != null) {
                        it.copy(buscandoNombre = false, nombreSugerido = nombre)
                    } else {
                        it.copy(
                            buscandoNombre = false,
                            aviso = "No se encontró el producto en Open Food Facts"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        buscandoNombre = false,
                        aviso = "No se pudo consultar el servicio. Revisa tu conexión"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun consumirSugerencia() {
        _uiState.update { it.copy(nombreSugerido = null) }
    }

    fun consumirAviso() {
        _uiState.update { it.copy(aviso = null) }
    }

    fun consumirCompletado() {
        _uiState.update { it.copy(completado = false) }
    }

    private fun fail(mensaje: String) {
        _uiState.update { it.copy(guardando = false, error = mensaje) }
    }
}
