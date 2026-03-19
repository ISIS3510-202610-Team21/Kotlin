package com.example.spendantt.ui.navigation

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.spendantt.ui.screens.budget.ProfileScreen
import com.example.spendantt.ui.screens.expense.NewExpenseScreen
import com.example.spendantt.ui.screens.goal.GoalsRoute
import com.example.spendantt.ui.screens.home.HomeScreen
import com.example.spendantt.ui.screens.notifications.NotificationsScreen
import com.example.spendantt.viewmodel.BudgetViewModel
import com.example.spendantt.viewmodel.GoalsViewModel
import com.example.spendantt.viewmodel.HomeViewModel
import com.example.spendantt.viewmodel.LoginViewModel
import com.example.spendantt.viewmodel.NewExpenseViewModel
import com.example.spendantt.viewmodel.NotificationsViewModel
import com.example.spendantt.viewmodel.RegisterViewModel

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object Profile : Screen("profile")
    object Goals : Screen("goals")
    object Budget : Screen("budget")
    object NewExpense : Screen("new_expense")
    object Notifications : Screen("notifications")
}

private val routesWithoutNavBar = listOf(
    Screen.Welcome.route,
    Screen.Login.route,
    Screen.Register.route,
    Screen.NewExpense.route,
    Screen.Notifications.route
)

@Composable
fun AppNavigation(
    navController: NavHostController,
    context: Context,
    currentUserId: Int?,
    hasLoggedInOnce: Boolean = false,
    lastUserDisplayName: String = "",
    canUseBiometric: Boolean = false,
    onFingerprintClick: () -> Unit = {},
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
            composable(Screen.Welcome.route) {
                WelcomeScreen(
                    onLoginClick = {
                        if (hasLoggedInOnce && canUseBiometric) {
                            onFingerprintClick()
                        } else {
                            navController.navigate(Screen.Login.route)
                        }
                    },
                    onRegisterClick = { navController.navigate(Screen.Register.route) },
                    onLoginWithOtherUserClick = { navController.navigate(Screen.Login.route) },
                    hasLoggedInOnce = hasLoggedInOnce,
                    lastUserDisplayName = lastUserDisplayName
                )
            }

            composable(Screen.Login.route) {
                val loginViewModel = remember { LoginViewModel(context) }
                LoginScreen(
                    viewModel = loginViewModel,
                    onLoginSuccess = { userId -> onLoginSuccess(userId) },
                    onRegisterClick = { navController.navigate(Screen.Register.route) }
                )
            }

            composable(Screen.Register.route) {
                val registerViewModel = remember { RegisterViewModel(context) }
                RegisterScreen(
                    viewModel = registerViewModel,
                    onRegisterSuccess = { userId -> onLoginSuccess(userId) },
                    onBackToLogin = { navController.popBackStack() }
                )
            }

            composable(Screen.Home.route) {
                if (currentUserId == null) return@composable
                val homeViewModel = remember(currentUserId) {
                    HomeViewModel(context, currentUserId)
                }
                HomeScreen(
                    viewModel = homeViewModel,
                    onNotificationsClick = {
                        navController.navigate(Screen.Notifications.route) { launchSingleTop = true }
                    }
                )
            }

            composable(Screen.NewExpense.route) {
                if (currentUserId == null) return@composable
                val newExpenseViewModel = remember(currentUserId) {
                    NewExpenseViewModel(context, currentUserId)
                }
                NewExpenseScreen(
                    viewModel = newExpenseViewModel,
                    onClose = { navController.popBackStack() },
                    onSaved = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Profile.route) {
                if (currentUserId == null) return@composable
                val budgetViewModel = remember(currentUserId) {
                    BudgetViewModel(
                        incomeRepository = IncomeRepository(db.incomeDao()),
                        userId = currentUserId
                    )
                }
                ProfileScreen(
                    viewModel = budgetViewModel,
                    displayName = displayName,
                    handle = handle,
                    onIncomeClick = { navController.navigate(Screen.Budget.route) },
                    onGoalsClick = { navController.navigate(Screen.Goals.route) },
                    onEditClick = {}
                )
            }

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
                if (currentUserId == null) return@composable
                val goalsViewModel = remember(currentUserId) {
                    GoalsViewModel(context, currentUserId)
                }
                GoalsRoute(
                    viewModel = goalsViewModel,
                    onExit = {
                        val popped = navController.popBackStack()
                        if (!popped) {
                            navController.navigate(Screen.Home.route) { launchSingleTop = true }
                        }
                    }
                )
            }

            composable(Screen.Notifications.route) {
                if (currentUserId == null) return@composable
                val notificationsViewModel = remember(currentUserId) {
                    NotificationsViewModel(context, currentUserId)
                }
                NotificationsScreen(
                    notifications = notificationsViewModel.notifications.value,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
