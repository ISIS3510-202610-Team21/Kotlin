package com.example.spendantt.ui.screens.budget

import androidx.compose.runtime.*
import com.example.spendantt.viewmodel.BudgetViewModel

/**
 * BudgetNavigation
 *
 * Maneja la navegación interna entre las 3 pantallas de Set a Budget:
 *   Profile → BudgetAndIncome → NewIncome
 *
 * Uso en MainActivity o NavHost:
 *   BudgetNavigation(viewModel = budgetViewModel, ...)
 *
 * Cuando tengas Navigation Compose instalado, reemplaza este
 * composable por un NavGraph con las 3 rutas.
 */

private enum class BudgetScreen {
    PROFILE,
    BUDGET_AND_INCOME,
    NEW_INCOME
}

@Composable
fun BudgetNavigation(
    viewModel: BudgetViewModel,
    displayName: String,
    handle: String,
    onGoalsClick: () -> Unit,   // Navega a Goals (otra funcionalidad)
    onBackToMain: () -> Unit    // Sale del flujo de Budget
) {
    var currentScreen by remember { mutableStateOf(BudgetScreen.BUDGET_AND_INCOME) }

    when (currentScreen) {
        BudgetScreen.PROFILE -> {
            ProfileScreen(
                viewModel = viewModel,
                displayName = displayName,
                handle = handle,
                avatarPath = null,
                onIncomeClick = { currentScreen = BudgetScreen.BUDGET_AND_INCOME },
                onGoalsClick = onGoalsClick,
                onCurrencyClick = {},
                onEditClick = { /* TODO: EditProfileScreen */ },
                onSummaryClick = {}
            )
        }

        BudgetScreen.BUDGET_AND_INCOME -> {
            BudgetAndIncomeScreen(
                viewModel = viewModel,
                onClose = { currentScreen = BudgetScreen.PROFILE },
                onNewIncome = { currentScreen = BudgetScreen.NEW_INCOME }
            )
        }

        BudgetScreen.NEW_INCOME -> {
            NewIncomeScreen(
                viewModel = viewModel,
                onClose = { currentScreen = BudgetScreen.BUDGET_AND_INCOME },
                onSuccess = { currentScreen = BudgetScreen.BUDGET_AND_INCOME }
            )
        }
    }
}
