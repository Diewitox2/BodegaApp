package com.example.bodega.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bodega.constant.TipoMovimiento
import com.example.bodega.data.local.LineaMovimiento
import com.example.bodega.data.local.ProductoEntity
import com.example.bodega.data.local.ProveedorEntity
import com.example.bodega.data.local.StockInsuficienteException
import com.example.bodega.data.repository.CatalogoRepository
import com.example.bodega.data.repository.MovimientoRepository
import com.example.bodega.data.repository.ProductoRepository
import com.example.bodega.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MovimientoFormUiState(
    val guardando: Boolean = false,
    val error: String? = null,
    val completado: Boolean = false
)

/** "Nuevo movimiento": entrada, salida o ajuste con varias lineas. */
class MovimientoFormViewModel(
    private val movimientoRepo: MovimientoRepository,
    productoRepo: ProductoRepository,
    catalogoRepo: CatalogoRepository,
    private val session: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MovimientoFormUiState())
    val uiState: StateFlow<MovimientoFormUiState> = _uiState

    val productos: StateFlow<List<ProductoEntity>> = productoRepo.observeActivos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val proveedores: StateFlow<List<ProveedorEntity>> = catalogoRepo.observeProveedoresActivos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun guardar(
        tipo: String,
        proveedorId: Int?,
        observacion: String,
        lineas: List<LineaMovimiento>
    ) {
        if (lineas.isEmpty()) return fail("Agrega al menos un producto")
        if (!session.haySesion) return fail("La sesión no es válida. Vuelve a iniciar sesión")

        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, error = null) }
            try {
                movimientoRepo.registrar(
                    tipo = tipo,
                    proveedorId = if (tipo == TipoMovimiento.ENTRADA) proveedorId else null,
                    observacion = observacion.trim(),
                    usuarioId = session.usuarioId,
                    lineas = lineas
                )
                _uiState.update { it.copy(guardando = false, completado = true) }
            } catch (e: StockInsuficienteException) {
                fail(e.message ?: "Stock insuficiente")
            } catch (e: Exception) {
                fail("No se pudo registrar el movimiento")
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun consumirCompletado() {
        _uiState.update { it.copy(completado = false) }
    }

    private fun fail(mensaje: String) {
        _uiState.update { it.copy(guardando = false, error = mensaje) }
    }
}
