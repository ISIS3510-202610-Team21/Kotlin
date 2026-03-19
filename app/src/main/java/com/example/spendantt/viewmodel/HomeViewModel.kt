package com.example.spendantt.viewmodel

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spendantt.data.local.AppDatabase
import com.example.spendantt.data.local.entity.ExpenseEntity
import com.example.spendantt.data.local.entity.ExpenseWithLabels
import com.example.spendantt.data.local.entity.LabelEntity
import com.example.spendantt.data.preferences.GoalPreferences
import com.example.spendantt.data.repository.ExpenseRepository
import com.example.spendantt.data.repository.GoalRepository
import com.example.spendantt.data.repository.IncomeRepository
import com.example.spendantt.util.DailyFinanceCalculator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeViewModel(context: Context, private val userId: Int) : ViewModel() {

    private val expenseRepository: ExpenseRepository
    private val goalRepository: GoalRepository
    private val incomeRepository: IncomeRepository
    private val database: AppDatabase

    private val _dailyBudget = mutableStateOf(0.0)
    val dailyBudget: State<Double> = _dailyBudget

    private val _monthlyBudget = mutableStateOf(0.0)
    val monthlyBudget: State<Double> = _monthlyBudget

    private val _monthlyExpenses = mutableStateOf(0.0)
    val monthlyExpenses: State<Double> = _monthlyExpenses

    private val _categoryExpenses = mutableStateOf<Map<String, Double>>(emptyMap())
    val categoryExpenses: State<Map<String, Double>> = _categoryExpenses

    private val _allExpenses = mutableStateOf<List<ExpenseWithLabels>>(emptyList())
    val allExpenses: State<List<ExpenseWithLabels>> = _allExpenses

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf("")
    val errorMessage: State<String> = _errorMessage

    private val _userName = mutableStateOf("")
    val userName: State<String> = _userName

    init {
        database = AppDatabase.getInstance(context)
        expenseRepository = ExpenseRepository(database.expenseDao(), database.labelDao())
        goalRepository = GoalRepository(database.goalDao())
        incomeRepository = IncomeRepository(database.incomeDao())
        loadHomeData()
        loadUserName()
    }

    private fun loadUserName() {
        viewModelScope.launch {
            try {
                val user = database.userDao().getUserById(userId)
                _userName.value = user?.displayName ?: user?.username ?: "User"
            } catch (e: Exception) {
                _userName.value = "User"
            }
        }
    }

    fun loadHomeData() {
        _isLoading.value = true
        observeExpenses()
        observeGoals()
        observeIncomesForBudget()
    }

    fun refreshDailyBudget() {
        viewModelScope.launch {
            try {
                val incomes = incomeRepository.getIncomesByUser(userId).first()
                val activeGoals = goalRepository.getActiveGoals(userId).first()
                _dailyBudget.value = (
                    DailyFinanceCalculator.sumDailyIncome(incomes) -
                        DailyFinanceCalculator.sumDailyGoals(activeGoals)
                    ).coerceAtLeast(0.0)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error refreshing goals"
            }
        }
    }

    private fun observeExpenses() {
        viewModelScope.launch {
            try {
                expenseRepository.getExpensesWithLabels(userId).collectLatest { expenses ->
                    val sortedExpenses = expenses.sortedByDescending { it.expense.date }
                    _allExpenses.value = sortedExpenses

                    val monthRange = getMonthDateRange()
                    val monthlyExpenses = sortedExpenses.filter {
                        it.expense.date in monthRange.first..monthRange.second
                    }
                    _monthlyExpenses.value = monthlyExpenses.sumOf { it.expense.amount }

                    val categoryMap = mutableMapOf<String, Double>()
                    monthlyExpenses.forEach { expenseWithLabels ->
                        expenseWithLabels.labels.forEach { label ->
                            categoryMap[label.name] =
                                (categoryMap[label.name] ?: 0.0) + expenseWithLabels.expense.amount
                        }
                    }

                    _categoryExpenses.value = categoryMap

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
                    val incomes = incomeRepository.getIncomesByUser(userId).first()
                    val dailyIncome = DailyFinanceCalculator.sumDailyIncome(incomes)
                    val dailyGoals = DailyFinanceCalculator.sumDailyGoals(goals)
                    _dailyBudget.value = (dailyIncome - dailyGoals).coerceAtLeast(0.0)
                    _monthlyBudget.value = (dailyIncome * 30).coerceAtLeast(0.0)
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error loading goals"
            }
        }
    }

    private fun observeIncomesForBudget() {
        viewModelScope.launch {
            try {
                incomeRepository.getIncomesByUser(userId).collectLatest { incomes ->
                    val activeGoals = goalRepository.getActiveGoals(userId).first()
                    _dailyBudget.value = (
                        DailyFinanceCalculator.sumDailyIncome(incomes) -
                            DailyFinanceCalculator.sumDailyGoals(activeGoals)
                        ).coerceAtLeast(0.0)
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error loading incomes"
            }
        }
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
}
