package com.example.bodega

import android.app.Application
import com.example.bodega.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BodegaApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Categorias basicas para la primera ejecucion
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            container.catalogoRepository.sembrarSiVacio()
        }
    }
}
