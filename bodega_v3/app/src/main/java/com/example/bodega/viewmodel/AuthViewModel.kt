package com.example.bodega.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bodega.data.repository.AuthRepository
import com.example.bodega.session.SessionManager
import com.example.bodega.util.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val success: Boolean = false
)

/** Registro e inicio de sesion contra el servidor (con copia local en Room para entrar sin internet). */
class AuthViewModel(
    private val repository: AuthRepository,
    private val session: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    fun register(nombre: String, email: String, password: String, confirmPassword: String) {
        val nom = nombre.trim()
        val mail = email.trim().lowercase()

        val error = when {
            nom.isEmpty() || mail.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() ->
                "Completa todos los campos"
            !Validators.isValidEmail(mail) ->
                "Ingresa un correo electrónico válido"
            password.length < Validators.MIN_PASSWORD_LENGTH ->
                "La contraseña debe tener al menos ${Validators.MIN_PASSWORD_LENGTH} caracteres"
            password != confirmPassword ->
                "Las contraseñas no coinciden"
            else -> null
        }
        if (error != null) {
            _uiState.value = AuthUiState(errorMessage = error)
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            try {
                val mensaje = repository.registrar(nom, mail, password)
                _uiState.value = if (mensaje == null) {
                    AuthUiState(success = true)
                } else {
                    AuthUiState(errorMessage = mensaje)
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState(errorMessage = "No se pudo registrar la cuenta")
            }
        }
    }

    fun login(email: String, password: String) {
        val mail = email.trim().lowercase()

        if (mail.isEmpty() || password.isEmpty()) {
            _uiState.value = AuthUiState(errorMessage = "Ingresa correo y contraseña")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            try {
                val mensaje = repository.iniciarSesion(mail, password)
                _uiState.value = if (mensaje == null) {
                    AuthUiState(success = true)
                } else {
                    AuthUiState(errorMessage = mensaje)
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState(errorMessage = "No se pudo iniciar sesión")
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun consumeSuccess() {
        _uiState.value = AuthUiState()
    }
}
