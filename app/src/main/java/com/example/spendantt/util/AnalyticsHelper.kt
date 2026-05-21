package com.example.spendantt.util

import android.content.Context
import com.example.spendantt.BuildConfig
import com.mixpanel.android.mpmetrics.MixpanelAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

object AnalyticsHelper {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun mp(context: Context): MixpanelAPI =
        MixpanelAPI.getInstance(context, BuildConfig.MIXPANEL_TOKEN, true)

    // ── BQ1: crash por módulo ─────────────────────────────────────────────────
    fun logModuleCrash(context: Context, moduleName: String, errorMessage: String) {
        android.util.Log.d("ANALYTICS", "Crash en módulo: $moduleName — $errorMessage")
        scope.launch {
            runCatching {
                mp(context).track("module_crash", JSONObject().apply {
                    put("module_name", moduleName)
                    put("error_message", errorMessage.take(100))
                })
            }
        }
    }

    // ── BQ2: días desde último gasto ──────────────────────────────────────────
    fun logDaysSinceLastExpense(context: Context, userId: Int, days: Int) {
        android.util.Log.d("ANALYTICS", "days_since_last_expense = $days (userId=$userId)")
        scope.launch {
            runCatching {
                mp(context).track("days_since_expense", JSONObject().apply {
                    put("user_id", userId)
                    put("days", days)
                    put("is_inactive", days >= 3)
                })
            }
        }
    }

    // ── BQ3: OCR edit rate ────────────────────────────────────────────────────
    fun logOcrEditRate(context: Context, userId: Int, fieldsPopulated: Int, fieldsEdited: Int) {
        val rate = if (fieldsPopulated > 0) fieldsEdited.toDouble() / fieldsPopulated else 0.0
        android.util.Log.d("ANALYTICS", "ocr_edit_rate = $rate ($fieldsEdited/$fieldsPopulated)")
        scope.launch {
            runCatching {
                mp(context).track("ocr_edit_rate", JSONObject().apply {
                    put("user_id", userId)
                    put("fields_populated", fieldsPopulated)
                    put("fields_edited", fieldsEdited)
                    put("edit_rate", rate)
                })
            }
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
            runCatching {
                mp(context).track("active_hour", JSONObject().apply {
                    put("user_id", userId)
                    put("hour", hour)
                    put("session", session)
                })
            }
        }
    }

    // ── BQ5: gastos pequeños recurrentes ─────────────────────────────────────
    fun logSmallRecurringExpenses(context: Context, userId: Int, count: Int, totalAmount: Double) {
        scope.launch {
            runCatching {
                mp(context).track("small_recurring_expenses", JSONObject().apply {
                    put("user_id", userId)
                    put("count", count)
                    put("total_amount", totalAmount)
                })
            }
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
            runCatching {
                mp(context).track("registration_methods", JSONObject().apply {
                    put("user_id", userId)
                    put("manual_count", manualCount)
                    put("ocr_count", ocrCount)
                    put("google_pay_count", googlePayCount)
                    put("least_used_method", leastUsed)
                })
            }
        }
    }
}
