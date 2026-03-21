package com.example.spendantt.data.service

import android.content.Context
import org.json.JSONArray

class AutoCategorizationUsageTracker(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun recordAutoCategorization(userId: Int, timestamp: Long = System.currentTimeMillis()) {
        val timestamps = getTrackedTimestamps(userId).toMutableList()
        timestamps.add(timestamp)
        saveTrackedTimestamps(userId, timestamps)
    }

    fun countBetween(userId: Int, fromInclusive: Long, toInclusive: Long): Int {
        return getTrackedTimestamps(userId).count { it in fromInclusive..toInclusive }
    }

    private fun getTrackedTimestamps(userId: Int): List<Long> {
        val raw = prefs.getString(key(userId), "[]") ?: "[]"
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                add(array.getLong(index))
            }
        }
    }

    private fun saveTrackedTimestamps(userId: Int, timestamps: List<Long>) {
        val sortedRecent = timestamps.sortedDescending().take(MAX_STORED_EVENTS).sorted()
        val array = JSONArray()
        sortedRecent.forEach(array::put)
        prefs.edit().putString(key(userId), array.toString()).apply()
    }

    private fun key(userId: Int): String = "auto_categorization_usage_$userId"

    private companion object {
        const val PREFS_NAME = "auto_categorization_usage_prefs"
        const val MAX_STORED_EVENTS = 500
    }
}
