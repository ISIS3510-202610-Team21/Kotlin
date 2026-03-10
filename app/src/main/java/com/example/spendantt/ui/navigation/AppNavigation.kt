package com.example.spendantt.ui.navigation

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.spendantt.data.local.AppDatabase
import com.example.spendantt.data.repository.IncomeRepository
import com.example.spendantt.ui.components.BottomNavBar
import com.example.spendantt.ui.screens.auth.LoginScreen
import com.example.spendantt.ui.screens.budget.BudgetNavigation
import com.example.spendantt.ui.screens.home.HomeScreen
import com.example.spendantt.viewmodel.BudgetViewModel
import com.example.spendantt.viewmodel.HomeViewModel
import com.example.spendantt.viewmodel.LoginViewModel
import androidx.compose.foundation.layout.WindowInsets


// ── RUTAS ─────────────────────────────────────────────────────────────────────

sealed class Screen(val route: String) {
    object Login   : Screen("login")
    object Home    : Screen("home")
    object Profile : Screen("profile")
    object Goals   : Screen("goals")
    object Budget  : Screen("budget")
    object NewExpense : Screen("new_expense")
}

// Rutas donde NO se muestra la BottomNavBar
private val routesWithoutNavBar = listOf(Screen.Login.route)

// ── NAVEGACIÓN PRINCIPAL ──────────────────────────────────────────────────────

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String,
    context: Context,
    currentUserId: Int?
) {
    // Ruta actual para saber qué icono marcar como activo
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            // Solo mostrar si no estamos en Login
            if (currentRoute !in routesWithoutNavBar) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onProfileClick = {
                        navController.navigate(Screen.Profile.route) {
                            launchSingleTop = true
                        }
                    },
                    onHomeClick = {
                        navController.navigate(Screen.Home.route) {
                            launchSingleTop = true
                        }
                    },
                    onAddClick = {
                        navController.navigate(Screen.NewExpense.route) {
                            launchSingleTop = true
                        }
                    },
                    onGoalsClick = {
                        navController.navigate(Screen.Goals.route) {
                            launchSingleTop = true
                        }
                    },
                    onBudgetClick = {
                        navController.navigate(Screen.Budget.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        },
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        ) {

            // ── LOGIN ─────────────────────────────────────────
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

            // ── HOME ──────────────────────────────────────────
            composable(Screen.Home.route) {
                if (currentUserId == null) return@composable
                val homeViewModel = remember(currentUserId) {
                    HomeViewModel(context, currentUserId)
                }
                HomeScreen(viewModel = homeViewModel)
            }

            // ── PROFILE + BUDGET ──────────────────────────────
            // Profile y Budget comparten el mismo flujo (BudgetNavigation)
            composable(Screen.Profile.route) {
                if (currentUserId == null) return@composable
                val db = AppDatabase.getInstance(context)
                val budgetViewModel = remember(currentUserId) {
                    BudgetViewModel(
                        incomeRepository = IncomeRepository(db.incomeDao()),
                        userId = currentUserId
                    )
                }
                BudgetNavigation(
                    viewModel = budgetViewModel,
                    displayName = "John Doe",       // TODO: traer de UserRepository
                    handle = "@user$currentUserId",
                    onGoalsClick = {
                        navController.navigate(Screen.Goals.route)
                    },
                    onBackToMain = {
                        navController.navigate(Screen.Home.route)
                    }
                )
            }

            // ── GOALS ─────────────────────────────────────────
            composable(Screen.Goals.route) {
                // TODO: GoalsScreen (funcionalidad 7)
            }

            // ── NEW EXPENSE ───────────────────────────────────
            composable(Screen.NewExpense.route) {
                // TODO: NewExpenseScreen (funcionalidad 3)
            }

            // Budget usa la misma pantalla que Profile
            composable(Screen.Budget.route) {
                if (currentUserId == null) return@composable
                val db = AppDatabase.getInstance(context)
                val budgetViewModel = remember(currentUserId) {
                    BudgetViewModel(
                        incomeRepository = IncomeRepository(db.incomeDao()),
                        userId = currentUserId
                    )
                }
                BudgetNavigation(
                    viewModel = budgetViewModel,
                    displayName = "John Doe",
                    handle = "@user$currentUserId",
                    onGoalsClick = {
                        navController.navigate(Screen.Goals.route)
                    },
                    onBackToMain = {
                        navController.navigate(Screen.Home.route)
                    }
                )
            }
        }
    }
}