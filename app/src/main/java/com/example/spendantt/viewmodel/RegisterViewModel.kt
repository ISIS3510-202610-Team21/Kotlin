package com.example.spendantt.viewmodel

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spendantt.data.local.AppDatabase
import com.example.spendantt.data.repository.UserRepository
import kotlinx.coroutines.launch

class RegisterViewModel(context: Context) : ViewModel() {

    private val userRepository = UserRepository(AppDatabase.getInstance(context).userDao())

    private val _username = mutableStateOf("")
    val username: State<String> = _username

    private val _email = mutableStateOf("")
    val email: State<String> = _email

    private val _password = mutableStateOf("")
    val password: State<String> = _password

    private val _showPassword = mutableStateOf(false)
    val showPassword: State<Boolean> = _showPassword

    private val _errorMessage = mutableStateOf("")
    val errorMessage: State<String> = _errorMessage

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    // Solo letras, dígitos y guion bajo — sin espacios ni emojis
    fun onUsernameChange(value: String) {
        val sanitized = value.replace(Regex("[^a-zA-Z0-9_]"), "").take(MAX_USERNAME_LENGTH)
        _username.value = sanitized
        _errorMessage.value = ""
    }

    // Sin espacios ni caracteres fuera de ASCII imprimible
    fun onEmailChange(value: String) {
        val sanitized = value.replace(Regex("[^\\x20-\\x7E]"), "").replace(" ", "").take(MAX_EMAIL_LENGTH)
        _email.value = sanitized
        _errorMessage.value = ""
    }

    // Sin espacios ni emojis — solo ASCII imprimible
    fun onPasswordChange(value: String) {
        val sanitized = value.replace(Regex("[^\\x21-\\x7E]"), "").take(MAX_PASSWORD_LENGTH)
        _password.value = sanitized
        _errorMessage.value = ""
    }

    fun toggleShowPassword() { _showPassword.value = !_showPassword.value }

    fun register(onSuccess: (Int) -> Unit) {
        val username = _username.value
        val email = _email.value.trim()
        val password = _password.value

        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Please fill all fields"
            return
        }
        if (username.length < MIN_USERNAME_LENGTH) {
            _errorMessage.value = "Username must be at least $MIN_USERNAME_LENGTH characters"
            return
        }
        if (!EMAIL_REGEX.matches(email)) {
            _errorMessage.value = "Enter a valid email address"
            return
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            _errorMessage.value = "Password must be at least $MIN_PASSWORD_LENGTH characters"
            return
        }
        if (!password.any { it.isUpperCase() }) {
            _errorMessage.value = "Password must contain at least one uppercase letter"
            return
        }
        if (!password.any { it.isLowerCase() }) {
            _errorMessage.value = "Password must contain at least one lowercase letter"
            return
        }
        if (!password.any { it.isDigit() }) {
            _errorMessage.value = "Password must contain at least one number"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = userRepository.register(
                    username = username,
                    email = email,
                    password = password
                )
                result.onSuccess { user ->
                    _isLoading.value = false
                    onSuccess(user.id)
                }.onFailure { exception ->
                    _errorMessage.value = exception.message ?: "Registration error"
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown error"
                _isLoading.value = false
            }
        }
    }

    companion object {
        private const val MIN_USERNAME_LENGTH = 3
        private const val MAX_USERNAME_LENGTH = 20
        private const val MAX_EMAIL_LENGTH = 100
        private const val MIN_PASSWORD_LENGTH = 8
        private const val MAX_PASSWORD_LENGTH = 64
        private val EMAIL_REGEX = Regex("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$")
    }
}