package com.example.spendantt.util

import com.example.spendantt.data.local.entity.GoalEntity
import com.example.spendantt.data.local.entity.ExpenseWithLabels
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

    fun calculateDailySavingsNeeded(goal: GoalEntity, now: Long = System.currentTimeMillis()): Double {
        val remaining = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
        val daysLeft = TimeUnit.MILLISECONDS
            .toDays(startOfDay(goal.deadline) - startOfDay(now))
            .coerceAtLeast(0L)

        return if (daysLeft > 0L) remaining / daysLeft.toDouble() else remaining
    }

    fun calculateGoalProgressPercent(goal: GoalEntity): Int {
        return if (goal.targetAmount <= 0.0) 0
        else ((goal.currentAmount / goal.targetAmount) * 100.0).toInt().coerceIn(0, 100)
    }

    fun calculateGoalProgressAmount(goal: GoalEntity): Double {
        val totalDays = goalDurationDays(goal.createdAt, goal.deadline)
        val elapsedDays = elapsedGoalDays(goal.createdAt, goal.deadline).coerceAtMost(totalDays)
        return (goal.targetAmount / totalDays.toDouble()) * elapsedDays.toDouble()
    }

    fun calculateCurrentMonthExpenses(expenses: List<ExpenseWithLabels>, now: Long = System.currentTimeMillis()): Double {
        val monthRange = monthRange(now)
        return expenses
            .filter { it.expense.date in monthRange.first..monthRange.second && it.expense.date <= now }
            .sumOf { it.expense.amount }
    }

    fun calculateTodayExpenses(expenses: List<ExpenseWithLabels>, now: Long = System.currentTimeMillis()): Double {
        val today = dayRange(now)
        return expenses
            .filter { it.expense.date in today.first..today.second && it.expense.date <= now }
            .sumOf { it.expense.amount }
    }

    fun calculateBaseDailyBudget(totalDailyIncome: Double, activeGoals: List<GoalEntity>): Double {
        return (totalDailyIncome - sumDailyGoals(activeGoals)).coerceAtLeast(0.0)
    }

    fun calculateRemainingDailyBudget(
        totalDailyIncome: Double,
        activeGoals: List<GoalEntity>,
        todayExpenses: Double
    ): Double {
        return (calculateBaseDailyBudget(totalDailyIncome, activeGoals) - todayExpenses).coerceAtLeast(0.0)
    }

    fun isDailyBudgetExceeded(
        totalDailyIncome: Double,
        activeGoals: List<GoalEntity>,
        todayExpenses: Double
    ): Boolean {
        val remainingBudgetAfterGoals =
            calculateBaseDailyBudget(totalDailyIncome, activeGoals) - todayExpenses
        return remainingBudgetAfterGoals <= 0.0001
    }

    fun calculateTodayGoalContributionPerGoal(
        totalDailyIncome: Double,
        todayExpenses: Double,
        activeGoals: List<GoalEntity>
    ): Double {
        if (activeGoals.isEmpty()) return 0.0
        val availableForGoals = (totalDailyIncome - todayExpenses).coerceAtLeast(0.0)
        return availableForGoals / activeGoals.size.toDouble()
    }

    fun canCoverTodayGoalTargets(
        totalDailyIncome: Double,
        todayExpenses: Double,
        activeGoals: List<GoalEntity>
    ): Boolean {
        val availableForGoals = (totalDailyIncome - todayExpenses).coerceAtLeast(0.0)
        return availableForGoals + 0.0001 >= sumDailyGoals(activeGoals)
    }

    fun calculateGoalShortfallAmount(
        totalDailyIncome: Double,
        todayExpenses: Double,
        activeGoals: List<GoalEntity>
    ): Double {
        val availableForGoals = (totalDailyIncome - todayExpenses).coerceAtLeast(0.0)
        return (sumDailyGoals(activeGoals) - availableForGoals).coerceAtLeast(0.0)
    }

    fun calculateGoalContributionsForDay(
        activeGoals: List<GoalEntity>,
        totalDailyIncome: Double,
        expensesForDay: Double,
        selectedGoalId: Int?
    ): Map<Int, Double> {
        if (activeGoals.isEmpty()) return emptyMap()

        val availableForGoals = (totalDailyIncome - expensesForDay).coerceAtLeast(0.0)
        val targetByGoalId = activeGoals.associate { it.id to calculateDailyGoal(it) }
        val totalGoalTargets = targetByGoalId.values.sum()
        val effectiveSelectedGoalId = selectedGoalId
            ?.takeIf { goalId -> activeGoals.any { it.id == goalId } }
            ?: activeGoals.first().id

        val contributions = activeGoals.associate { it.id to 0.0 }.toMutableMap()

        if (availableForGoals + 0.0001 >= totalGoalTargets) {
            targetByGoalId.forEach { (goalId, target) ->
                contributions[goalId] = target
            }
            val leftover = availableForGoals - totalGoalTargets
            if (leftover > 0.0) {
                contributions[effectiveSelectedGoalId] =
                    (contributions[effectiveSelectedGoalId] ?: 0.0) + leftover
            }
            return contributions
        }

        contributions[effectiveSelectedGoalId] = availableForGoals
        return contributions
    }

    fun calculateDynamicGoalAmount(
        goal: GoalEntity,
        allGoals: List<GoalEntity>,
        expenses: List<ExpenseWithLabels>,
        totalDailyIncome: Double,
        selectedGoalId: Int?,
        now: Long = System.currentTimeMillis()
    ): Double {
        val todayStart = startOfDay(now)
        val createdDay = startOfDay(goal.createdAt)
        var dayCursor = createdDay
        var accumulated = 0.0
        val expensesByDay = expenses.groupBy { startOfDay(it.expense.date) }

        // Only count fully completed days. Today's contribution is applied after the day ends.
        while (dayCursor < todayStart && accumulated < goal.targetAmount) {
            val activeGoalsForDay = allGoals.filter { activeGoal ->
                startOfDay(activeGoal.createdAt) <= dayCursor && dayCursor <= startOfDay(activeGoal.deadline)
            }

            if (activeGoalsForDay.any { it.id == goal.id } && activeGoalsForDay.isNotEmpty()) {
                val expensesForDay = expensesByDay[dayCursor].orEmpty().sumOf { it.expense.amount }
                val contributions = calculateGoalContributionsForDay(
                    activeGoals = activeGoalsForDay,
                    totalDailyIncome = totalDailyIncome,
                    expensesForDay = expensesForDay,
                    selectedGoalId = selectedGoalId
                )
                accumulated += contributions[goal.id] ?: 0.0
            }

            dayCursor += TimeUnit.DAYS.toMillis(1)
        }

        return accumulated.coerceAtMost(goal.targetAmount)
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

    private fun monthRange(now: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return start to calendar.timeInMillis
    }

    private fun dayRange(now: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return start to calendar.timeInMillis
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
