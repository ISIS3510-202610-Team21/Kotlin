package com.example.spendantt.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import android.content.Context
import com.example.spendantt.ui.screens.home.HomeScreen
import com.example.spendantt.ui.screens.auth.LoginScreen
import com.example.spendantt.viewmodel.HomeViewModel
import com.example.spendantt.viewmodel.LoginViewModel
import androidx.compose.runtime.remember
import androidx.compose.material3.Text


sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home") {
        fun createRoute(userId: Int) = "home/$userId"
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String,
    context: Context,
    currentUserId: Int?
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ── LOGIN SCREEN ──────────────────────────────────────
        composable(Screen.Login.route) {
            val loginViewModel = remember { LoginViewModel(context) }
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = { userId ->
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // ── HOME SCREEN ───────────────────────────────────────
        composable(Screen.Home.route) {
            if (currentUserId == null) {
                Text("No se pudo cargar el usuario")
                return@composable
            }

            val homeViewModel = remember(currentUserId) { HomeViewModel(context, currentUserId) }
            HomeScreen(viewModel = homeViewModel)
        }
    }
}