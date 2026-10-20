package com.example.bodega.util

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Las contrasenas nunca se guardan: solo su hash PBKDF2 con una sal aleatoria por usuario. */
object PasswordHasher {
    private const val ITERACIONES = 120_000
    private const val LONGITUD_BITS = 256

    fun nuevaSal(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun hash(clave: String, sal: String): String {
        val spec = PBEKeySpec(
            clave.toCharArray(),
            Base64.decode(sal, Base64.NO_WRAP),
            ITERACIONES,
            LONGITUD_BITS
        )
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return Base64.encodeToString(factory.generateSecret(spec).encoded, Base64.NO_WRAP)
    }

    fun verificar(clave: String, sal: String, hashEsperado: String): Boolean {
        val calculado = hash(clave, sal)
        return MessageDigest.isEqual(calculado.toByteArray(), hashEsperado.toByteArray())
    }
}
