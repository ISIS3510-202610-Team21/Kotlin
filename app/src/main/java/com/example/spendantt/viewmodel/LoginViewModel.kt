package com.example.spendantt.viewmodel

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spendantt.data.local.AppDatabase
import com.example.spendantt.data.repository.UserRepository
import com.example.spendantt.data.service.SyncService
import com.example.spendantt.util.AnalyticsHelper
import com.example.spendantt.util.ConnectivityObserver
import com.google.firebase.FirebaseNetworkException
import kotlinx.coroutines.launch

class LoginViewModel(private val context: Context) : ViewModel() {

    private val userRepository = UserRepository(AppDatabase.getInstance(context).userDao())
    private val syncService = SyncService(context)

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

    private val _offlineLoginBlocked = mutableStateOf(false)
    val offlineLoginBlocked: State<Boolean> = _offlineLoginBlocked

    fun dismissOfflineDialog() { _offlineLoginBlocked.value = false }

    // Solo letras, dígitos y guion bajo — sin espacios ni emojis
    fun onUsernameChange(value: String) {
        val sanitized = value.replace(Regex("[^a-zA-Z0-9_]"), "").take(MAX_USERNAME_LENGTH)
        _username.value = sanitized
        _errorMessage.value = ""
    }

    // Sin espacios ni emojis — solo ASCII imprimible
    fun onPasswordChange(value: String) {
        val sanitized = value.replace(Regex("[^\\x21-\\x7E]"), "").take(MAX_PASSWORD_LENGTH)
        _password.value = sanitized
        _errorMessage.value = ""
    }

    fun toggleShowPassword() { _showPassword.value = !_showPassword.value }

    fun login(onSuccess: (Int) -> Unit) {
        val username = _username.value
        val password = _password.value

        if (username.isEmpty() || password.isEmpty()) {
            _errorMessage.value = "Please fill all fields"
            return
        }
        if (username.length < MIN_USERNAME_LENGTH) {
            _errorMessage.value = "Username must be at least $MIN_USERNAME_LENGTH characters"
            return
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            _errorMessage.value = "Incorrect username or password"
            return
        }

        _isLoading.value = true
        // MULTI-THREADING | Dilan | 5pts | Corrutina con dispatcher: viewModelScope usa Dispatchers.Main por defecto, ligando la corrutina al ciclo de vida del ViewModel
        // MULTI-THREADING | Dilan | 10pts | Múltiples corrutinas anidadas: launch{} anidado dentro de viewModelScope.launch{} para sincronizar Firebase en paralelo sin bloquear la respuesta al usuario
        viewModelScope.launch {
            try {
                val isOnline = ConnectivityObserver.hasInternet(context)
                val result = userRepository.login(username, password)
                result.onSuccess { user ->
                    launch {
                        val firebaseUid = user.firebaseUid
                        if (firebaseUid != null) {
                            syncService.syncUserData(firebaseUid, user.id)
                        }
                    }
                    _isLoading.value = false
                    onSuccess(user.id)
                }.onFailure { exception ->
                    _isLoading.value = false
                    if (!isOnline || isNetworkError(exception)) {
                        _offlineLoginBlocked.value = true
                    } else {
                        _errorMessage.value = exception.message ?: "Error de login"
                    }
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = if (isNetworkError(e))
                    "Connection lost. Please check your internet and try again."
                else e.message ?: "Error desconocido"
            }
        }
    }

    private fun isNetworkError(e: Throwable): Boolean {
        if (e is FirebaseNetworkException) return true
        if (!ConnectivityObserver.isConnected.value) return true
        val msg = e.message?.lowercase() ?: ""
        return msg.contains("network") || msg.contains("internal error") ||
               msg.contains("timeout") || msg.contains("interrupted")
    }

    companion object {
        private const val MIN_USERNAME_LENGTH = 3
        private const val MAX_USERNAME_LENGTH = 20
        private const val MIN_PASSWORD_LENGTH = 8
        private const val MAX_PASSWORD_LENGTH = 64
    }
}