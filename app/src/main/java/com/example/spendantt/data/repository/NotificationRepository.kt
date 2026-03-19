package com.example.spendantt.data.repository

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class AppNotification(
    val id: String,
    val userId: Int,
    val type: String,
    val title: String,
    val body: String,
    val createdAt: Long
)

class NotificationRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getNotifications(userId: Int): List<AppNotification> {
        val raw = prefs.getString(notificationsKey(userId), "[]") ?: "[]"
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    AppNotification(
                        id = item.getString("id"),
                        userId = item.getInt("userId"),
                        type = item.getString("type"),
                        title = item.getString("title"),
                        body = item.getString("body"),
                        createdAt = item.getLong("createdAt")
                    )
                )
            }
        }.sortedByDescending { it.createdAt }
    }

    fun upsertDailyNotification(
        userId: Int,
        type: String,
        dayStart: Long,
        title: String,
        body: String
    ) {
        val notificationId = "${type}_$dayStart"
        val current = getNotifications(userId).toMutableList()
        val updated = AppNotification(
            id = notificationId,
            userId = userId,
            type = type,
            title = title,
            body = body,
            createdAt = System.currentTimeMillis()
        )
        val index = current.indexOfFirst { it.id == notificationId }
        if (index >= 0) {
            current[index] = updated
        } else {
            current.add(updated)
        }
        saveNotifications(userId, current)
    }

    fun upsertNotification(
        userId: Int,
        notificationId: String,
        type: String,
        title: String,
        body: String
    ) {
        val current = getNotifications(userId).toMutableList()
        val updated = AppNotification(
            id = notificationId,
            userId = userId,
            type = type,
            title = title,
            body = body,
            createdAt = System.currentTimeMillis()
        )
        val index = current.indexOfFirst { it.id == notificationId }
        if (index >= 0) {
            current[index] = updated
        } else {
            current.add(updated)
        }
        saveNotifications(userId, current)
    }

    fun removeDailyNotification(
        userId: Int,
        type: String,
        dayStart: Long
    ) {
        val notificationId = "${type}_$dayStart"
        val filtered = getNotifications(userId).filterNot { it.id == notificationId }
        saveNotifications(userId, filtered)
    }

    fun removeNotification(
        userId: Int,
        notificationId: String
    ) {
        val filtered = getNotifications(userId).filterNot { it.id == notificationId }
        saveNotifications(userId, filtered)
    }

    private fun saveNotifications(userId: Int, notifications: List<AppNotification>) {
        val array = JSONArray()
        notifications.forEach { notification ->
            array.put(
                JSONObject().apply {
                    put("id", notification.id)
                    put("userId", notification.userId)
                    put("type", notification.type)
                    put("title", notification.title)
                    put("body", notification.body)
                    put("createdAt", notification.createdAt)
                }
            )
        }
        prefs.edit().putString(notificationsKey(userId), array.toString()).apply()
    }

    private fun notificationsKey(userId: Int): String = "notifications_$userId"

    companion object {
        private const val PREFS_NAME = "notifications_prefs"
    }
}
