package com.example.spendantt.viewmodel

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spendantt.data.local.AppDatabase
import com.example.spendantt.data.local.entity.ExpenseWithLabels
import com.example.spendantt.data.local.entity.GoalEntity
import com.example.spendantt.data.preferences.GoalPreferences
import com.example.spendantt.data.repository.ExpenseRepository
import com.example.spendantt.data.repository.GoalRepository
import com.example.spendantt.data.repository.IncomeRepository
import com.example.spendantt.data.repository.NotificationRepository
import com.example.spendantt.util.DailyFinanceCalculator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.Calendar

class HomeViewModel(context: Context, private val userId: Int) : ViewModel() {

    private val expenseRepository: ExpenseRepository
    private val goalRepository: GoalRepository
    private val incomeRepository: IncomeRepository
    private val notificationRepository: NotificationRepository
    private val goalPreferences: GoalPreferences
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
        notificationRepository = NotificationRepository(context)
        goalPreferences = GoalPreferences(context)
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
        observeIncomes()
    }

    fun refreshDailyBudget() {
        viewModelScope.launch {
            try {
                val latestExpenses = expenseRepository.getExpensesWithLabels(userId).first()
                    .sortedByDescending { it.expense.date }
                _allExpenses.value = latestExpenses
                _monthlyExpenses.value = DailyFinanceCalculator.calculateCurrentMonthExpenses(latestExpenses)
                _categoryExpenses.value = calculateCurrentMonthCategoryMap(latestExpenses)
                recalculateFinancialState(latestExpenses)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error refreshing home data"
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

                    if (categoryMap.isEmpty()) {
                        categoryMap["Food"] = 250000.0
                        categoryMap["Transport"] = 150000.0
                        categoryMap["Services"] = 120000.0
                        categoryMap["Other"] = 80000.0
                        _monthlyExpenses.value = 600000.0
                    }
                    _categoryExpenses.value = categoryMap

                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error loading expenses"
                _isLoading.value = false
            }
        }
    }

    private fun observeGoals() {
        viewModelScope.launch {
            try {
                goalRepository.getActiveGoals(userId).collectLatest { goals ->
                    val incomes = incomeRepository.getIncomesByUser(userId).first()
                    _dailyBudget.value = (
                        DailyFinanceCalculator.sumDailyIncome(incomes) -
                            DailyFinanceCalculator.sumDailyGoals(goals)
                        ).coerceAtLeast(0.0)
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error loading goals"
            }
        }
    }

    private fun observeIncomes() {
        viewModelScope.launch {
            try {
                incomeRepository.getIncomesByUser(userId).collectLatest {
                    recalculateFinancialState(_allExpenses.value)
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error loading incomes"
            }
        }
    }

    private suspend fun recalculateFinancialState(expenses: List<ExpenseWithLabels>) {
        try {
            val incomes = incomeRepository.getIncomesByUser(userId).first()
            val activeGoals = goalRepository.getActiveGoals(userId).first()
            val totalDailyIncome = DailyFinanceCalculator.sumDailyIncome(incomes)
            val todayExpenses = DailyFinanceCalculator.calculateTodayExpenses(expenses)

            _dailyBudget.value = DailyFinanceCalculator.calculateRemainingDailyBudget(
                totalDailyIncome = totalDailyIncome,
                activeGoals = activeGoals,
                todayExpenses = todayExpenses
            ).coerceAtLeast(0.0)

            updateNotifications(
                activeGoals = activeGoals,
                totalDailyIncome = totalDailyIncome,
                todayExpenses = todayExpenses
            )
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Error recalculating home data"
        }
    }

    private fun calculateCurrentMonthCategoryMap(expenses: List<ExpenseWithLabels>): Map<String, Double> {
        val monthRange = getMonthDateRange()
        val categoryMap = mutableMapOf<String, Double>()

        expenses
            .filter { it.expense.date in monthRange.first..monthRange.second }
            .forEach { expenseWithLabels ->
                expenseWithLabels.labels.forEach { label ->
                    categoryMap[label.name] =
                        (categoryMap[label.name] ?: 0.0) + expenseWithLabels.expense.amount
                }
            }

        return categoryMap
    }

    private fun updateNotifications(
        activeGoals: List<GoalEntity>,
        totalDailyIncome: Double,
        todayExpenses: Double
    ) {
        val todayKey = startOfTodayMillis()
        val currency = DecimalFormat("#,##0").format(todayExpenses)

        if (DailyFinanceCalculator.isDailyBudgetExceeded(totalDailyIncome, activeGoals, todayExpenses)) {
            notificationRepository.upsertDailyNotification(
                userId = userId,
                type = "budget_exceeded",
                dayStart = todayKey,
                title = "Daily budget reached zero",
                body = "Today's remaining budget after goals is now COP 0. Today's expenses reached COP $currency."
            )
        } else {
            notificationRepository.removeDailyNotification(
                userId = userId,
                type = "budget_exceeded",
                dayStart = todayKey
            )
        }

        val canCoverGoalTargets = DailyFinanceCalculator.canCoverTodayGoalTargets(
            totalDailyIncome = totalDailyIncome,
            todayExpenses = todayExpenses,
            activeGoals = activeGoals
        )

        if (!canCoverGoalTargets && activeGoals.isNotEmpty()) {
            val shortfall = DailyFinanceCalculator.calculateGoalShortfallAmount(
                totalDailyIncome = totalDailyIncome,
                todayExpenses = todayExpenses,
                activeGoals = activeGoals
            )
            val shortfallText = DecimalFormat("#,##0").format(shortfall)
            notificationRepository.upsertDailyNotification(
                userId = userId,
                type = "goal_adjustment",
                dayStart = todayKey,
                title = "Today's goal target was not met",
                body = "Today's income after expenses is short by COP $shortfallText for your goals. Consider saving more tomorrow or moving a deadline to a later date."
            )
        } else {
            notificationRepository.removeDailyNotification(
                userId = userId,
                type = "goal_adjustment",
                dayStart = todayKey
            )
        }

        activeGoals.forEach { goal ->
            val selectedGoalId = goalPreferences.getSelectedGoalId(userId)
            val currentAmount = DailyFinanceCalculator.calculateDynamicGoalAmount(
                goal = goal,
                allGoals = activeGoals,
                expenses = _allExpenses.value,
                totalDailyIncome = totalDailyIncome,
                selectedGoalId = selectedGoalId
            )
            val notificationId = "goal_completed_${goal.id}"
            if (currentAmount + 0.0001 >= goal.targetAmount) {
                notificationRepository.upsertNotification(
                    userId = userId,
                    notificationId = notificationId,
                    type = "goal_completed",
                    title = "Goal completed",
                    body = "You completed your goal \"${goal.name}\"."
                )
            } else {
                notificationRepository.removeNotification(
                    userId = userId,
                    notificationId = notificationId
                )
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

    private fun startOfTodayMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
