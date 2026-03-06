package com.example.spendantt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.spendantt.ui.screens.auth.LoginScreen
import com.example.spendantt.ui.theme.SpendAnttTheme
import com.example.spendantt.viewmodel.LoginViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpendAnttTheme {
                // Estado para saber si el usuario está autenticado
                val isLoggedIn = remember { mutableStateOf(false) }
                val currentUserId = remember { mutableStateOf<Int?>(null) }

                if (isLoggedIn.value) {
                    // Pantalla placeholder después del login
                    Scaffold { paddingValues ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("¡Bienvenido! Usuario ID: ${currentUserId.value}")
                        }
                    }
                } else {
                    // Mostrar pantalla de login
                    val loginViewModel = LoginViewModel(this)
                    LoginScreen(
                        viewModel = loginViewModel,
                        onLoginSuccess = { userId ->
                            currentUserId.value = userId
                            isLoggedIn.value = true
                        }
                    )
                }
            }
        }
    }
}