package com.example.spendantt.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spendantt.data.local.AppDatabase
import com.example.spendantt.util.AnalyticsHelper
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * AnalyticsViewModel
 *
 * Calcula y loggea todos los eventos de Business Questions a Firebase Analytics.
 * Se instancia una vez al entrar al Home y corre en background.
 *
 * BQ2 — días desde último gasto
 * BQ3 — tasa de gastos sin categorizar
 * BQ4 — hora más activa de registro
 * BQ5 — gastos pequeños recurrentes en 3 meses
 * BQ6 — método de registro menos usado
 */
class AnalyticsViewModel(
    private val context: Context,
    private val userId: Int
) : ViewModel() {

    private val db = AppDatabase.getInstance(context)

    fun logAllBusinessQuestions() {
        viewModelScope.launch {
            try {
                android.util.Log.d("ANALYTICS", "Iniciando log de BQs...")
                logBQ2DaysSinceLastExpense()
                logBQ3UncategorizedRate()
                logBQ4MostActiveHour()
                logBQ5SmallRecurringExpenses()
                logBQ6RegistrationMethods()
            } catch (e: Exception) {
                // Fallo silencioso — analytics no debe interrumpir el flujo
            }
        }
    }

    // ── BQ2: días desde último gasto ──────────────────────────
    private suspend fun logBQ2DaysSinceLastExpense() {
        val lastExpenseDate = db.expenseDao().getLastExpenseDate(userId) ?: return
        val now = System.currentTimeMillis()
        val days = TimeUnit.MILLISECONDS.toDays(now - lastExpenseDate).toInt()
        AnalyticsHelper.logDaysSinceLastExpense(context, days)
    }

    // ── BQ3: tasa de gastos sin categorizar ──────────────────
    private suspend fun logBQ3UncategorizedRate() {
        val uncategorized = db.expenseDao().countUncategorizedExpenses(userId)
        val total = db.expenseDao().countAllExpenses(userId)
        if (total == 0) return
        AnalyticsHelper.logUncategorizedExpenseRate(context, uncategorized, total)
    }

    // ── BQ4: hora más activa de registro ─────────────────────
    private suspend fun logBQ4MostActiveHour() {
        val times = db.expenseDao().getAllExpenseTimes(userId)
        if (times.isEmpty()) return

        // Parsear el string de hora "09:30PM" → hora 21
        val hourCounts = mutableMapOf<Int, Int>()
        times.forEach { timeStr ->
            val hour = parseHourFromTimeString(timeStr)
            if (hour != null) {
                hourCounts[hour] = (hourCounts[hour] ?: 0) + 1
            }
        }

        val mostActiveHour = hourCounts.maxByOrNull { it.value }?.key ?: return
        AnalyticsHelper.logMostActiveHour(context, mostActiveHour)
    }

    private fun parseHourFromTimeString(timeStr: String): Int? {
        return try {
            // Formato: "09:30PM" o "09:30AM"
            val isPm = timeStr.uppercase().contains("PM")
            val parts = timeStr.replace("AM", "").replace("PM", "").replace("am", "").replace("pm", "").trim().split(":")
            var hour = parts[0].trim().toInt()
            if (isPm && hour != 12) hour += 12
            if (!isPm && hour == 12) hour = 0
            hour
        } catch (e: Exception) {
            null
        }
    }

    // ── BQ5: gastos pequeños recurrentes en 3 meses ──────────
    private suspend fun logBQ5SmallRecurringExpenses() {
        val now = System.currentTimeMillis()
        val threeMonthsAgo = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.MONTH, -3)
        }.timeInMillis

        val recurringExpenses = db.expenseDao().getRecurringExpensesInRange(
            userId = userId,
            from = threeMonthsAgo,
            to = now
        )

        // Pequeño = menos de 50,000 COP
        val smallThreshold = 50_000.0
        val smallRecurring = recurringExpenses.filter { it.amount < smallThreshold }

        if (smallRecurring.isEmpty()) return

        AnalyticsHelper.logSmallRecurringExpenses(
            context = context,
            count = smallRecurring.size,
            totalAmount = smallRecurring.sumOf { it.amount }
        )
    }

    // ── BQ6: método de registro menos usado ──────────────────
    private suspend fun logBQ6RegistrationMethods() {
        val manualCount = db.expenseDao().countManualExpenses(userId)
        val ocrCount = db.expenseDao().countOcrExpenses(userId)
        val googlePayCount = db.expenseDao().countGooglePayExpenses(userId)

        if (manualCount + ocrCount + googlePayCount == 0) return

        AnalyticsHelper.logExpenseRegistrationMethods(
            context = context,
            manualCount = manualCount,
            ocrCount = ocrCount,
            googlePayCount = googlePayCount
        )
    }
}