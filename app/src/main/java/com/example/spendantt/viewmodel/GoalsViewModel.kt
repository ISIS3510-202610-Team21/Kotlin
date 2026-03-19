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
import com.example.spendantt.data.repository.NotificationRepository
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
    val isSelected: Boolean,
    val isCompleted: Boolean
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
    private val notificationRepository = NotificationRepository(context)

    private val _goals = mutableStateOf<List<GoalListItemUiState>>(emptyList())
    val goals: State<List<GoalListItemUiState>> = _goals

    private val _isCreatingGoal = mutableStateOf(false)
    val isCreatingGoal: State<Boolean> = _isCreatingGoal

    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading

    private val _isSavingGoal = mutableStateOf(false)
    val isSavingGoal: State<Boolean> = _isSavingGoal

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
        _isSavingGoal.value = true
        return try {
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
            _isLoading.value = false
            null
        } catch (e: Exception) {
            val message = e.message ?: "Could not save goal."
            _goalLimitError.value = message
            message
        } finally {
            _isSavingGoal.value = false
        }
    }

    fun clearGoalLimitError() {
        _goalLimitError.value = null
    }

    fun deleteGoal(goalId: Int) {
        viewModelScope.launch {
            val goal = repository.getGoalById(goalId) ?: return@launch
            repository.deleteGoal(goal)
            if (preferences.getSelectedGoalId(userId) == goalId) {
                preferences.clearSelectedGoalId(userId)
            }
            notificationRepository.removeNotification(userId, "goal_half_$goalId")
            notificationRepository.removeNotification(userId, "goal_completed_$goalId")
        }
    }

    private fun observeGoals() {
        viewModelScope.launch {
            repository.getGoalsByUser(userId).collectLatest { goals ->
                val now = System.currentTimeMillis()
                val incomes = incomeRepository.getIncomesByUser(userId).first()
                val expenses = expenseRepository.getExpensesWithLabels(userId).first()
                    .filter { it.expense.date <= now }
                val totalDailyIncome = DailyFinanceCalculator.sumDailyIncome(incomes)
                val selectedGoalId = ensureSelectedGoal(goals)
                _goals.value = goals.map { goal ->
                    val dailyAmount = DailyFinanceCalculator.calculateDailyGoal(goal)
                    val currentAmount = DailyFinanceCalculator.calculateDynamicGoalAmount(
                        goal = goal,
                        allGoals = goals,
                        expenses = expenses,
                        totalDailyIncome = totalDailyIncome,
                        selectedGoalId = selectedGoalId,
                        now = now
                    )
                    val isCompleted = currentAmount + 0.0001 >= goal.targetAmount
                    goal.copy(currentAmount = currentAmount).let {
                        GoalListItemUiState(
                            id = it.id,
                            name = it.name,
                            deadline = it.deadline,
                            dailyAmount = dailyAmount,
                            progressPercent = DailyFinanceCalculator.calculateGoalProgressPercent(it),
                            isSelected = it.id == selectedGoalId,
                            isCompleted = isCompleted
                        )
                    }
                }.sortedWith(
                    compareBy<GoalListItemUiState> { it.isCompleted }
                        .thenBy { it.deadline }
                )
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
