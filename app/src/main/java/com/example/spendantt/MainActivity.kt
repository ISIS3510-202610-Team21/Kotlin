package com.example.spendantt

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.example.spendantt.data.local.AppDatabase
import com.example.spendantt.data.repository.UserRepository
import com.example.spendantt.ui.navigation.AppNavigation
import com.example.spendantt.ui.navigation.Screen
import com.example.spendantt.ui.theme.SpendAnttTheme
import com.example.spendantt.work.NotificationSyncScheduler

class MainActivity : AppCompatActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        requestNotificationPermissionIfNeeded()
        NotificationSyncScheduler.schedulePeriodic(this)

        val activity = this

        setContent {
            SpendAnttTheme {
                val prefs = remember {
                    activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                }

                val currentUserId = remember { mutableStateOf<Int?>(null) }
                val hasLoggedInOnce = remember {
                    mutableStateOf(prefs.getBoolean(KEY_HAS_LOGGED_IN_ONCE, false))
                }
                val lastUserId = remember {
                    val savedId = prefs.getInt(KEY_LAST_USER_ID, -1)
                    mutableStateOf(if (savedId == -1) null else savedId)
                }

                var lastUserDisplayName by remember { mutableStateOf("") }
                LaunchedEffect(lastUserId.value) {
                    if (lastUserId.value != null) {
                        val db = AppDatabase.getInstance(activity)
                        val repo = UserRepository(db.userDao())
                        val user = repo.getUserById(lastUserId.value!!)
                        lastUserDisplayName = user?.displayName ?: user?.username ?: ""
                    }
                }

                val canUseBiometric = hasLoggedInOnce.value && canUseFingerprint(activity)
                val navController = rememberNavController()

                AppNavigation(
                    navController = navController,
                    context = activity,
                    currentUserId = currentUserId.value,
                    hasLoggedInOnce = hasLoggedInOnce.value,
                    lastUserDisplayName = lastUserDisplayName,
                    canUseBiometric = canUseBiometric,
                    onFingerprintClick = {
                        authenticateWithFingerprint(
                            onAuthenticated = {
                                val userId = lastUserId.value
                                if (userId != null) {
                                    currentUserId.value = userId
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Welcome.route) { inclusive = true }
                                    }
                                }
                            },
                            onFailedAttempt = {},
                            onFallbackToManual = {
                                navController.navigate(Screen.Login.route)
                            }
                        )
                    },
                    onLoginSuccess = { userId ->
                        currentUserId.value = userId
                        hasLoggedInOnce.value = true
                        lastUserId.value = userId
                        prefs.edit()
                            .putBoolean(KEY_HAS_LOGGED_IN_ONCE, true)
                            .putInt(KEY_LAST_USER_ID, userId)
                            .apply()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    },
                    onLogout = {
                        currentUserId.value = null
                    }
                )
            }
        }
    }

    private fun canUseFingerprint(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) return
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun authenticateWithFingerprint(
        onAuthenticated: () -> Unit,
        onFailedAttempt: () -> Unit,
        onFallbackToManual: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(this)
        var isHandled = false

        val biometricPrompt = BiometricPrompt(
            this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    if (!isHandled) { isHandled = true; onAuthenticated() }
                }
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    if (!isHandled) onFailedAttempt()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (!isHandled) { isHandled = true; onFallbackToManual() }
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
    }
}
