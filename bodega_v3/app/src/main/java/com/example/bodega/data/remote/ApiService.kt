package com.example.bodega.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Servicios REST propios (PHP + MySQL). El token se envia en la cabecera X-Token.
 * Servicios: login (POST), register (POST), catalogo (GET y POST) y movimientos (POST).
 */
interface ApiService {

    @POST("login.php")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @POST("register.php")
    suspend fun register(@Body body: RegisterRequest): Response<AuthResponse>

    @GET("catalogo.php")
    suspend fun catalogo(@Header("X-Token") token: String): Response<CatalogoDto>

    @POST("catalogo.php")
    suspend fun subirCatalogo(
        @Header("X-Token") token: String,
        @Body body: CatalogoDto
    ): Response<OkResponse>

    @POST("movimientos.php")
    suspend fun enviarMovimiento(
        @Header("X-Token") token: String,
        @Body body: MovimientoRequest
    ): Response<OkResponse>
}
