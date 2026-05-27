package com.example.spendantt.ui.navigation

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.spendantt.data.currency.CurrencyProvider
import com.example.spendantt.data.repository.IncomeRepository
import com.example.spendantt.data.repository.UserRepository
import com.example.spendantt.ui.components.BottomNavBar
import com.example.spendantt.ui.screens.auth.LoginScreen
import com.example.spendantt.ui.screens.auth.RegisterScreen
import com.example.spendantt.ui.screens.auth.WelcomeScreen
import com.example.spendantt.ui.screens.onboarding.OnboardingScreen1
import com.example.spendantt.ui.screens.onboarding.OnboardingScreen2
import com.example.spendantt.ui.screens.onboarding.OnboardingScreen3
import com.example.spendantt.ui.screens.onboarding.OnboardingScreen4
import com.example.spendantt.ui.screens.budget.BudgetNavigation
import com.example.spendantt.ui.screens.budget.CurrencyConverterScreen
import com.example.spendantt.ui.screens.budget.EditProfileScreen
import com.example.spendantt.ui.screens.budget.ProfileScreen
import com.example.spendantt.ui.screens.expense.ExpenseDetailScreen
import com.example.spendantt.ui.screens.expense.NewExpenseScreen
import com.example.spendantt.ui.screens.goal.GoalsRoute
import com.example.spendantt.ui.screens.home.HomeScreen
import com.example.spendantt.ui.screens.notifications.NotificationsScreen
import com.example.spendantt.ui.screens.report.ReportFlowScreen
import com.example.spendantt.viewmodel.BudgetViewModel
import com.example.spendantt.viewmodel.ReportViewModel
import com.example.spendantt.viewmodel.GoalsViewModel
import com.example.spendantt.viewmodel.HomeViewModel
import com.example.spendantt.viewmodel.LoginViewModel
import com.example.spendantt.viewmodel.ExpenseDetailViewModel
import com.example.spendantt.viewmodel.NewExpenseViewModel
import com.example.spendantt.viewmodel.NotificationsViewModel
import com.example.spendantt.viewmodel.RegisterViewModel
import com.example.spendantt.viewmodel.AnalyticsViewModel
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object Register : Screen("register")
    object Onboarding1 : Screen("onboarding1")
    object Onboarding2 : Screen("onboarding2")
    object Onboarding3 : Screen("onboarding3")
    object Onboarding4 : Screen("onboarding4")
    object CalendarImportGate : Screen("calendar_import_gate")
    object Home : Screen("home")
    object Profile : Screen("profile")
    object EditProfile : Screen("edit_profile")
    object Goals : Screen("goals")
    object Budget : Screen("budget")
    object CurrencyConverter : Screen("currency_converter")
    object NewExpense : Screen("new_expense")
    object ExpenseDetail : Screen("expense_detail")
    object Notifications : Screen("notifications")
    object Report : Screen("report")
}

private val routesWithoutNavBar = listOf(
    Screen.Welcome.route,
    Screen.Login.route,
    Screen.Register.route,
    Screen.Onboarding1.route,
    Screen.Onboarding2.route,
    Screen.Onboarding3.route,
    Screen.Onboarding4.route,
    Screen.CalendarImportGate.route,
    Screen.CurrencyConverter.route,
    Screen.NewExpense.route,
    Screen.ExpenseDetail.route,
    Screen.EditProfile.route,
    Screen.Notifications.route,
    Screen.Report.route
)

