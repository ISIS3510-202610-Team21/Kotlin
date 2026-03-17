package com.example.spendantt.util

import com.example.spendantt.data.local.entity.GoalEntity
import com.example.spendantt.data.local.entity.IncomeEntity
import com.example.spendantt.data.local.entity.IncomeType
import com.example.spendantt.data.local.entity.RecurrenceUnit
import java.util.Calendar
import java.util.concurrent.TimeUnit

object DailyFinanceCalculator {

    fun calculateDailyIncome(income: IncomeEntity): Double {
        val days = when (income.type) {
            IncomeType.JUST_ONCE -> 120
            IncomeType.FREQUENTLY -> recurrenceToDays(
                interval = income.recurrenceInterval ?: 1,
                unit = income.recurrenceUnit ?: RecurrenceUnit.MONTHS
            )
        }
        return income.amount / days.toDouble()
    }

    fun calculateDailyGoal(goal: GoalEntity): Double {
        val totalDays = goalDurationDays(goal.createdAt, goal.deadline)
        return goal.targetAmount / totalDays.toDouble()
    }

    fun calculateGoalProgressPercent(goal: GoalEntity): Int {
        val totalDays = goalDurationDays(goal.createdAt, goal.deadline)
        val elapsedDays = elapsedGoalDays(goal.createdAt, goal.deadline)
        return ((elapsedDays.toDouble() / totalDays.toDouble()) * 100.0)
            .toInt()
            .coerceIn(0, 100)
    }

    fun calculateGoalProgressAmount(goal: GoalEntity): Double {
        val totalDays = goalDurationDays(goal.createdAt, goal.deadline)
        val elapsedDays = elapsedGoalDays(goal.createdAt, goal.deadline).coerceAtMost(totalDays)
        return (goal.targetAmount / totalDays.toDouble()) * elapsedDays.toDouble()
    }

    fun sumDailyIncome(incomes: List<IncomeEntity>): Double {
        return incomes.sumOf(::calculateDailyIncome)
    }

    fun sumDailyGoals(goals: List<GoalEntity>): Double {
        return goals.sumOf(::calculateDailyGoal)
    }

    fun goalDurationDays(createdAt: Long, deadline: Long): Long {
        val diffDays = TimeUnit.MILLISECONDS.toDays(startOfDay(deadline) - startOfDay(createdAt))
        return diffDays.coerceAtLeast(1L)
    }

    private fun elapsedGoalDays(createdAt: Long, deadline: Long): Long {
        val today = startOfDay(System.currentTimeMillis())
        if (today >= startOfDay(deadline)) {
            return goalDurationDays(createdAt, deadline)
        }
        val diffDays = TimeUnit.MILLISECONDS.toDays(today - startOfDay(createdAt))
        return diffDays.coerceAtLeast(0L)
    }

    private fun recurrenceToDays(interval: Int, unit: RecurrenceUnit): Int {
        return when (unit) {
            RecurrenceUnit.DAYS -> interval
            RecurrenceUnit.WEEKS -> interval * 7
            RecurrenceUnit.MONTHS -> interval * 30
        }.coerceAtLeast(1)
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
