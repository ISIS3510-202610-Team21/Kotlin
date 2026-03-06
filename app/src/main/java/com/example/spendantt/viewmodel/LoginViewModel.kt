package com.example.spendantt.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import com.example.spendantt.data.local.AppDatabase
import com.example.spendantt.data.repository.UserRepository
import kotlinx.coroutines.launch

class LoginViewModel(context: Context) : ViewModel() {

    // Inicializar repositorio
    private val userRepository: UserRepository

    init {
        val database = AppDatabase.getInstance(context)
        userRepository = UserRepository(database.userDao())
    }

    // ── ESTADO ─────────────────────────────────────────────────
    private val _username = mutableStateOf("")
    val username: State<String> = _username

    private val _password = mutableStateOf("")
    val password: State<String> = _password

    private val _showPassword = mutableStateOf(false)
    val showPassword: State<Boolean> = _showPassword

    private val _errorMessage = mutableStateOf("")
    val errorMessage: State<String> = _errorMessage

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    // ── ACCIONES ───────────────────────────────────────────────
    fun onUsernameChange(newUsername: String) {
        _username.value = newUsername
        _errorMessage.value = ""
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
        _errorMessage.value = ""
    }

    fun toggleShowPassword() {
        _showPassword.value = !_showPassword.value
    }

    fun setErrorMessage(message: String) {
        _errorMessage.value = message
    }

    fun login(onSuccess: (Int) -> Unit) {
        // Validar campos vacíos
        if (_username.value.isEmpty() || _password.value.isEmpty()) {
            _errorMessage.value = "Por favor completa todos los campos"
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val result = userRepository.login(_username.value, _password.value)
                result.onSuccess { user ->
                    _isLoading.value = false
                    onSuccess(user.id) // Pasar el ID del usuario
                }.onFailure { exception ->
                    _errorMessage.value = exception.message ?: "Error de login"
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error desconocido"
                _isLoading.value = false
            }
        }
    }
}
