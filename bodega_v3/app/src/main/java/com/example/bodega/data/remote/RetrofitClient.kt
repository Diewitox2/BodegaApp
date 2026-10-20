package com.example.bodega.data.remote

import com.example.bodega.constant.Constants
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private val gson: Gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()

    // Los hostings gratuitos pueden tardar en responder: damos mas tiempo que el valor por defecto.
    private val cliente: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(cliente)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val api: ApiService by lazy {
        retrofit(Constants.API_BASE_URL).create(ApiService::class.java)
    }

    val productoInfoApi: ProductoInfoApi by lazy {
        retrofit(Constants.PRODUCT_INFO_BASE_URL).create(ProductoInfoApi::class.java)
    }

    /** Mensaje de error que envia el servidor ({"error": "..."}) o uno generico. */
    fun mensajeError(respuesta: Response<*>): String {
        val texto = try {
            respuesta.errorBody()?.string().orEmpty()
        } catch (e: Exception) {
            ""
        }
        val mensaje = try {
            gson.fromJson(texto, ErrorResponse::class.java)?.error
        } catch (e: Exception) {
            null
        }
        return mensaje ?: "Error del servidor (${respuesta.code()})"
    }
}
