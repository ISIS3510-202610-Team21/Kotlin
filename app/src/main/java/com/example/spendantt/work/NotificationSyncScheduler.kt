package com.example.spendantt.work

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object NotificationSyncScheduler {
    private const val TAG = "SpendAntNotifSync"
    private const val PERIODIC_WORK_NAME = "notification_sync_work"
    private const val IMMEDIATE_WORK_NAME = "notification_sync_immediate"
    private const val HABIT_FIXER_PERIODIC_WORK_NAME = "habit_fixer_periodic_work"
    private const val HABIT_FIXER_IMMEDIATE_WORK_NAME = "habit_fixer_immediate"

    // Workers periódicos solo corren cuando hay red disponible
    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<NotificationSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(networkConstraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )

        val habitRequest = PeriodicWorkRequestBuilder<HabitFixerWorker>(30, TimeUnit.MINUTES)
            .setConstraints(networkConstraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            HABIT_FIXER_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            habitRequest
        )
        Log.d(TAG, "schedulePeriodic constraints=CONNECTED")
    }

    fun enqueueImmediate(context: Context, reason: String) {
        val request = OneTimeWorkRequestBuilder<NotificationSyncWorker>()
            .setConstraints(networkConstraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
        Log.d(TAG, "enqueueImmediate reason=$reason constraints=CONNECTED")
    }

    fun enqueueHabitFixerImmediate(context: Context, reason: String) {
        val request = OneTimeWorkRequestBuilder<HabitFixerWorker>()
            .setConstraints(networkConstraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            HABIT_FIXER_IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
        Log.d(TAG, "enqueueHabitFixerImmediate reason=$reason constraints=CONNECTED")
    }
}
