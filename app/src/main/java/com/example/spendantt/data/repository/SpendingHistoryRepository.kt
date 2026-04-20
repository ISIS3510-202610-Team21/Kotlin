package com.example.spendantt.data.repository

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class DailyExpenseTotal(
    val dayStart: Long,
    val totalExpense: Double
)

// LOCAL STORAGE | William | 5pts | Preferences: SharedPreferences serializa historial de gastos diarios como JSONArray, clave por userId; permite análisis de anomalías de gasto sin consultar Room
class SpendingHistoryRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveRecentClosedDays(userId: Int, totals: List<DailyExpenseTotal>) {
        val normalized = totals
            .distinctBy { it.dayStart }
            .sortedByDescending { it.dayStart }
            .take(MAX_STORED_DAYS)

        val array = JSONArray()
        normalized.forEach { total ->
            array.put(
                JSONObject().apply {
                    put("dayStart", total.dayStart)
                    put("totalExpense", total.totalExpense)
                }
            )
        }

        prefs.edit().putString(historyKey(userId), array.toString()).commit()
    }

    fun getRecentClosedDays(userId: Int): List<DailyExpenseTotal> {
        val raw = prefs.getString(historyKey(userId), "[]") ?: "[]"
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    DailyExpenseTotal(
                        dayStart = item.getLong("dayStart"),
                        totalExpense = item.getDouble("totalExpense")
                    )
                )
            }
        }.sortedByDescending { it.dayStart }
    }

    private fun historyKey(userId: Int): String = "spending_history_$userId"

    companion object {
        private const val PREFS_NAME = "spending_history_prefs"
        private const val MAX_STORED_DAYS = 6
    }
}
