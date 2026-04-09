package com.example.spendantt.util

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent

/**
 * AnalyticsHelper
 *
 * Centraliza todos los eventos de Firebase Analytics del pipeline.
 * Cada método corresponde a una Business Question (BQ).
 *
 * BQ2 — días desde último gasto
 * BQ3 — frecuencia de gastos sin categorizar
 * BQ4 — hora más común de registro
 * BQ5 — gastos pequeños recurrentes en 3 meses
 * BQ6 — método de registro menos usado
 */
object AnalyticsHelper {

    private fun analytics(context: Context) = FirebaseAnalytics.getInstance(context)


    // ── BQ1: crash por módulo ─────────────────────────────────
    fun logModuleCrash(context: Context, moduleName: String, errorMessage: String) {
        android.util.Log.d("ANALYTICS", "Crash en módulo: $moduleName — $errorMessage")
        analytics(context).logEvent("module_crash") {
            param("module_name", moduleName)
            param("error_message", errorMessage.take(100))
        }
    }

    // ── BQ2: días desde último gasto ──────────────────────────
    /**
     * Loggea cuántos días han pasado desde el último gasto registrado.
     * Permite detectar usuarios inactivos y disparar recordatorios.
     * Parámetros: days_since_last_expense (Int)
     */
    fun logDaysSinceLastExpense(context: Context, days: Int) {
        android.util.Log.d("ANALYTICS", "Enviando a Firebase: days_since_last_expense = $days")
        analytics(context).logEvent("days_since_last_expense") {
            param("days", days.toLong())
            param("is_inactive", if (days >= 3) "true" else "false")
        }
    }

    // ── BQ3: frecuencia de gastos sin categorizar ─────────────
    /**
     * Loggea el porcentaje de gastos sin categorizar del usuario.
     * Permite identificar usuarios que no usan bien las labels.
     * Parámetros: uncategorized_count (Int), total_count (Int), percentage (Double)
     */
    fun logUncategorizedExpenseRate(
        context: Context,
        uncategorizedCount: Int,
        totalCount: Int
    ) {
        val percentage = if (totalCount > 0) {
            (uncategorizedCount.toDouble() / totalCount * 100).toLong()
        } else 0L

        analytics(context).logEvent("uncategorized_expense_rate") {
            param("uncategorized_count", uncategorizedCount.toLong())
            param("total_count", totalCount.toLong())
            param("percentage", percentage)
        }
    }

    // ── BQ4: hora más común de registro ──────────────────────
    /**
     * Loggea la hora del día en que el usuario registra más gastos.
     * Permite personalizar recordatorios a la hora más activa.
     * Parámetros: most_active_hour (Int 0-23), session (morning/afternoon/night)
     */
    fun logMostActiveHour(context: Context, hour: Int) {
        val session = when (hour) {
            in 6..11 -> "morning"
            in 12..17 -> "afternoon"
            in 18..22 -> "evening"
            else -> "night"
        }
        analytics(context).logEvent("most_active_hour") {
            param("hour", hour.toLong())
            param("session", session)
        }
    }

    // ── BQ5: gastos pequeños recurrentes en 3 meses ───────────
    /**
     * Loggea cuántos gastos pequeños recurrentes tiene el usuario.
     * Un gasto pequeño = menor al 5% del ingreso diario promedio.
     * Parámetros: count (Int), total_amount (Double)
     */
    fun logSmallRecurringExpenses(
        context: Context,
        count: Int,
        totalAmount: Double
    ) {
        analytics(context).logEvent("small_recurring_expenses") {
            param("count", count.toLong())
            param("total_amount", totalAmount.toLong())
        }
    }

    // ── BQ6: método de registro menos usado ──────────────────
    /**
     * Loggea la distribución de métodos de registro de gastos.
     * manual = escrito a mano, ocr = scan de recibo, google_pay = importado
     * Permite identificar qué funcionalidad tiene menos adopción.
     * Parámetros: manual_count, ocr_count, google_pay_count, least_used_method
     */
    fun logExpenseRegistrationMethods(
        context: Context,
        manualCount: Int,
        ocrCount: Int,
        googlePayCount: Int
    ) {
        val leastUsed = mapOf(
            "manual" to manualCount,
            "ocr" to ocrCount,
            "ocr" to ocrCount,
            "google_pay" to googlePayCount
        ).minByOrNull { it.value }?.key ?: "none"

        analytics(context).logEvent("expense_registration_methods") {
            param("manual_count", manualCount.toLong())
            param("ocr_count", ocrCount.toLong())
            param("google_pay_count", googlePayCount.toLong())
            param("least_used_method", leastUsed)
        }
    }
}