package com.example.spendantt.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import com.example.spendantt.data.local.AppDatabase
import com.example.spendantt.data.local.entity.ExpenseWithLabels
import com.example.spendantt.data.local.entity.LabelEntity
import com.example.spendantt.data.repository.ExpenseRepository
import com.example.spendantt.data.repository.GoalRepository
import com.example.spendantt.data.repository.LabelRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

class HomeViewModel(context: Context, private val userId: Int) : ViewModel() {

    private val expenseRepository: ExpenseRepository
    private val goalRepository: GoalRepository
    private val labelRepository: LabelRepository

    init {
        val database = AppDatabase.getInstance(context)
        expenseRepository = ExpenseRepository(database.expenseDao(), database.labelDao())
        goalRepository = GoalRepository(database.goalDao())
        labelRepository = LabelRepository(database.labelDao())
    }

    // ── ESTADO ─────────────────────────────────────────────────
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

    // ── INICIALIZACIÓN ─────────────────────────────────────────
    init {
        loadHomeData()
    }

    fun loadHomeData() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                // Cargar gastos del mes
                val monthRange = getMonthDateRange()
                expenseRepository.getExpensesByDateRange(
                    userId,
                    monthRange.first,
                    monthRange.second
                ).collect { expenses ->
                    _allExpenses.value = expenses

                    // Calcular gastos totales del mes
                    _monthlyExpenses.value = expenses.sumOf { it.expense.amount }

                    // Agrupar por categoría y calcular totales
                    val categoryMap = mutableMapOf<String, Double>()
                    expenses.forEach { expenseWithLabels ->
                        expenseWithLabels.labels.forEach { label ->
                            val current = categoryMap[label.name] ?: 0.0
                            categoryMap[label.name] = current + expenseWithLabels.expense.amount
                        }
                    }

                    // ── DATOS MOCK: Eliminar cuando exista backend con datos reales ──
                    if (categoryMap.isEmpty()) {
                        categoryMap["Food"] = 250000.0
                        categoryMap["Transport"] = 150000.0
                        categoryMap["Services"] = 120000.0
                        categoryMap["Other"] = 80000.0
                        _monthlyExpenses.value = 600000.0
                    }
                    // ──────────────────────────────────────────────────────────────────
                    
                    _categoryExpenses.value = categoryMap

                    // Separar gastos de hoy y ayer
                    val today = getTodayRange()
                    val yesterday = getYesterdayRange()

                    val todayReal = expenses.filter { expense ->
                        expense.expense.date in today.first..today.second
                    }

                    val yesterdayReal = expenses.filter { expense ->
                        expense.expense.date in yesterday.first..yesterday.second
                    }

                    // ── DATOS MOCK: Eliminar cuando exista backend con datos reales ──
                    if (expenses.isEmpty()) {
                        val now = System.currentTimeMillis()

                        _todayExpenses.value = listOf(
                            ExpenseWithLabels(
                                expense = com.example.spendantt.data.local.entity.ExpenseEntity(
                                    id = -1,
                                    userId = userId,
                                    name = "Chick & Chips Lunch",
                                    amount = 23000.0,
                                    date = now,
                                    time = "13:10"
                                ),
                                labels = listOf(
                                    com.example.spendantt.data.local.entity.LabelEntity(
                                        id = -1,
                                        name = "Food",
                                        iconEmoji = "\uD83C\uDF54",
                                        userId = userId
                                    )
                                )
                            ),
                            ExpenseWithLabels(
                                expense = com.example.spendantt.data.local.entity.ExpenseEntity(
                                    id = -2,
                                    userId = userId,
                                    name = "TM To the University",
                                    amount = 3500.0,
                                    date = now,
                                    time = "08:30"
                                ),
                                labels = listOf(
                                    com.example.spendantt.data.local.entity.LabelEntity(
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
                                expense = com.example.spendantt.data.local.entity.ExpenseEntity(
                                    id = -3,
                                    userId = userId,
                                    name = "Google Drive Month",
                                    amount = 3500.0,
                                    date = now - 86_400_000L,
                                    time = "19:00"
                                ),
                                labels = listOf(
                                    com.example.spendantt.data.local.entity.LabelEntity(
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
                    // ──────────────────────────────────────────────────────────────────

                    _isLoading.value = false
                }

                // Cargar presupuesto diario (si existe)
                goalRepository.getActiveGoals(userId).collect { goals ->
                    // Buscar goal de tipo "Daily Budget" o similar
                    val dailyGoal = goals.firstOrNull { 
                        it.name.contains("daily", ignoreCase = true) ||
                        it.name.contains("budget", ignoreCase = true)
                    }
                    
                    // ── DATOS MOCK: Eliminar cuando exista backend con datos reales ──
                    _dailyBudget.value = dailyGoal?.targetAmount ?: 25500.0
                    // ──────────────────────────────────────────────────────────────────
                }

            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error loading data"
                _isLoading.value = false
            }
        }
    }

    // ── HELPERS ────────────────────────────────────────────────
    private fun getMonthDateRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val firstDay = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val lastDay = calendar.timeInMillis

        return Pair(firstDay, lastDay)
    }

    private fun getTodayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val start = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val end = calendar.timeInMillis

        return Pair(start, end)
    }

    private fun getYesterdayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val start = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val end = calendar.timeInMillis

        return Pair(start, end)
    }
}