package com.example.spendantt.work

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.spendantt.data.local.AppDatabase
import com.example.spendantt.data.repository.ExpenseRepository
import com.example.spendantt.data.repository.NotificationRepository
import com.example.spendantt.data.service.HabitFixerService
import com.example.spendantt.data.service.IcsImportService
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class HabitFixerWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = applicationContext.getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)
            val userId = prefs.getInt(KEY_LAST_USER_ID, -1)
            if (userId == -1) return Result.success()

            val hasFineLocation = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val hasCoarseLocation = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasFineLocation && !hasCoarseLocation) return Result.success()

            val icsImportService = IcsImportService(applicationContext)
            val importedEvents = icsImportService.getImportedEvents(userId)
            val now = System.currentTimeMillis()

            val habitFixerService = HabitFixerService(applicationContext)
            if (!habitFixerService.isWithinFreeTimeSlot(userId, importedEvents, now)) {
                return Result.success()
            }

            val database = AppDatabase.getInstance(applicationContext)
            val expenseRepository = ExpenseRepository(database.expenseDao(), database.labelDao())
            val expenses = expenseRepository.getExpensesWithLabels(userId).first().map { it.expense }
            val patterns = habitFixerService.detectAndStorePatterns(userId, expenses)
            if (patterns.isEmpty()) return Result.success()

            val location = requestCurrentLocation() ?: return Result.success()
            val bucketStart = now / THIRTY_MINUTES_MILLIS
            val notificationRepository = NotificationRepository(applicationContext)

            patterns.filter { pattern ->
                habitFixerService.shouldTriggerPattern(
                    pattern = pattern,
                    currentLatitude = location.latitude,
                    currentLongitude = location.longitude,
                    now = now
                )
            }.forEach { pattern ->
                notificationRepository.upsertNotification(
                    userId = userId,
                    notificationId = "habit_fixer_${pattern.id}_$bucketStart",
                    type = "habit_fixer",
                    title = "Habit fixer alert",
                    body = "You might be about to make a purchase you'll regret. Stay mindful."
                )
            }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private suspend fun requestCurrentLocation(): android.location.Location? {
        val client = LocationServices.getFusedLocationProviderClient(applicationContext)
        return suspendCancellableCoroutine { continuation ->
            client.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { location ->
                if (continuation.isActive) continuation.resume(location)
            }.addOnFailureListener {
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }

    private companion object {
        const val AUTH_PREFS_NAME = "auth_prefs"
        const val KEY_LAST_USER_ID = "last_user_id"
        const val THIRTY_MINUTES_MILLIS = 30L * 60L * 1000L
    }
}
