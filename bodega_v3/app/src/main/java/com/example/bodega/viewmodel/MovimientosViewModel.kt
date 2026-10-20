package com.example.bodega.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bodega.data.local.DetalleVista
import com.example.bodega.data.local.MovimientoResumen
import com.example.bodega.data.repository.MovimientoRepository
import com.example.bodega.data.repository.SesionExpiradaException
import com.example.bodega.data.repository.SyncRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetalleDialogo(
    val resumen: MovimientoResumen,
    val lineas: List<DetalleVista>
)

/** Historial de movimientos + sincronizacion de los pendientes. */
class MovimientosViewModel(
    private val movimientoRepo: MovimientoRepository,
    private val syncRepo: SyncRepository
) : ViewModel() {

    /** null mientras Room entrega la primera lectura. */
    val movimientos: StateFlow<List<MovimientoResumen>?> = movimientoRepo.observeResumenes()
        .stateIn<List<MovimientoResumen>?>(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val pendientes: StateFlow<Int> = movimientoRepo.observePendientes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _sincronizando = MutableStateFlow(false)
    val sincronizando: StateFlow<Boolean> = _sincronizando

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje

    private val _detalle = MutableStateFlow<DetalleDialogo?>(null)
    val detalle: StateFlow<DetalleDialogo?> = _detalle

    fun verDetalle(resumen: MovimientoResumen) {
        viewModelScope.launch {
            val lineas = movimientoRepo.detalles(resumen.movimiento.id)
            _detalle.value = DetalleDialogo(resumen, lineas)
        }
    }

    fun cerrarDetalle() {
        _detalle.value = null
    }

    fun sincronizar() {
        if (_sincronizando.value) return
        viewModelScope.launch {
            _sincronizando.value = true
            try {
                val r = syncRepo.sincronizar()
                _mensaje.value = when {
                    r.total == 0 -> "No hay movimientos pendientes. Catálogo actualizado."
                    r.fallidos == 0 -> "Se enviaron ${r.enviados} movimiento(s)"
                    else -> "Enviados: ${r.enviados}. Con error: ${r.fallidos}. " +
                        (r.aviso ?: "Reintenta luego.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: SesionExpiradaException) {
                _mensaje.value = e.message ?: "Sesión expirada"
            } catch (e: Exception) {
                _mensaje.value = "No se pudo sincronizar. Revisa tu conexión. ${e.message.orEmpty()}"
            } finally {
                _sincronizando.value = false
            }
        }
    }

    fun consumirMensaje() {
        _mensaje.value = null
    }
}
