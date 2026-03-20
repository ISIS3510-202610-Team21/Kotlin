package com.example.spendantt.util

import com.example.spendantt.data.local.entity.ExpenseWithLabels
import com.example.spendantt.data.repository.DailyExpenseTotal
import kotlin.math.pow
import kotlin.math.sqrt

data class SpendingAnomalyStats(
    val mean: Double,
    val standardDeviation: Double
)

object SpendingAnomalyCalculator {
    fun sumExpensesForDay(
        expenses: List<ExpenseWithLabels>,
        dayStart: Long,
        dayEndExclusive: Long
    ): Double {
        return expenses
            .filter { it.expense.date >= dayStart && it.expense.date < dayEndExclusive }
            .sumOf { it.expense.amount }
    }

    fun calculateStats(history: List<DailyExpenseTotal>): SpendingAnomalyStats? {
        if (history.size < 5) return null

        val values = history.take(5).map { it.totalExpense }
        val mean = values.average()
        val variance = values.map { (it - mean).pow(2) }.average()
        return SpendingAnomalyStats(
            mean = mean,
            standardDeviation = sqrt(variance)
        )
    }

    fun isAnomalous(todayExpense: Double, stats: SpendingAnomalyStats): Boolean {
        return todayExpense > stats.mean + 2 * stats.standardDeviation
    }
}
