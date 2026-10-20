package com.example.bodega.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bodega.data.local.CategoriaEntity
import com.example.bodega.data.local.ProveedorEntity
import com.example.bodega.data.repository.CatalogoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Mantenimiento de categorias y proveedores. */
class CatalogosViewModel(
    private val repository: CatalogoRepository
) : ViewModel() {

    val categorias: StateFlow<List<CategoriaEntity>?> = repository.observeCategorias()
        .stateIn<List<CategoriaEntity>?>(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val proveedores: StateFlow<List<ProveedorEntity>?> = repository.observeProveedores()
        .stateIn<List<ProveedorEntity>?>(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje

    /** onResultado recibe el mensaje de error, o null si se guardo bien. */
    fun guardarCategoria(
        id: Int,
        nombre: String,
        activo: Boolean,
        onResultado: (String?) -> Unit
    ) {
        viewModelScope.launch {
            val error = repository.guardarCategoria(
                CategoriaEntity(id = id, nombre = nombre, activo = activo)
            )
            if (error == null) _mensaje.value = "Categoría guardada"
            onResultado(error)
        }
    }

    fun eliminarCategoria(categoria: CategoriaEntity) {
        viewModelScope.launch {
            _mensaje.value = repository.eliminarCategoria(categoria) ?: "Categoría eliminada"
        }
    }

    fun guardarProveedor(
        id: Int,
        razonSocial: String,
        ruc: String,
        telefono: String,
        activo: Boolean,
        onResultado: (String?) -> Unit
    ) {
        viewModelScope.launch {
            val error = repository.guardarProveedor(
                ProveedorEntity(
                    id = id,
                    razonSocial = razonSocial,
                    ruc = ruc,
                    telefono = telefono,
                    activo = activo
                )
            )
            if (error == null) _mensaje.value = "Proveedor guardado"
            onResultado(error)
        }
    }

    fun eliminarProveedor(proveedor: ProveedorEntity) {
        viewModelScope.launch {
            _mensaje.value = repository.eliminarProveedor(proveedor) ?: "Proveedor eliminado"
        }
    }

    fun consumirMensaje() {
        _mensaje.value = null
    }
}
