package com.example.spendantt.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spendantt.data.local.AppDatabase
import com.example.spendantt.util.AnalyticsHelper
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class AnalyticsViewModel(
    private val context: Context,
    private val userId: Int,
) : ViewModel() {

    private val db = AppDatabase.getInstance(context)

    fun logAllBusinessQuestions() {
        viewModelScope.launch {
            try {
                logBQ2DaysSinceLastExpense()
                // BQ3 (OCR edit rate) se dispara desde NewExpenseViewModel al guardar con OCR
                logBQ4MostActiveHour()
                logBQ5SmallRecurringExpenses()
                logBQ6RegistrationMethods()
                logBQ7SavingsGoalProgress()
                logBQ8BudgetMidpointConsumption()
                logBQ9CategoryHighestGrowth()
            } catch (_: Exception) {
                // Fallo silencioso — analytics no interrumpe el flujo
            }
        }
    }

    // ── BQ2: días desde último gasto ──────────────────────────────────────────
    private suspend fun logBQ2DaysSinceLastExpense() {
        val lastExpenseDate = db.expenseDao().getLastExpenseDate(userId) ?: return
        val days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastExpenseDate).toInt()
        AnalyticsHelper.logDaysSinceLastExpense(context, userId, days)
    }

    // ── BQ4: hora más activa de registro ─────────────────────────────────────
    private suspend fun logBQ4MostActiveHour() {
        val times = db.expenseDao().getAllExpenseTimes(userId)
        if (times.isEmpty()) return

        val hourCounts = mutableMapOf<Int, Int>()
        times.forEach { timeStr ->
            val hour = parseHour(timeStr) ?: return@forEach
            hourCounts[hour] = (hourCounts[hour] ?: 0) + 1
        }

        val mostActiveHour = hourCounts.maxByOrNull { it.value }?.key ?: return
        AnalyticsHelper.logMostActiveHour(context, userId, mostActiveHour)
    }

    private fun parseHour(timeStr: String): Int? = try {
        val isPm = timeStr.uppercase().contains("PM")
        val parts = timeStr.uppercase().replace("AM", "").replace("PM", "").trim().split(":")
        var hour = parts[0].trim().toInt()
        if (isPm && hour != 12) hour += 12
        if (!isPm && hour == 12) hour = 0
        hour
    } catch (_: Exception) { null }

    // ── BQ5: gastos pequeños recurrentes en 3 meses ──────────────────────────
    private suspend fun logBQ5SmallRecurringExpenses() {
        val now = System.currentTimeMillis()
        val threeMonthsAgo = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.MONTH, -3)
        }.timeInMillis

        val recurring = db.expenseDao().getRecurringExpensesInRange(userId, threeMonthsAgo, now)
        val small = recurring.filter { it.amount < 50_000.0 }
        if (small.isEmpty()) return

        AnalyticsHelper.logSmallRecurringExpenses(
            context = context,
            userId = userId,
            count = small.size,
            totalAmount = small.sumOf { it.amount },
        )
    }

    // ── BQ6: método de registro menos usado ──────────────────────────────────
    private suspend fun logBQ6RegistrationMethods() {
        val manual = db.expenseDao().countManualExpenses(userId)
        val ocr = db.expenseDao().countOcrExpenses(userId)
        val googlePay = db.expenseDao().countGooglePayExpenses(userId)
        if (manual + ocr + googlePay == 0) return

        AnalyticsHelper.logExpenseRegistrationMethods(
            context = context,
            userId = userId,
            manualCount = manual,
            ocrCount = ocr,
            googlePayCount = googlePay,
        )
    }

    // ── BQ7: % meta de ahorro mensual alcanzada al día actual ─────────────────
    private suspend fun logBQ7SavingsGoalProgress() {
        val goals = db.goalDao().getActiveGoalsList(userId)
        if (goals.isEmpty()) return

        val totalTarget = goals.sumOf { it.targetAmount }
        val totalCurrent = goals.sumOf { it.currentAmount }
        if (totalTarget == 0.0) return

        val achievedPercent = (totalCurrent / totalTarget * 100 * 10.0).roundToInt() / 10.0
        AnalyticsHelper.logSavingsGoalProgress(
            context = context,
            userId = userId,
            achievedPercent = achievedPercent,
            goalsCount = goals.size,
        )
    }

    // ── BQ8: % presupuesto consumido al punto medio del mes ──────────────────
    private suspend fun logBQ8BudgetMidpointConsumption() {
        val totalIncome = db.incomeDao().getTotalIncomeSnapshot(userId) ?: return
        if (totalIncome == 0.0) return

        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        // Inicio del mes actual
        val startOfMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)

        // Si hoy es después del día 15, evaluamos hasta el cierre del día 15
        val cutoff: Long
        val evaluatedAtMidpoint: Boolean
        if (dayOfMonth <= 15) {
            cutoff = now
            evaluatedAtMidpoint = false
        } else {
            cutoff = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 15)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
            evaluatedAtMidpoint = true
        }

        val totalSpent = db.expenseDao().getTotalSpentInRangeSnapshot(userId, startOfMonth, cutoff) ?: 0.0
        val consumedPercent = (totalSpent / totalIncome * 100 * 10.0).roundToInt() / 10.0

        AnalyticsHelper.logBudgetMidpointConsumption(
            context = context,
            userId = userId,
            consumedPercent = consumedPercent,
            evaluatedAtMidpoint = evaluatedAtMidpoint,
        )
    }

    // ── BQ9: categoría con mayor crecimiento vs mismo período del mes anterior ─
    private suspend fun logBQ9CategoryHighestGrowth() {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)

        // Rango del mes actual: día 1 hasta hoy
        val startCurrent = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Mismo período del mes anterior: día 1 hasta el mismo día del mes pasado
        val startPrev = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val endPrev = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val currentExpenses = db.expenseDao().getExpensesWithLabelsInRangeSnapshot(userId, startCurrent, now)
        val prevExpenses = db.expenseDao().getExpensesWithLabelsInRangeSnapshot(userId, startPrev, endPrev)

        // Agregar por primer label (o "Uncategorized" si no tiene)
        fun aggregateByCategory(expenses: List<com.example.spendantt.data.local.entity.ExpenseWithLabels>): Map<String, Double> =
            expenses.groupBy { it.labels.firstOrNull()?.name ?: "Uncategorized" }
                .mapValues { (_, list) -> list.sumOf { it.expense.amount } }

        val currentByCategory = aggregateByCategory(currentExpenses)
        val prevByCategory = aggregateByCategory(prevExpenses)

        if (currentByCategory.isEmpty()) return

        val topEntry = currentByCategory.entries.mapNotNull { (cat, current) ->
            val prev = prevByCategory[cat] ?: 0.0
            val growth = when {
                prev > 0 -> (current - prev) / prev * 100
                current > 0 -> 100.0
                else -> null
            }
            growth?.let { Triple(cat, current, prev) to it }
        }.maxByOrNull { it.second } ?: return

        val (triple, growthPercent) = topEntry
        val (categoryName, currentAmt, prevAmt) = triple

        AnalyticsHelper.logCategoryHighestGrowth(
            context = context,
            userId = userId,
            categoryName = categoryName,
            currentAmount = currentAmt,
            previousAmount = prevAmt,
            growthPercent = (growthPercent * 10.0).roundToInt() / 10.0,
        )
    }
}
