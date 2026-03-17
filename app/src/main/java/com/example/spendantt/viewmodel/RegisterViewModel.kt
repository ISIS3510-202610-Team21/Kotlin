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

    fun onUsernameChange(value: String) { _username.value = value; _errorMessage.value = "" }
    fun onEmailChange(value: String) { _email.value = value; _errorMessage.value = "" }
    fun onPasswordChange(value: String) { _password.value = value; _errorMessage.value = "" }
    fun toggleShowPassword() { _showPassword.value = !_showPassword.value }

    fun register(onSuccess: (Int) -> Unit) {
        if (_username.value.isBlank() || _email.value.isBlank() || _password.value.isBlank()) {
            _errorMessage.value = "Please fill all fields"
            return
        }
        if (!_email.value.contains("@")) {
            _errorMessage.value = "Enter a valid email"
            return
        }
        if (_password.value.length < 4) {
            _errorMessage.value = "Password must be at least 4 characters"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = userRepository.register(
                    username = _username.value.trim(),
                    email = _email.value.trim(),
                    password = _password.value
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
}