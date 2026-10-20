package com.example.bodega.di

import android.content.Context
import com.example.bodega.data.local.AppDatabase
import com.example.bodega.data.remote.RetrofitClient
import com.example.bodega.data.repository.AuthRepository
import com.example.bodega.data.repository.AuthRepositoryImpl
import com.example.bodega.data.repository.CatalogoRepository
import com.example.bodega.data.repository.CatalogoRepositoryImpl
import com.example.bodega.data.repository.MovimientoRepository
import com.example.bodega.data.repository.MovimientoRepositoryImpl
import com.example.bodega.data.repository.ProductoInfoRepository
import com.example.bodega.data.repository.ProductoInfoRepositoryImpl
import com.example.bodega.data.repository.ProductoRepository
import com.example.bodega.data.repository.ProductoRepositoryImpl
import com.example.bodega.data.repository.SyncRepository
import com.example.bodega.data.repository.SyncRepositoryImpl
import com.example.bodega.session.SessionManager

/** Contenedor manual de dependencias (sin Hilt), igual que en el T2. */
class AppContainer(context: Context) {

    private val database = AppDatabase.getInstance(context)

    val session = SessionManager(context)

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(database.usuarioDao(), RetrofitClient.api, session)
    }

    val catalogoRepository: CatalogoRepository by lazy {
        CatalogoRepositoryImpl(database.categoriaDao(), database.proveedorDao())
    }

    val productoRepository: ProductoRepository by lazy {
        ProductoRepositoryImpl(database.productoDao())
    }

    val movimientoRepository: MovimientoRepository by lazy {
        MovimientoRepositoryImpl(database.movimientoDao())
    }

    val syncRepository: SyncRepository by lazy {
        SyncRepositoryImpl(
            database.movimientoDao(),
            database.productoDao(),
            database.categoriaDao(),
            database.proveedorDao(),
            database.catalogoSyncDao(),
            RetrofitClient.api,
            session
        )
    }

    val productoInfoRepository: ProductoInfoRepository by lazy {
        ProductoInfoRepositoryImpl(RetrofitClient.productoInfoApi)
    }
}
