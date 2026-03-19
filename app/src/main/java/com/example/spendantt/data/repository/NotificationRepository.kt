package com.example.spendantt.data.repository

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.spendantt.R
import org.json.JSONArray
import org.json.JSONObject

data class AppNotification(
    val id: String,
    val userId: Int,
    val type: String,
    val title: String,
    val body: String,
    val createdAt: Long,
    val isRead: Boolean
)

class NotificationRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        createNotificationChannel()
    }

    fun getNotifications(userId: Int): List<AppNotification> {
        val raw = prefs.getString(notificationsKey(userId), "[]") ?: "[]"
        val array = JSONArray(raw)
        val notifications = buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    AppNotification(
                        id = item.getString("id"),
                        userId = item.getInt("userId"),
                        type = item.getString("type"),
                        title = item.getString("title"),
                        body = item.getString("body"),
                        createdAt = item.getLong("createdAt"),
                        isRead = item.optBoolean("isRead", false)
                    )
                )
            }
        }.sortedByDescending { it.createdAt }
        Log.d(TAG, "getNotifications userId=$userId count=${notifications.size} unread=${notifications.count { !it.isRead }} ids=${notifications.joinToString { "${it.id}:${it.isRead}" }}")
        return notifications
    }

    fun upsertDailyNotification(
        userId: Int,
        type: String,
        dayStart: Long,
        title: String,
        body: String
    ) {
        val notificationId = "${type}_$dayStart"
        upsertNotificationInternal(
            userId = userId,
            notificationId = notificationId,
            type = type,
            title = title,
            body = body
        )
    }

    fun upsertNotification(
        userId: Int,
        notificationId: String,
        type: String,
        title: String,
        body: String
    ) {
        upsertNotificationInternal(
            userId = userId,
            notificationId = notificationId,
            type = type,
            title = title,
            body = body
        )
    }

    private fun upsertNotificationInternal(
        userId: Int,
        notificationId: String,
        type: String,
        title: String,
        body: String
    ) {
        val current = getNotifications(userId).toMutableList()
        val index = current.indexOfFirst { it.id == notificationId }
        val existing = current.getOrNull(index)
        val alreadySentToSystem = wasSentToSystem(userId, notificationId)
        if (existing != null) {
            Log.d(TAG, "upsert skip existing userId=$userId id=$notificationId type=$type isRead=${existing.isRead} alreadySent=$alreadySentToSystem")
            return
        }

        val updated = AppNotification(
            id = notificationId,
            userId = userId,
            type = type,
            title = title,
            body = body,
            createdAt = System.currentTimeMillis(),
            isRead = false
        )

        if (index >= 0) {
            current[index] = updated
        } else {
            current.add(updated)
        }
        saveNotifications(userId, current)
        Log.d(TAG, "upsert stored userId=$userId id=$notificationId type=$type alreadySent=$alreadySentToSystem unread=${current.count { !it.isRead }}")
        if (!alreadySentToSystem) {
            postSystemNotification(updated)
            markAsSentToSystem(userId, notificationId)
            Log.d(TAG, "postSystemNotification userId=$userId id=$notificationId type=$type")
        } else {
            Log.d(TAG, "skip system repost userId=$userId id=$notificationId type=$type")
        }
    }

    fun removeDailyNotification(
        userId: Int,
        type: String,
        dayStart: Long
    ) {
        val notificationId = "${type}_$dayStart"
        val filtered = getNotifications(userId).filterNot { it.id == notificationId }
        saveNotifications(userId, filtered)
        NotificationManagerCompat.from(appContext).cancel(notificationId.hashCode())
        removeSentToSystem(userId, listOf(notificationId))
        Log.d(TAG, "removeDailyNotification userId=$userId id=$notificationId remaining=${filtered.size}")
    }

    fun removeNotification(
        userId: Int,
        notificationId: String
    ) {
        val filtered = getNotifications(userId).filterNot { it.id == notificationId }
        saveNotifications(userId, filtered)
        NotificationManagerCompat.from(appContext).cancel(notificationId.hashCode())
        removeSentToSystem(userId, listOf(notificationId))
        Log.d(TAG, "removeNotification userId=$userId id=$notificationId remaining=${filtered.size}")
    }

    fun getUnreadCount(userId: Int): Int {
        val seenIds = getSeenNotificationIds(userId)
        val unread = getNotifications(userId).count { it.id !in seenIds }
        Log.d(TAG, "getUnreadCount userId=$userId unread=$unread")
        return unread
    }

    fun markAllAsRead(userId: Int) {
        val currentNotifications = getNotifications(userId)
        val updated = currentNotifications.map { it.copy(isRead = true) }
        saveNotifications(userId, updated)
        val updatedSeenIds = getSeenNotificationIds(userId).toMutableSet()
        updatedSeenIds.addAll(currentNotifications.map { it.id })
        saveSeenNotificationIds(userId, updatedSeenIds)
        currentNotifications.forEach { notification ->
            NotificationManagerCompat.from(appContext).cancel(notification.id.hashCode())
        }
        Log.d(TAG, "markAllAsRead userId=$userId marked=${updated.size}")
    }

    fun pruneFutureDailyNotifications(userId: Int, maxAllowedDayStart: Long) {
        val notifications = getNotifications(userId)
        val removedIds = notifications.filter { notification ->
            val isDailyType = notification.type == "budget_exceeded" || notification.type == "goal_adjustment"
            val dayStart = notification.id.substringAfterLast('_').toLongOrNull()
            isDailyType && dayStart != null && dayStart > maxAllowedDayStart
        }.map { it.id }
        val filtered = notifications.filterNot { it.id in removedIds }
        removedIds.forEach { id -> NotificationManagerCompat.from(appContext).cancel(id.hashCode()) }
        saveNotifications(userId, filtered)
        removeSentToSystem(userId, removedIds)
        Log.d(TAG, "pruneFutureDailyNotifications userId=$userId removed=${removedIds.joinToString()}")
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
                    put("isRead", notification.isRead)
                }
            )
        }
        prefs.edit().putString(notificationsKey(userId), array.toString()).commit()
    }

    private fun postSystemNotification(notification: AppNotification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val launchIntent = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            notification.id.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val systemNotification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.spendant_logo)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(appContext)
            .notify(notification.id.hashCode(), systemNotification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SpendAnt Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Budget, goal, and activity alerts"
        }
        manager.createNotificationChannel(channel)
    }

    private fun notificationsKey(userId: Int): String = "notifications_$userId"
    private fun sentSystemKey(userId: Int): String = "sent_system_$userId"
    private fun seenNotificationsKey(userId: Int): String = "seen_notifications_$userId"

    private fun wasSentToSystem(userId: Int, notificationId: String): Boolean {
        return getSentToSystemIds(userId).contains(notificationId)
    }

    private fun markAsSentToSystem(userId: Int, notificationId: String) {
        val updated = getSentToSystemIds(userId).toMutableSet()
        updated.add(notificationId)
        saveSentToSystemIds(userId, updated)
    }

    private fun removeSentToSystem(userId: Int, notificationIds: List<String>) {
        if (notificationIds.isEmpty()) return
        val updated = getSentToSystemIds(userId).toMutableSet()
        updated.removeAll(notificationIds.toSet())
        saveSentToSystemIds(userId, updated)
    }

    private fun getSentToSystemIds(userId: Int): Set<String> {
        return prefs.getStringSet(sentSystemKey(userId), emptySet()) ?: emptySet()
    }

    private fun saveSentToSystemIds(userId: Int, ids: Set<String>) {
        prefs.edit().putStringSet(sentSystemKey(userId), ids).commit()
    }

    private fun getSeenNotificationIds(userId: Int): Set<String> {
        return prefs.getStringSet(seenNotificationsKey(userId), emptySet()) ?: emptySet()
    }

    private fun saveSeenNotificationIds(userId: Int, ids: Set<String>) {
        prefs.edit().putStringSet(seenNotificationsKey(userId), ids).commit()
    }

    companion object {
        private const val TAG = "SpendAntNotif"
        private const val PREFS_NAME = "notifications_prefs"
        private const val CHANNEL_ID = "spendant_notifications"
    }
}
