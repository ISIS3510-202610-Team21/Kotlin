package com.example.spendantt.util

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object AnalyticsHelper {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ── BQ1: crash por módulo ─────────────────────────────────────────────────
    fun logModuleCrash(context: Context, moduleName: String, errorMessage: String) {
        android.util.Log.d("ANALYTICS", "Crash en módulo: $moduleName — $errorMessage")
        scope.launch {
            AnalyticsApiClient.post("/events/crash", mapOf(
                "module_name" to moduleName,
                "error_message" to errorMessage.take(100),
            ))
        }
    }

    // ── BQ2: días desde último gasto ──────────────────────────────────────────
    fun logDaysSinceLastExpense(context: Context, userId: Int, days: Int) {
        android.util.Log.d("ANALYTICS", "days_since_last_expense = $days (userId=$userId)")
        scope.launch {
            AnalyticsApiClient.post("/events/days-since-expense", mapOf(
                "user_id" to userId,
                "days" to days,
                "is_inactive" to (days >= 3),
            ))
        }
    }

    // ── BQ3: OCR edit rate ────────────────────────────────────────────────────
    fun logOcrEditRate(context: Context, userId: Int, fieldsPopulated: Int, fieldsEdited: Int) {
        val rate = if (fieldsPopulated > 0) fieldsEdited.toDouble() / fieldsPopulated else 0.0
        android.util.Log.d("ANALYTICS", "ocr_edit_rate = $rate ($fieldsEdited/$fieldsPopulated)")
        scope.launch {
            AnalyticsApiClient.post("/events/ocr-edit-rate", mapOf(
                "user_id" to userId,
                "fields_populated" to fieldsPopulated,
                "fields_edited" to fieldsEdited,
                "edit_rate" to rate,
            ))
        }
    }

    // ── BQ4: hora más activa ──────────────────────────────────────────────────
    fun logMostActiveHour(context: Context, userId: Int, hour: Int) {
        val session = when (hour) {
            in 6..11 -> "morning"
            in 12..17 -> "afternoon"
            in 18..22 -> "evening"
            else -> "night"
        }
        scope.launch {
            AnalyticsApiClient.post("/events/active-hour", mapOf(
                "user_id" to userId,
                "hour" to hour,
                "session" to session,
            ))
        }
    }

    // ── BQ5: gastos pequeños recurrentes ─────────────────────────────────────
    fun logSmallRecurringExpenses(context: Context, userId: Int, count: Int, totalAmount: Double) {
        scope.launch {
            AnalyticsApiClient.post("/events/small-recurring", mapOf(
                "user_id" to userId,
                "count" to count,
                "total_amount" to totalAmount,
            ))
        }
    }

    // ── BQ6: métodos de registro ──────────────────────────────────────────────
    fun logExpenseRegistrationMethods(
        context: Context,
        userId: Int,
        manualCount: Int,
        ocrCount: Int,
        googlePayCount: Int,
    ) {
        val leastUsed = mapOf(
            "manual" to manualCount,
            "ocr" to ocrCount,
            "google_pay" to googlePayCount,
        ).minByOrNull { it.value }?.key ?: "none"

        scope.launch {
            AnalyticsApiClient.post("/events/registration-methods", mapOf(
                "user_id" to userId,
                "manual_count" to manualCount,
                "ocr_count" to ocrCount,
                "google_pay_count" to googlePayCount,
                "least_used_method" to leastUsed,
            ))
        }
    }
}
