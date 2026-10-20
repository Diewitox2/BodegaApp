package com.example.bodega.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bodega.data.local.ProductoEntity
import com.example.bodega.data.local.ProductoVista
import com.example.bodega.data.repository.ProductoRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Lista de productos con busqueda en SQLite. */
class ProductosViewModel(
    private val repository: ProductoRepository
) : ViewModel() {

    private val _texto = MutableStateFlow("")
    val texto: StateFlow<String> = _texto

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje

    /** null mientras Room entrega la primera lectura. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val productos: StateFlow<List<ProductoVista>?> = _texto
        .flatMapLatest { repository.buscar(it.trim()) }
        .stateIn<List<ProductoVista>?>(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    fun onTextoChange(nuevo: String) {
        _texto.value = nuevo
    }

    fun eliminar(producto: ProductoEntity) {
        viewModelScope.launch {
            val error = repository.eliminar(producto)
            _mensaje.value = error ?: "Producto eliminado"
        }
    }

    fun consumirMensaje() {
        _mensaje.value = null
    }
}
