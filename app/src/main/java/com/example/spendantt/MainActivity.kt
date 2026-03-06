package com.example.spendantt

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.spendantt.data.local.AppDatabase
import com.example.spendantt.data.repository.UserRepository
import com.example.spendantt.ui.screens.auth.LoginScreen
import com.example.spendantt.ui.theme.SpendAnttTheme
import com.example.spendantt.viewmodel.LoginViewModel
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val activity = this
        seedTestUser()

        setContent {
            SpendAnttTheme {
                val prefs = remember {
                    activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                }

                val isLoggedIn = remember { mutableStateOf(false) }
                val currentUserId = remember { mutableStateOf<Int?>(null) }
                val hasLoggedInOnce = remember {
                    mutableStateOf(prefs.getBoolean(KEY_HAS_LOGGED_IN_ONCE, false))
                }
                val lastUserId = remember {
                    val savedId = prefs.getInt(KEY_LAST_USER_ID, -1)
                    mutableStateOf(if (savedId == -1) null else savedId)
                }
                val biometricFailures = remember { mutableStateOf(0) }
                val forceManualLogin = remember { mutableStateOf(false) }

                val loginViewModel = remember { LoginViewModel(activity) }
                val shouldUseBiometric = hasLoggedInOnce.value &&
                    !forceManualLogin.value &&
                    canUseFingerprint(activity)

                if (isLoggedIn.value) {
                    Scaffold { paddingValues ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Bienvenido. Usuario ID: ${currentUserId.value}")
                        }
                    }
                } else {
                    if (shouldUseBiometric) {
                        LoginScreen(
                            viewModel = loginViewModel,
                            onLoginSuccess = {},
                            loginButtonText = "Ingresar con huella",
                            useBiometricMode = true,
                            manualFieldsEnabled = false,
                            showManualFallbackAction = true,
                            onUseManualLogin = {
                                forceManualLogin.value = true
                                loginViewModel.setErrorMessage("")
                            },
                            onBiometricLoginClick = {
                                authenticateWithFingerprint(
                                    onAuthenticated = {
                                        biometricFailures.value = 0
                                        val userId = lastUserId.value
                                        if (userId != null) {
                                            currentUserId.value = userId
                                            isLoggedIn.value = true
                                            loginViewModel.setErrorMessage("")
                                        } else {
                                            forceManualLogin.value = true
                                            loginViewModel.setErrorMessage(
                                                "No hay usuario guardado. Inicia sesion manual."
                                            )
                                        }
                                    },
                                    onFailedAttempt = {
                                        biometricFailures.value += 1
                                        if (biometricFailures.value >= MAX_BIOMETRIC_ATTEMPTS) {
                                            forceManualLogin.value = true
                                            loginViewModel.setErrorMessage(
                                                "Fallaste 5 intentos con huella. Usa login manual."
                                            )
                                        }
                                    },
                                    onFallbackToManual = {
                                        forceManualLogin.value = true
                                        if (biometricFailures.value < MAX_BIOMETRIC_ATTEMPTS) {
                                            loginViewModel.setErrorMessage(
                                                "Usa login manual para continuar."
                                            )
                                        }
                                    }
                                )
                            }
                        )
                    } else {
                        LoginScreen(
                            viewModel = loginViewModel,
                            onLoginSuccess = { userId ->
                                currentUserId.value = userId
                                isLoggedIn.value = true
                                hasLoggedInOnce.value = true
                                lastUserId.value = userId
                                biometricFailures.value = 0
                                forceManualLogin.value = false
                                loginViewModel.setErrorMessage("")

                                prefs.edit()
                                    .putBoolean(KEY_HAS_LOGGED_IN_ONCE, true)
                                    .putInt(KEY_LAST_USER_ID, userId)
                                    .apply()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun seedTestUser() {
        lifecycleScope.launch {
            val database = AppDatabase.getInstance(applicationContext)
            val repository = UserRepository(database.userDao())
            repository.ensureTestUser()
        }
    }

    private fun canUseFingerprint(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun authenticateWithFingerprint(
        onAuthenticated: () -> Unit,
        onFailedAttempt: () -> Unit,
        onFallbackToManual: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(this)
        var isHandled = false

        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    if (!isHandled) {
                        isHandled = true
                        onAuthenticated()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    if (!isHandled) {
                        onFailedAttempt()
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (!isHandled) {
                        isHandled = true
                        onFallbackToManual()
                    }
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Ingreso con huella")
            .setSubtitle("Confirma tu identidad")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText("Usar contrasena")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    companion object {
        private const val PREFS_NAME = "auth_prefs"
        private const val KEY_HAS_LOGGED_IN_ONCE = "has_logged_in_once"
        private const val KEY_LAST_USER_ID = "last_user_id"
        private const val MAX_BIOMETRIC_ATTEMPTS = 5
    }
}
