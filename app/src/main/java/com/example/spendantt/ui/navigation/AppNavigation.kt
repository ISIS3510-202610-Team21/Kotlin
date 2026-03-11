package com.example.spendantt.ui.navigation

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.spendantt.data.local.AppDatabase
import com.example.spendantt.data.repository.IncomeRepository
import com.example.spendantt.data.repository.UserRepository
import com.example.spendantt.ui.components.BottomNavBar
import com.example.spendantt.ui.screens.auth.LoginScreen
import com.example.spendantt.ui.screens.auth.RegisterScreen
import com.example.spendantt.ui.screens.auth.WelcomeScreen
import com.example.spendantt.ui.screens.budget.BudgetNavigation
import com.example.spendantt.ui.screens.goal.SetGoalFlowScreen
import com.example.spendantt.ui.screens.home.HomeScreen
import com.example.spendantt.viewmodel.BudgetViewModel
import com.example.spendantt.viewmodel.HomeViewModel
import com.example.spendantt.viewmodel.LoginViewModel
import com.example.spendantt.viewmodel.RegisterViewModel

sealed class Screen(val route: String) {
    object Welcome    : Screen("welcome")
    object Login      : Screen("login")
    object Register   : Screen("register")
    object Home       : Screen("home")
    object Profile    : Screen("profile")
    object Goals      : Screen("goals")
    object Budget     : Screen("budget")
    object NewExpense : Screen("new_expense")
}

private val routesWithoutNavBar = listOf(
    Screen.Welcome.route,
    Screen.Login.route,
    Screen.Register.route
)

@Composable
fun AppNavigation(
    navController: NavHostController,
    context: Context,
    currentUserId: Int?,
    hasLoggedInOnce: Boolean = false,
    lastUserDisplayName: String = "",
    onLoginSuccess: (Int) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val db = AppDatabase.getInstance(context)
    val userRepository = remember { UserRepository(db.userDao()) }
    var displayName by remember { mutableStateOf("") }
    var handle by remember { mutableStateOf("") }

    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            val user = userRepository.getUserById(currentUserId)
            displayName = user?.displayName ?: user?.username ?: ""
            handle = user?.handle ?: "@${user?.username ?: ""}"
        }
    }

    Scaffold(
        bottomBar = {
            if (currentRoute !in routesWithoutNavBar) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onProfileClick = {
                        navController.navigate(Screen.Profile.route) { launchSingleTop = true }
                    },
                    onHomeClick = {
                        navController.navigate(Screen.Home.route) { launchSingleTop = true }
                    },
                    onAddClick = {
                        navController.navigate(Screen.NewExpense.route) { launchSingleTop = true }
                    },
                    onGoalsClick = {
                        navController.navigate(Screen.Goals.route) { launchSingleTop = true }
                    },
                    onBudgetClick = {
                        navController.navigate(Screen.Budget.route) { launchSingleTop = true }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Welcome.route,
            modifier = Modifier.padding(paddingValues)
        ) {

            // ── WELCOME ───────────────────────────────────────
            composable(Screen.Welcome.route) {
                WelcomeScreen(
                    onLoginClick = { navController.navigate(Screen.Login.route) },
                    onRegisterClick = { navController.navigate(Screen.Register.route) },
                    hasLoggedInOnce = hasLoggedInOnce,              // ← AGREGA
                    lastUserDisplayName = lastUserDisplayName
                )
            }

            // ── LOGIN ─────────────────────────────────────────
            composable(Screen.Login.route) {
                val loginViewModel = remember { LoginViewModel(context) }
                LoginScreen(
                    viewModel = loginViewModel,
                    onLoginSuccess = { userId -> onLoginSuccess(userId) }
                )
            }

            // ── REGISTER ──────────────────────────────────────
            composable(Screen.Register.route) {
                val registerViewModel = remember { RegisterViewModel(context) }
                RegisterScreen(
                    viewModel = registerViewModel,
                    onRegisterSuccess = { userId -> onLoginSuccess(userId) },
                    onBackToLogin = { navController.popBackStack() }
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

            // ── PROFILE ───────────────────────────────────────
            composable(Screen.Profile.route) {
                if (currentUserId == null) return@composable
                val budgetViewModel = remember(currentUserId) {
                    BudgetViewModel(
                        incomeRepository = IncomeRepository(db.incomeDao()),
                        userId = currentUserId
                    )
                }
                BudgetNavigation(
                    viewModel = budgetViewModel,
                    displayName = displayName,
                    handle = handle,
                    onGoalsClick = { navController.navigate(Screen.Goals.route) },
                    onBackToMain = { navController.navigate(Screen.Home.route) }
                )
            }

            // ── BUDGET ────────────────────────────────────────
            composable(Screen.Budget.route) {
                if (currentUserId == null) return@composable
                val budgetViewModel = remember(currentUserId) {
                    BudgetViewModel(
                        incomeRepository = IncomeRepository(db.incomeDao()),
                        userId = currentUserId
                    )
                }
                BudgetNavigation(
                    viewModel = budgetViewModel,
                    displayName = displayName,
                    handle = handle,
                    onGoalsClick = { navController.navigate(Screen.Goals.route) },
                    onBackToMain = { navController.navigate(Screen.Home.route) }
                )
            }

            composable(Screen.Goals.route) {
                SetGoalFlowScreen(
                    onExit = {
                        val popped = navController.popBackStack()
                        if (!popped) {
                            navController.navigate(Screen.Home.route) {
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
            composable(Screen.NewExpense.route) { /* TODO */ }
        }
    }
}
