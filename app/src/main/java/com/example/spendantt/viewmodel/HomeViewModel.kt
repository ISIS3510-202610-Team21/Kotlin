package com.example.spendantt.viewmodel

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spendantt.data.local.AppDatabase
import com.example.spendantt.data.local.entity.ExpenseEntity
import com.example.spendantt.data.local.entity.ExpenseWithLabels
import com.example.spendantt.data.local.entity.GoalEntity
import com.example.spendantt.data.local.entity.LabelEntity
import com.example.spendantt.data.preferences.GoalPreferences
import com.example.spendantt.data.repository.ExpenseRepository
import com.example.spendantt.data.repository.GoalRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class HomeViewModel(context: Context, private val userId: Int) : ViewModel() {

    private val expenseRepository: ExpenseRepository
    private val goalRepository: GoalRepository
    private val goalPreferences: GoalPreferences

    private val _dailyBudget = mutableStateOf(0.0)
    val dailyBudget: State<Double> = _dailyBudget

    private val _monthlyExpenses = mutableStateOf(0.0)
    val monthlyExpenses: State<Double> = _monthlyExpenses

    private val _categoryExpenses = mutableStateOf<Map<String, Double>>(emptyMap())
    val categoryExpenses: State<Map<String, Double>> = _categoryExpenses

    private val _todayExpenses = mutableStateOf<List<ExpenseWithLabels>>(emptyList())
    val todayExpenses: State<List<ExpenseWithLabels>> = _todayExpenses

    private val _yesterdayExpenses = mutableStateOf<List<ExpenseWithLabels>>(emptyList())
    val yesterdayExpenses: State<List<ExpenseWithLabels>> = _yesterdayExpenses

    private val _allExpenses = mutableStateOf<List<ExpenseWithLabels>>(emptyList())
    val allExpenses: State<List<ExpenseWithLabels>> = _allExpenses

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf("")
    val errorMessage: State<String> = _errorMessage

    init {
        val database = AppDatabase.getInstance(context)
        expenseRepository = ExpenseRepository(database.expenseDao(), database.labelDao())
        goalRepository = GoalRepository(database.goalDao())
        goalPreferences = GoalPreferences(context)
        loadHomeData()
    }

    fun loadHomeData() {
        _isLoading.value = true
        observeExpenses()
        observeGoals()
    }

    fun refreshDailyBudget() {
        viewModelScope.launch {
            try {
                val activeGoals = goalRepository.getActiveGoals(userId).first()
                val selectedGoal = resolveSelectedGoal(activeGoals)
                _dailyBudget.value = selectedGoal?.let(::calculateDailyBudget) ?: 0.0
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error refreshing goals"
            }
        }
    }

    private fun observeExpenses() {
        viewModelScope.launch {
            try {
                val monthRange = getMonthDateRange()
                expenseRepository.getExpensesByDateRange(
                    userId,
                    monthRange.first,
                    monthRange.second
                ).collectLatest { expenses ->
                    _allExpenses.value = expenses
                    _monthlyExpenses.value = expenses.sumOf { it.expense.amount }

                    val categoryMap = mutableMapOf<String, Double>()
                    expenses.forEach { expenseWithLabels ->
                        expenseWithLabels.labels.forEach { label ->
                            categoryMap[label.name] =
                                (categoryMap[label.name] ?: 0.0) + expenseWithLabels.expense.amount
                        }
                    }

                    if (categoryMap.isEmpty()) {
                        categoryMap["Food"] = 250000.0
                        categoryMap["Transport"] = 150000.0
                        categoryMap["Services"] = 120000.0
                        categoryMap["Other"] = 80000.0
                        _monthlyExpenses.value = 600000.0
                    }
                    _categoryExpenses.value = categoryMap

                    val today = getTodayRange()
                    val yesterday = getYesterdayRange()
                    val todayReal = expenses.filter { it.expense.date in today.first..today.second }
                    val yesterdayReal = expenses.filter { it.expense.date in yesterday.first..yesterday.second }

                    if (expenses.isEmpty()) {
                        val now = System.currentTimeMillis()
                        _todayExpenses.value = listOf(
                            ExpenseWithLabels(
                                expense = ExpenseEntity(
                                    id = -1,
                                    userId = userId,
                                    name = "Chick & Chips Lunch",
                                    amount = 23000.0,
                                    date = now,
                                    time = "13:10"
                                ),
                                labels = listOf(
                                    LabelEntity(
                                        id = -1,
                                        name = "Food",
                                        iconEmoji = "\uD83C\uDF54",
                                        userId = userId
                                    )
                                )
                            ),
                            ExpenseWithLabels(
                                expense = ExpenseEntity(
                                    id = -2,
                                    userId = userId,
                                    name = "TM To the University",
                                    amount = 3500.0,
                                    date = now,
                                    time = "08:30"
                                ),
                                labels = listOf(
                                    LabelEntity(
                                        id = -2,
                                        name = "Transport",
                                        iconEmoji = "\uD83D\uDE8C",
                                        userId = userId
                                    )
                                )
                            )
                        )

                        _yesterdayExpenses.value = listOf(
                            ExpenseWithLabels(
                                expense = ExpenseEntity(
                                    id = -3,
                                    userId = userId,
                                    name = "Google Drive Month",
                                    amount = 3500.0,
                                    date = now - 86_400_000L,
                                    time = "19:00"
                                ),
                                labels = listOf(
                                    LabelEntity(
                                        id = -3,
                                        name = "Services",
                                        iconEmoji = "\uD83D\uDCA1",
                                        userId = userId
                                    )
                                )
                            )
                        )
                    } else {
                        _todayExpenses.value = todayReal
                        _yesterdayExpenses.value = yesterdayReal
                    }

                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error loading data"
                _isLoading.value = false
            }
        }
    }

    private fun observeGoals() {
        viewModelScope.launch {
            try {
                goalRepository.getActiveGoals(userId).collectLatest { goals ->
                    val selectedGoal = resolveSelectedGoal(goals)
                    _dailyBudget.value = selectedGoal?.let(::calculateDailyBudget) ?: 0.0
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error loading goals"
            }
        }
    }

    private fun resolveSelectedGoal(goals: List<GoalEntity>): GoalEntity? {
        if (goals.isEmpty()) {
            goalPreferences.clearSelectedGoalId(userId)
            return null
        }

        val selectedGoalId = goalPreferences.getSelectedGoalId(userId)
        val goal = when {
            selectedGoalId != null -> goals.firstOrNull { it.id == selectedGoalId }
            goals.size == 1 -> goals.first()
            else -> goals.first()
        } ?: goals.first()

        goalPreferences.setSelectedGoalId(userId, goal.id)
        return goal
    }

    private fun calculateDailyBudget(goal: GoalEntity): Double {
        val totalDays = TimeUnit.MILLISECONDS
            .toDays(startOfDay(goal.deadline) - startOfDay(goal.createdAt))
            .coerceAtLeast(1L)
        return goal.targetAmount / totalDays.toDouble()
    }

    private fun getMonthDateRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val firstDay = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val lastDay = calendar.timeInMillis

        return Pair(firstDay, lastDay)
    }

    private fun getTodayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val end = calendar.timeInMillis

        return Pair(start, end)
    }

    private fun getYesterdayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val end = calendar.timeInMillis

        return Pair(start, end)
    }

    private fun startOfDay(timeMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