@Composable
fun AppNavigation(
    navController: NavHostController,
    context: Context,
    currentUserId: Int?,
    hasLoggedInOnce: Boolean = false,
    lastUserDisplayName: String = "",
    canUseBiometric: Boolean = false,
    pendingExpenseId: Int? = null,
    onExpenseNavigationConsumed: () -> Unit = {},
    onFingerprintClick: () -> Unit = {},
    onLoginSuccess: (Int) -> Unit,
    onLogout: () -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val db = AppDatabase.getInstance(context)
    val userRepository = remember { UserRepository(db.userDao()) }
    val coroutineScope = rememberCoroutineScope()
    var displayName by remember { mutableStateOf("") }
    var handle by remember { mutableStateOf("") }
    var avatarPath by remember { mutableStateOf<String?>(null) }
    var firebaseUid by remember { mutableStateOf<String?>(null) }
    var hideGoalsBottomBar by remember { mutableStateOf(false) }

    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            val user = userRepository.getUserById(currentUserId)
            displayName = user?.displayName ?: user?.username ?: ""
            handle = user?.handle ?: "@${user?.username ?: ""}"
            avatarPath = user?.avatarPath
            firebaseUid = user?.firebaseUid
        }
    }

    // Navigate to expense detail when launched from a Bold notification
    LaunchedEffect(pendingExpenseId, currentUserId) {
        if (pendingExpenseId != null && currentUserId != null) {
            navController.navigate("${Screen.ExpenseDetail.route}/$pendingExpenseId") {
                launchSingleTop = true
            }
            onExpenseNavigationConsumed()
        }
    }

    Scaffold(
        bottomBar = {
            val shouldHideNavBar = routesWithoutNavBar.any { route ->
                currentRoute == route || currentRoute?.startsWith("$route/") == true
            } || (currentRoute == Screen.Goals.route && hideGoalsBottomBar)
            if (!shouldHideNavBar) {
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
                    onRegisterSuccess = { userId ->
                        navController.navigate("${Screen.Onboarding1.route}/$userId") {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    },
                    onBackToLogin = { navController.popBackStack() }
                )
            }

            composable("${Screen.Onboarding1.route}/{userId}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId")?.toIntOrNull() ?: 0
                OnboardingScreen1(
                    onContinue = { navController.navigate("${Screen.Onboarding2.route}/$userId") }
                )
            }

            composable("${Screen.Onboarding2.route}/{userId}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId")?.toIntOrNull() ?: 0
                OnboardingScreen2(
                    userId = userId,
                    onImportSuccess = { navController.navigate("${Screen.Onboarding3.route}/$userId") },
                    onSkip = { navController.navigate("${Screen.Onboarding3.route}/$userId") }
                )
            }

            composable("${Screen.Onboarding3.route}/{userId}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId")?.toIntOrNull() ?: 0
                OnboardingScreen3(
                    onAllowNotifications = { navController.navigate("${Screen.Onboarding4.route}/$userId") },
                    onSkip = { navController.navigate("${Screen.Onboarding4.route}/$userId") }
                )
            }

            composable("${Screen.Onboarding4.route}/{userId}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId")?.toIntOrNull() ?: 0
                OnboardingScreen4(
                    onAllowLocation = { onLoginSuccess(userId) },
                    onSkip = { onLoginSuccess(userId) }
                )
            }

            composable("${Screen.CalendarImportGate.route}/{userId}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId")?.toIntOrNull() ?: 0
                OnboardingScreen2(
                    userId = userId,
                    onImportSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.CalendarImportGate.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onSkip = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.CalendarImportGate.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                if (currentUserId == null) return@composable
                val homeViewModel = remember(currentUserId) {
                    HomeViewModel(context, currentUserId)
                }

                val analyticsViewModel = remember(currentUserId) {
                    AnalyticsViewModel(context, currentUserId)
                }
                LaunchedEffect(currentUserId) {
                    analyticsViewModel.logAllBusinessQuestions()
                }

                HomeScreen(
                    viewModel = homeViewModel,
                    onNotificationsClick = {
                        homeViewModel.markNotificationsAsRead()
                        navController.navigate(Screen.Notifications.route) { launchSingleTop = true }
                    },
                    onExpenseClick = { expenseId ->
                        navController.navigate("${Screen.ExpenseDetail.route}/$expenseId")
                    },
                    onLogoutClick = {
                        onLogout()
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(0) { inclusive = true }
                        }
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

            composable("${Screen.ExpenseDetail.route}/{expenseId}") { backStackEntry ->
                if (currentUserId == null) return@composable
                val expenseId = backStackEntry.arguments?.getString("expenseId")?.toIntOrNull()
                    ?: return@composable
                val expenseDetailViewModel = remember(currentUserId, expenseId, firebaseUid) {
                    ExpenseDetailViewModel(context, expenseId, currentUserId, firebaseUid)
                }
                ExpenseDetailScreen(
                    viewModel = expenseDetailViewModel,
                    onBack = { navController.popBackStack() },
                    onDeleted = {
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
                        context = context,
                        incomeRepository = IncomeRepository(db.incomeDao()),
                        userId = currentUserId,
                        firebaseUid = firebaseUid
                    )
                }
                ProfileScreen(
                    viewModel = budgetViewModel,
                    displayName = displayName,
                    handle = handle,
                    avatarPath = avatarPath,
                    onIncomeClick = { navController.navigate(Screen.Budget.route) },
                    onGoalsClick = { navController.navigate(Screen.Goals.route) },
                    onCurrencyClick = { navController.navigate(Screen.CurrencyConverter.route) },
                    onEditClick = { navController.navigate(Screen.EditProfile.route) },
                    onSummaryClick = { navController.navigate(Screen.Report.route) }
                )
            }

            composable(Screen.Report.route) {
                if (currentUserId == null) return@composable
                val reportViewModel = remember(currentUserId) {
                    ReportViewModel(context, currentUserId)
                }
                ReportFlowScreen(
                    viewModel = reportViewModel,
                    onExit = { navController.popBackStack() },
                )
            }

            composable(Screen.CurrencyConverter.route) {
                CurrencyConverterScreen(
                    onClose = { navController.popBackStack() },
                    onConfirm = { iso, rate ->
                        coroutineScope.launch {
                            CurrencyProvider.setActiveCurrency(context, iso, rate)
                        }
                    }
                )
            }

            composable(Screen.Budget.route) {
                if (currentUserId == null) return@composable
                val budgetViewModel = remember(currentUserId) {
                    BudgetViewModel(
                        context = context,
                        incomeRepository = IncomeRepository(db.incomeDao()),
                        userId = currentUserId,
                        firebaseUid = firebaseUid
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
                    GoalsViewModel(context, currentUserId, firebaseUid)
                }
                DisposableEffect(Unit) {
                    onDispose { hideGoalsBottomBar = false }
                }
                GoalsRoute(
                    viewModel = goalsViewModel,
                    onBottomBarVisibilityChange = { shouldHide ->
                        hideGoalsBottomBar = shouldHide
                    },
                    onExit = {
                        val popped = navController.popBackStack()
                        if (!popped) {
                            navController.navigate(Screen.Home.route) { launchSingleTop = true }
                        }
                    }
                )
            }

            composable(Screen.EditProfile.route) {
                if (currentUserId == null) return@composable
                EditProfileScreen(
                    currentDisplayName = displayName,
                    currentAvatarPath = avatarPath,
                    onBackClick = { navController.popBackStack() },
                    onSaveClick = { newDisplayName, selectedAvatarUri ->
                        coroutineScope.launch {
                            val result = userRepository.updateLocalProfile(
                                context = context.applicationContext,
                                userId = currentUserId,
                                displayName = newDisplayName,
                                avatarUri = selectedAvatarUri
                            )
                            result.onSuccess { updatedUser ->
                                displayName = updatedUser.displayName ?: updatedUser.username
                                avatarPath = updatedUser.avatarPath
                                handle = updatedUser.handle ?: "@${updatedUser.username}"
                                navController.popBackStack()
                            }
                        }
                    }
                )
            }

            composable(Screen.Notifications.route) {
                if (currentUserId == null) return@composable
                val notificationsViewModel = remember(currentUserId) {
                    NotificationsViewModel(context, currentUserId)
                }
                LaunchedEffect(Unit) {
                    notificationsViewModel.markAllAsRead()
                }
                NotificationsScreen(
                    notifications = notificationsViewModel.notifications.value,
                    onBackClick = { navController.popBackStack() },
                    onSimulateGooglePayClick = {
                        notificationsViewModel.simulateGooglePayExpense()
                    }
                )
            }
        }
    }
}
