package com.example.spendantt.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spendantt.ui.screens.home.components.BudgetCard
import com.example.spendantt.ui.screens.home.components.CategoryBarChart
import com.example.spendantt.ui.screens.home.components.ExpenseListItem
import com.example.spendantt.ui.screens.home.components.MonthlyExpensesCard
import com.example.spendantt.ui.theme.SpendAntGreen
import com.example.spendantt.viewmodel.HomeViewModel

@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    LaunchedEffect(Unit) {
        viewModel.refreshDailyBudget()
    }

    if (viewModel.isLoading.value) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // ── HEADER ─────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SpendAntGreen)
                    .padding(16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "Home",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }

        // ── PRESUPUESTO DIARIO ─────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(8.dp))
            BudgetCard(dailyBudget = viewModel.dailyBudget.value)
        }

        // ── GASTOS DEL MES ─────────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(8.dp))
            MonthlyExpensesCard(monthlyExpenses = viewModel.monthlyExpenses.value)
        }

        // ── GRÁFICO DE CATEGORÍAS ──────────────────────────────
        item {
            Spacer(modifier = Modifier.height(8.dp))
            CategoryBarChart(categoryExpenses = viewModel.categoryExpenses.value)
        }

        // ── GASTOS DE HOY ──────────────────────────────────────
        if (viewModel.todayExpenses.value.isNotEmpty()) {
            item {
                Text(
                    text = "Today Expenses",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(16.dp)
                )
            }

            items(viewModel.todayExpenses.value) { expense ->
                ExpenseListItem(expense = expense)
            }
        }

        // ── GASTOS DE AYER ─────────────────────────────────────
        if (viewModel.yesterdayExpenses.value.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Yesterday Expenses",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(16.dp)
                )
            }

            items(viewModel.yesterdayExpenses.value) { expense ->
                ExpenseListItem(expense = expense)
            }
        }

        // ── ESPACIADOR FINAL ───────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
