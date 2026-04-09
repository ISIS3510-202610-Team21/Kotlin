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
import kotlinx.coroutines.launch

class LoginViewModel(context: Context) : ViewModel() {

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

    fun onUsernameChange(value: String) { _username.value = value; _errorMessage.value = "" }
    fun onPasswordChange(value: String) { _password.value = value; _errorMessage.value = "" }
    fun toggleShowPassword() { _showPassword.value = !_showPassword.value }

    fun login(onSuccess: (Int) -> Unit) {
        if (_username.value.isEmpty() || _password.value.isEmpty()) {
            _errorMessage.value = "Por favor completa todos los campos"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = userRepository.login(_username.value.trim(), _password.value)
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