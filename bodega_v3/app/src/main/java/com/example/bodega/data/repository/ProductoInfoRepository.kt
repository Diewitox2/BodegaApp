package com.example.bodega.data.repository

import com.example.bodega.constant.Constants
import com.example.bodega.data.remote.ProductoInfoApi
import retrofit2.HttpException

interface ProductoInfoRepository {

    /** Nombre del producto segun Open Food Facts, o null si no esta registrado. */
    suspend fun nombreDesdeCodigo(codigo: String): String?
}

class ProductoInfoRepositoryImpl(
    private val api: ProductoInfoApi
) : ProductoInfoRepository {

    override suspend fun nombreDesdeCodigo(codigo: String): String? {
        return try {
            val respuesta = api.buscarProducto(
                codigo = codigo,
                userAgent = Constants.USER_AGENT,
                campos = "product_name,brands"
            )
            if (respuesta.status != 1) return null

            val nombre = respuesta.product?.productName?.trim().orEmpty()
            val marca = respuesta.product?.brands?.split(",")?.firstOrNull()?.trim().orEmpty()
            when {
                nombre.isEmpty() -> null
                marca.isEmpty() -> nombre
                else -> "$nombre ($marca)"
            }
        } catch (e: HttpException) {
            if (e.code() == 404) null else throw e
        }
    }
}
