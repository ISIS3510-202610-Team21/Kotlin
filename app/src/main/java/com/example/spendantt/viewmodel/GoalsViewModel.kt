package com.example.spendantt.viewmodel

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spendantt.data.local.AppDatabase
import com.example.spendantt.data.local.entity.GoalEntity
import com.example.spendantt.data.preferences.GoalPreferences
import com.example.spendantt.data.repository.ExpenseRepository
import com.example.spendantt.data.repository.GoalRepository
import com.example.spendantt.data.repository.IncomeRepository
import com.example.spendantt.util.DailyFinanceCalculator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

data class GoalListItemUiState(
    val id: Int,
    val name: String,
    val deadline: Long,
    val dailyAmount: Double,
    val progressPercent: Int,
    val isSelected: Boolean
)

class GoalsViewModel(
    context: Context,
    private val userId: Int
) : ViewModel() {
    private val database = AppDatabase.getInstance(context)
    private val expenseRepository = ExpenseRepository(database.expenseDao(), database.labelDao())
    private val repository = GoalRepository(database.goalDao())
    private val incomeRepository = IncomeRepository(database.incomeDao())
    private val preferences = GoalPreferences(context)

    private val _goals = mutableStateOf<List<GoalListItemUiState>>(emptyList())
    val goals: State<List<GoalListItemUiState>> = _goals

    private val _isCreatingGoal = mutableStateOf(false)
    val isCreatingGoal: State<Boolean> = _isCreatingGoal

    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading

    private val _goalLimitError = mutableStateOf<String?>(null)
    val goalLimitError: State<String?> = _goalLimitError

    init {
        observeGoals()
    }

    fun showCreateGoal() {
        _goalLimitError.value = null
        _isCreatingGoal.value = true
    }

    fun showGoalList() {
        _goalLimitError.value = null
        _isCreatingGoal.value = false
    }

    fun selectGoal(goalId: Int) {
        preferences.setSelectedGoalId(userId, goalId)
        _goals.value = _goals.value.map { it.copy(isSelected = it.id == goalId) }
    }

    suspend fun saveGoal(name: String, targetAmount: Double, deadline: Long, dailyAmount: Double): String? {
        val createdAt = startOfTodayMillis()
        val incomes = incomeRepository.getIncomesByUser(userId).first()
        val existingGoals = repository.getGoalsByUser(userId).first()

        val totalDailyIncome = DailyFinanceCalculator.sumDailyIncome(incomes)
        val existingDailyGoals = DailyFinanceCalculator.sumDailyGoals(existingGoals)

        if (totalDailyIncome <= 0.0) {
            val message = "You need at least one income before creating a goal."
            _goalLimitError.value = message
            return message
        }

        if (dailyAmount > totalDailyIncome) {
            val message = "This goal needs more daily savings than your daily income allows."
            _goalLimitError.value = message
            return message
        }

        if (existingDailyGoals + dailyAmount > totalDailyIncome) {
            val message = "Your goals together cannot exceed your total daily income."
            _goalLimitError.value = message
            return message
        }

        val result = repository.insertGoal(
            GoalEntity(
                userId = userId,
                name = name,
                targetAmount = targetAmount,
                currentAmount = 0.0,
                deadline = deadline,
                createdAt = createdAt,
                isCompleted = false
            )
        )

        val newGoalId = result.getOrNull()?.toInt()
        if (newGoalId == null) {
            val message = result.exceptionOrNull()?.message ?: "Could not save goal."
            _goalLimitError.value = message
            return message
        }

        if (_goals.value.isEmpty() || preferences.getSelectedGoalId(userId) == null) {
            preferences.setSelectedGoalId(userId, newGoalId)
        }
        _goalLimitError.value = null
        _isCreatingGoal.value = false
        return null
    }

    fun clearGoalLimitError() {
        _goalLimitError.value = null
    }

    private fun observeGoals() {
        viewModelScope.launch {
            repository.getGoalsByUser(userId).collectLatest { goals ->
                val incomes = incomeRepository.getIncomesByUser(userId).first()
                val expenses = expenseRepository.getExpensesWithLabels(userId).first()
                val totalDailyIncome = DailyFinanceCalculator.sumDailyIncome(incomes)
                val selectedGoalId = ensureSelectedGoal(goals)
                _goals.value = goals.map { goal ->
                    val dailyAmount = DailyFinanceCalculator.calculateDailyGoal(goal)
                    val currentAmount = DailyFinanceCalculator.calculateDynamicGoalAmount(
                        goal = goal,
                        allGoals = goals,
                        expenses = expenses,
                        totalDailyIncome = totalDailyIncome,
                        selectedGoalId = selectedGoalId
                    )
                    goal.copy(currentAmount = currentAmount).let {
                        GoalListItemUiState(
                            id = it.id,
                            name = it.name,
                            deadline = it.deadline,
                            dailyAmount = dailyAmount,
                            progressPercent = DailyFinanceCalculator.calculateGoalProgressPercent(it),
                            isSelected = it.id == selectedGoalId
                        )
                    }
                }
                _isLoading.value = false
            }
        }
    }

    private fun ensureSelectedGoal(goals: List<GoalEntity>): Int? {
        if (goals.isEmpty()) {
            preferences.clearSelectedGoalId(userId)
            return null
        }

        val savedId = preferences.getSelectedGoalId(userId)
        val selectedId = when {
            savedId != null && goals.any { it.id == savedId } -> savedId
            goals.size == 1 -> goals.first().id
            else -> goals.first().id
        }
        preferences.setSelectedGoalId(userId, selectedId)
        return selectedId
    }

    private fun startOfTodayMillis(): Long = startOfDayMillis(System.currentTimeMillis())

    private fun startOfDayMillis(timeMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
