package com.example.spendantt.work

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object NotificationSyncScheduler {
    private const val TAG = "SpendAntNotifSync"
    private const val PERIODIC_WORK_NAME = "notification_sync_work"
    private const val IMMEDIATE_WORK_NAME = "notification_sync_immediate"

    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<NotificationSyncWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
        Log.d(TAG, "schedulePeriodic")
    }

    fun enqueueImmediate(context: Context, reason: String) {
        val request = OneTimeWorkRequestBuilder<NotificationSyncWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
        Log.d(TAG, "enqueueImmediate reason=$reason")
    }
}
