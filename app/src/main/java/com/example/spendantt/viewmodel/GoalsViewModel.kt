package com.example.spendantt.viewmodel

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spendantt.data.local.AppDatabase
import com.example.spendantt.data.local.entity.GoalEntity
import com.example.spendantt.data.preferences.GoalPreferences
import com.example.spendantt.data.repository.GoalRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

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
    private val repository = GoalRepository(AppDatabase.getInstance(context).goalDao())
    private val preferences = GoalPreferences(context)

    private val _goals = mutableStateOf<List<GoalListItemUiState>>(emptyList())
    val goals: State<List<GoalListItemUiState>> = _goals

    private val _isCreatingGoal = mutableStateOf(false)
    val isCreatingGoal: State<Boolean> = _isCreatingGoal

    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading

    init {
        observeGoals()
    }

    fun showCreateGoal() {
        _isCreatingGoal.value = true
    }

    fun showGoalList() {
        _isCreatingGoal.value = false
    }

    fun selectGoal(goalId: Int) {
        preferences.setSelectedGoalId(userId, goalId)
        _goals.value = _goals.value.map { it.copy(isSelected = it.id == goalId) }
    }

    fun saveGoal(name: String, targetAmount: Double, deadline: Long, dailyAmount: Double) {
        viewModelScope.launch {
            val createdAt = startOfTodayMillis()
            val currentAmount = calculateProgressAmount(createdAt, deadline, targetAmount)
            val result = repository.insertGoal(
                GoalEntity(
                    userId = userId,
                    name = name,
                    targetAmount = targetAmount,
                    currentAmount = currentAmount,
                    deadline = deadline,
                    createdAt = createdAt,
                    isCompleted = currentAmount >= targetAmount
                )
            )

            val newGoalId = result.getOrNull()?.toInt() ?: return@launch
            if (_goals.value.isEmpty()) {
                preferences.setSelectedGoalId(userId, newGoalId)
            } else if (preferences.getSelectedGoalId(userId) == null) {
                preferences.setSelectedGoalId(userId, newGoalId)
            }
            _isCreatingGoal.value = false
        }
    }

    private fun observeGoals() {
        viewModelScope.launch {
            repository.getGoalsByUser(userId).collectLatest { goals ->
                val selectedGoalId = ensureSelectedGoal(goals)
                _goals.value = goals.map { goal ->
                    val dailyAmount = calculateDailyAmount(goal)
                    val progressPercent = calculateTimeProgress(goal)
                    goal.copy(
                        currentAmount = calculateProgressAmount(
                            goal.createdAt,
                            goal.deadline,
                            goal.targetAmount
                        )
                    ).let {
                        GoalListItemUiState(
                            id = it.id,
                            name = it.name,
                            deadline = it.deadline,
                            dailyAmount = dailyAmount,
                            progressPercent = progressPercent,
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

    private fun calculateDailyAmount(goal: GoalEntity): Double {
        val totalDays = totalGoalDays(goal.createdAt, goal.deadline)
        return if (totalDays > 0) goal.targetAmount / totalDays else goal.targetAmount
    }

    private fun calculateTimeProgress(goal: GoalEntity): Int {
        val totalDays = totalGoalDays(goal.createdAt, goal.deadline)
        if (totalDays <= 0) return 100
        val elapsedDays = elapsedGoalDays(goal.createdAt, goal.deadline)
        return ((elapsedDays.toDouble() / totalDays.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
    }

    private fun calculateProgressAmount(createdAt: Long, deadline: Long, targetAmount: Double): Double {
        val totalDays = totalGoalDays(createdAt, deadline)
        if (totalDays <= 0) return targetAmount
        val elapsedDays = elapsedGoalDays(createdAt, deadline).coerceAtMost(totalDays)
        return (targetAmount / totalDays) * elapsedDays
    }

    private fun totalGoalDays(createdAt: Long, deadline: Long): Long {
        val diffDays = TimeUnit.MILLISECONDS.toDays(deadline - startOfDayMillis(createdAt))
        return diffDays.coerceAtLeast(1L)
    }

    private fun elapsedGoalDays(createdAt: Long, deadline: Long): Long {
        val today = startOfTodayMillis()
        if (today >= deadline) {
            return totalGoalDays(createdAt, deadline)
        }
        val diffDays = TimeUnit.MILLISECONDS.toDays(today - startOfDayMillis(createdAt))
        return diffDays.coerceAtLeast(0L)
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
