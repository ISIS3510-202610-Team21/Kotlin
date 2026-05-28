package com.example.spendantt.data.report

import android.content.Context
import com.example.spendantt.data.local.AppDatabase
import com.example.spendantt.data.local.entity.ReportCacheEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate

class ReportRepository(private val appContext: Context) {

    private val db = AppDatabase.getInstance(appContext)
    private val prefs by lazy {
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // CACHE | William | 5 pts | Reporte serializado como JSON en Room con TTL de 24 h; clave userId_start_end evita regenerar el mismo reporte si ya existe uno vigente
    /** Returns the cached report when it exists and is younger than [TTL_MILLIS]. */
    suspend fun getCached(userId: Int, start: LocalDate, end: LocalDate): FinancialReport? = withContext(Dispatchers.IO) {
        val key = buildKey(userId, start, end)
        val row = db.reportCacheDao().getByKey(key) ?: return@withContext null
        val age = System.currentTimeMillis() - row.generatedAt
        if (age > TTL_MILLIS) {
            db.reportCacheDao().delete(key)
            return@withContext null
        }
        runCatching { FinancialReportJson.decode(row.reportJson) }.getOrNull()
    }

    /** Returns the cache row (without parsing) when valid — useful to show "tap to open" hints. */
    suspend fun peekCacheRow(userId: Int, start: LocalDate, end: LocalDate): ReportCacheEntity? = withContext(Dispatchers.IO) {
        val key = buildKey(userId, start, end)
        val row = db.reportCacheDao().getByKey(key) ?: return@withContext null
        if (System.currentTimeMillis() - row.generatedAt > TTL_MILLIS) {
            db.reportCacheDao().delete(key)
            null
        } else row
    }

    /** Recent reports for the user (within the TTL window). */
    suspend fun getRecentReports(userId: Int): List<ReportCacheEntity> = withContext(Dispatchers.IO) {
        val since = System.currentTimeMillis() - TTL_MILLIS
        db.reportCacheDao().deleteOlderThan(since)
        db.reportCacheDao().getRecentForUser(userId, since)
    }

    suspend fun decodeRow(row: ReportCacheEntity): FinancialReport? = withContext(Dispatchers.Default) {
        runCatching { FinancialReportJson.decode(row.reportJson) }.getOrNull()
    }

    // MULTI-THREADING | William | 5 pts | Pipeline IO → Default → IO: Dispatchers.IO para lecturas de BD y escritura de caché, Dispatchers.Default para agregación CPU-intensiva, manteniendo el hilo principal libre
    /**
     * Generates a new report and persists it to the cache.
     * The heavy work runs on [Dispatchers.Default] so the UI thread stays responsive.
     */
    suspend fun generate(
        userId: Int,
        start: LocalDate,
        end: LocalDate,
        displayCurrency: String,
        displayRate: Double,
    ): FinancialReport {
        val (allExpenses, activeGoals, totalIncomeCop) = withContext(Dispatchers.IO) {
            Triple(
                db.expenseDao().getExpensesWithLabels(userId).first(),
                db.goalDao().getActiveGoalsList(userId),
                db.incomeDao().getTotalIncomeSnapshot(userId) ?: 0.0,
            )
        }

        val newCount = bumpUsageCounter(userId)

        val report = withContext(Dispatchers.Default) {
            ReportAggregator.build(
                allExpensesAllTime = allExpenses,
                startDate = start,
                endDate = end,
                displayCurrency = displayCurrency,
                displayRate = displayRate,
                reportsGeneratedCount = newCount,
                activeGoals = activeGoals,
                totalIncomeCop = totalIncomeCop,
            )
        }

        withContext(Dispatchers.IO) {
            val json = FinancialReportJson.encode(report)
            db.reportCacheDao().upsert(
                ReportCacheEntity(
                    cacheKey = buildKey(userId, start, end),
                    userId = userId,
                    startEpochDay = start.toEpochDay(),
                    endEpochDay = end.toEpochDay(),
                    reportJson = json,
                    generatedAt = report.generatedAt,
                )
            )
        }
        return report
    }

    /** Returns the smallest first-expense date for the user, or null. */
    suspend fun getFirstExpenseDate(userId: Int): LocalDate? = withContext(Dispatchers.IO) {
        val expenses = db.expenseDao().getExpensesWithLabels(userId).first()
        expenses.minOfOrNull { it.expense.date }
            ?.let { java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }
    }

    private fun bumpUsageCounter(userId: Int): Int {
        val key = "$USAGE_KEY_PREFIX$userId"
        val current = prefs.getInt(key, 0)
        val updated = current + 1
        prefs.edit().putInt(key, updated).apply()
        return updated
    }

    fun getUsageCount(userId: Int): Int =
        prefs.getInt("$USAGE_KEY_PREFIX$userId", 0)

    companion object {
        private const val PREFS_NAME = "report_prefs"
        private const val USAGE_KEY_PREFIX = "report_usage_"
        private const val TTL_MILLIS = 24L * 60L * 60L * 1000L

        fun buildKey(userId: Int, start: LocalDate, end: LocalDate): String {
            return "${userId}_${start}_$end"
        }
    }
}
