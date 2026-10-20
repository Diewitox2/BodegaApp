package com.example.bodega.data.repository

import com.example.bodega.data.local.UsuarioDao
import com.example.bodega.data.local.UsuarioEntity
import com.example.bodega.data.remote.ApiService
import com.example.bodega.data.remote.AuthResponse
import com.example.bodega.data.remote.LoginRequest
import com.example.bodega.data.remote.RegisterRequest
import com.example.bodega.data.remote.RetrofitClient
import com.example.bodega.session.SessionManager
import com.example.bodega.util.PasswordHasher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Registro e inicio de sesion. El servidor es la fuente de verdad (token, rol, primer ADMIN).
 * Cada ingreso exitoso deja una copia local (hash PBKDF2 + sal) para poder entrar sin internet.
 * Los metodos devuelven un mensaje de error, o null si todo salio bien.
 */
interface AuthRepository {

    /** Crea la cuenta en el servidor (necesita internet). La primera cuenta del sistema es ADMIN. */
    suspend fun registrar(nombre: String, email: String, clave: String): String?

    /** Inicia sesion con el servidor; si no hay internet, con la copia local. Deja la sesion abierta. */
    suspend fun iniciarSesion(email: String, clave: String): String?
}

class AuthRepositoryImpl(
    private val dao: UsuarioDao,
    private val api: ApiService,
    private val session: SessionManager
) : AuthRepository {

    override suspend fun registrar(nombre: String, email: String, clave: String): String? {
        val respuesta = try {
            api.register(RegisterRequest(nombre, email, clave))
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            return "Necesitas internet para registrarte"
        } catch (e: Exception) {
            return "No se pudo leer la respuesta del servidor"
        }
        if (!respuesta.isSuccessful) return RetrofitClient.mensajeError(respuesta)

        // Se guarda la copia local, pero la sesion se abre al iniciar sesion.
        val cuerpo = respuesta.body()
        val usuario = cuerpo?.usuario ?: return "Respuesta inválida del servidor"
        guardarCopiaLocal(usuario.nombre, usuario.email, usuario.rol, clave)
        return null
    }

    override suspend fun iniciarSesion(email: String, clave: String): String? {
        try {
            val respuesta = api.login(LoginRequest(email, clave))
            if (respuesta.isSuccessful) return abrirSesion(respuesta.body(), clave)
            if (respuesta.code() == 401) return "Correo o contraseña incorrectos"
            // Cualquier otro codigo: el servidor falla; se intenta con la copia local.
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Sin internet o respuesta no valida: se intenta con la copia local.
        }
        return iniciarSesionLocal(email, clave)
    }

    private suspend fun abrirSesion(cuerpo: AuthResponse?, clave: String): String? {
        val usuario = cuerpo?.usuario
        val token = cuerpo?.token
        if (usuario == null || token.isNullOrEmpty()) return "Respuesta inválida del servidor"

        val idLocal = guardarCopiaLocal(usuario.nombre, usuario.email, usuario.rol, clave)
        session.guardar(idLocal, usuario.nombre, usuario.rol, token)
        return null
    }

    /** Crea o actualiza (por correo) la copia local del usuario. Devuelve su id local. */
    private suspend fun guardarCopiaLocal(
        nombre: String,
        email: String,
        rol: String,
        clave: String
    ): Int {
        val sal = PasswordHasher.nuevaSal()
        val hash = withContext(Dispatchers.Default) { PasswordHasher.hash(clave, sal) }
        val existente = dao.getByEmail(email)
        return if (existente == null) {
            dao.insertar(
                UsuarioEntity(nombre = nombre, email = email, passwordHash = hash, salt = sal, rol = rol)
            ).toInt()
        } else {
            dao.actualizar(
                existente.copy(nombre = nombre, passwordHash = hash, salt = sal, rol = rol, activo = true)
            )
            existente.id
        }
    }

    private suspend fun iniciarSesionLocal(email: String, clave: String): String? {
        val usuario = dao.getByEmail(email)
            ?: return "No hay conexión con el servidor y esta cuenta no está guardada en este celular"
        if (!usuario.activo) return "Correo o contraseña incorrectos"

        val correcta = withContext(Dispatchers.Default) {
            PasswordHasher.verificar(clave, usuario.salt, usuario.passwordHash)
        }
        if (!correcta) return "Correo o contraseña incorrectos"

        // Sin internet no hay token nuevo: se conserva el anterior (si existe).
        session.guardar(usuario.id, usuario.nombre, usuario.rol, null)
        return null
    }
}
