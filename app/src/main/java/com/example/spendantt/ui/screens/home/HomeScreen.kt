package com.example.spendantt.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.example.spendantt.data.local.entity.ExpenseEntity
import com.example.spendantt.data.local.entity.ExpenseWithLabels
import com.example.spendantt.data.local.entity.LabelEntity
import com.example.spendantt.ui.screens.home.components.BudgetCard
import com.example.spendantt.ui.screens.home.components.CategoryBarChart
import com.example.spendantt.ui.screens.home.components.ExpenseListItem
import com.example.spendantt.ui.screens.home.components.MonthlyExpensesCard
import com.example.spendantt.ui.theme.SpendAntFontFamily
import com.example.spendantt.ui.theme.SpendAntGreen
import com.example.spendantt.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.LinkedHashMap
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNotificationsClick: () -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        viewModel.refreshDailyBudget()
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshDailyBudget()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    HomeScreenContent(
        isLoading = viewModel.isLoading.value,
        dailyBudget = viewModel.dailyBudget.value,
        monthlyExpenses = viewModel.monthlyExpenses.value,
        categoryExpenses = viewModel.categoryExpenses.value,
        allExpenses = viewModel.allExpenses.value,
        onNotificationsClick = onNotificationsClick
    )
}

@Composable
private fun HomeScreenContent(
    isLoading: Boolean,
    dailyBudget: Double,
    monthlyExpenses: Double,
    categoryExpenses: Map<String, Double>,
    allExpenses: List<ExpenseWithLabels>,
    onNotificationsClick: () -> Unit = {}
) {
    val expenseSections = rememberExpenseSections(allExpenses)

    if (isLoading) {
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
            .background(Color(0xFFF7F7F2)),
        contentPadding = PaddingValues(bottom = 110.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SpendAntGreen)
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = "Notifications",
                    tint = Color.Black,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(22.dp)
                        .clickable(onClick = onNotificationsClick)
                )
                Text(
                    text = "Home",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SpendAntFontFamily,
                    color = Color.Black,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(14.dp))
            BudgetCard(
                dailyBudget = dailyBudget,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            MonthlyExpensesCard(
                monthlyExpenses = monthlyExpenses,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            CategoryBarChart(
                categoryExpenses = categoryExpenses,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
        }

        expenseSections.forEach { section ->
            item(key = "header_${section.title}") {
                Text(
                    text = section.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SpendAntFontFamily,
                    color = Color.Black,
                    modifier = Modifier.padding(start = 22.dp, top = 16.dp, end = 22.dp, bottom = 10.dp)
                )
            }

            items(
                count = section.expenses.size,
                key = { index -> section.expenses[index].expense.id }
            ) { index ->
                ExpenseListItem(
                    expense = section.expenses[index],
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    val now = 1_710_720_000_000L
    val allExpenses = listOf(
        ExpenseWithLabels(
            expense = ExpenseEntity(
                id = 1,
                userId = 1,
                name = "Lunch",
                amount = 23000.0,
                date = now,
                time = "13:10"
            ),
            labels = listOf(
                LabelEntity(
                    id = 1,
                    name = "Food",
                    iconEmoji = "\uD83C\uDF54",
                    userId = 1
                )
            )
        ),
        ExpenseWithLabels(
            expense = ExpenseEntity(
                id = 2,
                userId = 1,
                name = "Bus to campus",
                amount = 3500.0,
                date = now,
                time = "08:30"
            ),
            labels = listOf(
                LabelEntity(
                    id = 2,
                    name = "Transport",
                    iconEmoji = "\uD83D\uDE8C",
                    userId = 1
                )
            )
        ),
        ExpenseWithLabels(
            expense = ExpenseEntity(
                id = 3,
                userId = 1,
                name = "Google Drive",
                amount = 3500.0,
                date = now - 86_400_000L,
                time = "19:00"
            ),
            labels = listOf(
                LabelEntity(
                    id = 3,
                    name = "Services",
                    iconEmoji = "\uD83D\uDCA1",
                    userId = 1
                )
            )
        )
    )

    HomeScreenContent(
        isLoading = false,
        dailyBudget = 75000.0,
        monthlyExpenses = 320000.0,
        categoryExpenses = mapOf(
            "Food" to 150000.0,
            "Transport" to 70000.0,
            "Services" to 60000.0,
            "Other" to 40000.0
        ),
        allExpenses = allExpenses
    )
}

private data class ExpenseSection(
    val title: String,
    val expenses: List<ExpenseWithLabels>
)

@Composable
private fun rememberExpenseSections(allExpenses: List<ExpenseWithLabels>): List<ExpenseSection> {
    val todayStart = rememberDayStart(System.currentTimeMillis())
    val yesterdayStart = todayStart - ONE_DAY_MILLIS
    val oneWeekAgoStart = todayStart - (ONE_DAY_MILLIS * 7)
    val groupedByDay = LinkedHashMap<Long, MutableList<ExpenseWithLabels>>()

    allExpenses.forEach { expense ->
        val dayStart = startOfDay(expense.expense.date)
        groupedByDay.getOrPut(dayStart) { mutableListOf() }.add(expense)
    }

    return groupedByDay.entries.map { (dayStart, expenses) ->
        val title = when {
            dayStart == todayStart -> "Today"
            dayStart == yesterdayStart -> "Yesterday"
            dayStart >= oneWeekAgoStart -> formatWeekday(dayStart)
            else -> formatFullDate(dayStart)
        }

        ExpenseSection(
            title = title,
            expenses = expenses.sortedByDescending { it.expense.date }
        )
    }
}

@Composable
private fun rememberDayStart(timeMillis: Long): Long = startOfDay(timeMillis)

private fun startOfDay(timeMillis: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = timeMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun formatWeekday(timeMillis: Long): String {
    return SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(timeMillis))
}

private fun formatFullDate(timeMillis: Long): String {
    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timeMillis))
}

private const val ONE_DAY_MILLIS = 86_400_000L
