package com.example.bodega.data.remote

// Los nombres en camelCase se convierten solos a snake_case en el JSON
// (ver RetrofitClient: FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).

data class LoginRequest(val email: String, val password: String)

data class RegisterRequest(val nombre: String, val email: String, val password: String)

data class UsuarioDto(
    val id: Int = 0,
    val nombre: String = "",
    val email: String = "",
    val rol: String = ""
)

data class AuthResponse(
    val token: String? = null,
    val usuario: UsuarioDto? = null
)

data class ErrorResponse(val error: String? = null)

data class OkResponse(
    val ok: Boolean = false,
    val duplicado: Boolean = false
)

// ----- Catalogo (se sube y se baja con la misma forma) -----

data class CategoriaDto(
    val nombre: String,
    val activo: Boolean = true
)

data class ProveedorDto(
    val razonSocial: String,
    val ruc: String,
    val telefono: String? = null,
    val activo: Boolean = true
)

data class ProductoDto(
    val codigo: String,
    val nombre: String,
    val categoria: String,
    val proveedorRuc: String? = null,
    val precioCompra: Double = 0.0,
    val precioVenta: Double = 0.0,
    val stockMinimo: Int = 0,
    /** Solo se usa al bajar el catalogo; el servidor ignora el stock que envia el celular. */
    val stockActual: Int = 0,
    val activo: Boolean = true
)

data class CatalogoDto(
    val categorias: List<CategoriaDto>? = null,
    val proveedores: List<ProveedorDto>? = null,
    val productos: List<ProductoDto>? = null
)

// ----- Movimientos -----

data class DetalleRequest(
    val codigo: String,
    val cantidad: Int,
    val precioUnitario: Double
)

data class MovimientoRequest(
    val uuid: String,
    val tipo: String,
    val fecha: String,
    val observacion: String,
    val proveedorRuc: String?,
    val detalle: List<DetalleRequest>
)
