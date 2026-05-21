package com.example.spendantt.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spendantt.data.local.AppDatabase
import com.example.spendantt.util.AnalyticsHelper
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

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
}
