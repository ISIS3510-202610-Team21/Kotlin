package com.example.spendantt.work

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.example.spendantt.data.repository.NotificationRepository
import kotlin.math.abs

object NotificationTimeSanitizer {
    private const val TAG = "SpendAntNotifSync"
    private const val PREFS_NAME = "notification_time_guard"
    private const val KEY_LAST_WALL_TIME = "last_wall_time"
    private const val KEY_LAST_ELAPSED_TIME = "last_elapsed_time"
    private const val AUTH_PREFS_NAME = "auth_prefs"
    private const val KEY_LAST_USER_ID = "last_user_id"
    private const val SIGNIFICANT_TIME_JUMP_MILLIS = 5L * 60L * 1000L

    fun sanitizeIfNeeded(context: Context, reason: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val nowWall = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()
        val lastWall = prefs.getLong(KEY_LAST_WALL_TIME, Long.MIN_VALUE)
        val lastElapsed = prefs.getLong(KEY_LAST_ELAPSED_TIME, Long.MIN_VALUE)

        val timeJumpDetected = if (lastWall == Long.MIN_VALUE || lastElapsed == Long.MIN_VALUE) {
            false
        } else {
            val wallDelta = nowWall - lastWall
            val elapsedDelta = nowElapsed - lastElapsed
            wallDelta < 0L || abs(wallDelta - elapsedDelta) > SIGNIFICANT_TIME_JUMP_MILLIS
        }

        prefs.edit()
            .putLong(KEY_LAST_WALL_TIME, nowWall)
            .putLong(KEY_LAST_ELAPSED_TIME, nowElapsed)
            .apply()

        if (!timeJumpDetected) return false

        val authPrefs = context.getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)
        val userId = authPrefs.getInt(KEY_LAST_USER_ID, -1)
        if (userId != -1) {
            NotificationRepository(context).pruneInvalidScheduledNotifications(userId, nowWall)
        }
        Log.d(TAG, "sanitizeIfNeeded reason=$reason timeJumpDetected=true userId=$userId")
        return true
    }
}
