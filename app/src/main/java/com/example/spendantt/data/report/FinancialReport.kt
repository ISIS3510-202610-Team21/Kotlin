package com.example.spendantt.data.report

import java.time.LocalDate

/**
 * Snapshot of a spending report for a given user and date range.
 * All monetary amounts are stored in COP (the canonical currency of the app).
 * Display conversion to the active currency happens at render time.
 */
data class FinancialReport(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val periodLabel: String,                 // "May 3 - May 22, 2026"
    val generatedAt: Long,                   // unix millis
    val displayCurrency: String,             // currency used to format the displayed strings
    val displayRate: Double,                 // copAmount * displayRate -> displayAmount
    val totalSpentCop: Double,
    val dailySpends: List<DailySpend>,
    val topExpenses: List<TopExpense>,
    val topCategories: List<CategoryTotal>,
    val reportsGeneratedCount: Int,
    val mostActiveWeekday: Int?,             // 1=Mon..7=Sun
    val bqInsights: List<String>,
)

data class DailySpend(
    val date: LocalDate,
    val amountCop: Double,
)

data class TopExpense(
    val name: String,
    val amountCop: Double,
    val category: String,
    val date: LocalDate,
)

data class CategoryTotal(
    val label: String,
    val amountCop: Double,
    val count: Int,
)
