package com.example.bodega.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Servicio REST #2 (GET): consulta publica de Open Food Facts por codigo de barras.
 * Datos bajo licencia ODbL: se cita la fuente en la pantalla del producto.
 */
interface ProductoInfoApi {

    @GET("api/v2/product/{codigo}.json")
    suspend fun buscarProducto(
        @Path("codigo") codigo: String,
        @Header("User-Agent") userAgent: String,
        @Query("fields") campos: String
    ): ProductoInfoResponse
}

data class ProductoInfoResponse(
    val status: Int? = null,
    val product: ProductoInfo? = null
)

data class ProductoInfo(
    @SerializedName("product_name")
    val productName: String? = null,
    val brands: String? = null
)
