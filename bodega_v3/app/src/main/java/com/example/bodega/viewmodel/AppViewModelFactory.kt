package com.example.bodega.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.bodega.di.AppContainer

/** Una sola factory para todos los ViewModels de la app. */
class AppViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) ->
                AuthViewModel(container.authRepository, container.session)

            modelClass.isAssignableFrom(ProductosViewModel::class.java) ->
                ProductosViewModel(container.productoRepository)

            modelClass.isAssignableFrom(ProductoFormViewModel::class.java) ->
                ProductoFormViewModel(
                    container.productoRepository,
                    container.catalogoRepository,
                    container.productoInfoRepository
                )

            modelClass.isAssignableFrom(MovimientosViewModel::class.java) ->
                MovimientosViewModel(container.movimientoRepository, container.syncRepository)

            modelClass.isAssignableFrom(MovimientoFormViewModel::class.java) ->
                MovimientoFormViewModel(
                    container.movimientoRepository,
                    container.productoRepository,
                    container.catalogoRepository,
                    container.session
                )

            modelClass.isAssignableFrom(CatalogosViewModel::class.java) ->
                CatalogosViewModel(container.catalogoRepository)

            else -> throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
        } as T
    }
}
