package com.example.spendantt.data.service

import android.content.Context
import com.example.spendantt.data.local.entity.ExpenseEntity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class HabitPattern(
    val id: String,
    val meanMinutesOfDay: Int,
    val timeDeviationMinutes: Int,
    val centerLatitude: Double,
    val centerLongitude: Double,
    val radiusMeters: Double,
    val sampleCount: Int,
    val expenseIds: List<Int>
)

class HabitFixerService(
    context: Context
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun detectAndStorePatterns(userId: Int, expenses: List<ExpenseEntity>): List<HabitPattern> {
        val patterns = detectPatterns(expenses)
        savePatterns(userId, patterns)
        return patterns
    }

    fun getStoredPatterns(userId: Int): List<HabitPattern> {
        val raw = prefs.getString(patternsKey(userId), "[]") ?: "[]"
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    HabitPattern(
                        id = item.getString("id"),
                        meanMinutesOfDay = item.getInt("meanMinutesOfDay"),
                        timeDeviationMinutes = item.getInt("timeDeviationMinutes"),
                        centerLatitude = item.getDouble("centerLatitude"),
                        centerLongitude = item.getDouble("centerLongitude"),
                        radiusMeters = item.getDouble("radiusMeters"),
                        sampleCount = item.getInt("sampleCount"),
                        expenseIds = buildList {
                            val ids = item.getJSONArray("expenseIds")
                            for (i in 0 until ids.length()) {
                                add(ids.getInt(i))
                            }
                        }
                    )
                )
            }
        }
    }

    fun isWithinFreeTimeSlot(userId: Int, importedEvents: List<ImportedCalendarEvent>, now: Long): Boolean {
        if (importedEvents.isEmpty()) return false
        return importedEvents.none { event ->
            val start = event.startAt ?: return@none false
            val end = event.endAt ?: inferEventEnd(start)
            now in start until end
        }
    }

    fun shouldTriggerPattern(
        pattern: HabitPattern,
        currentLatitude: Double,
        currentLongitude: Double,
        now: Long
    ): Boolean {
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val allowedDeviation = max(pattern.timeDeviationMinutes, MIN_TRIGGER_WINDOW_MINUTES)
        val withinTime = circularMinutesDistance(currentMinutes, pattern.meanMinutesOfDay) <= allowedDeviation
        val withinLocation = distanceMeters(
            currentLatitude,
            currentLongitude,
            pattern.centerLatitude,
            pattern.centerLongitude
        ) <= pattern.radiusMeters
        return withinTime && withinLocation
    }

    private fun detectPatterns(expenses: List<ExpenseEntity>): List<HabitPattern> {
        val samples = expenses
            .filter { it.isRecurring && it.latitude != null && it.longitude != null }
            .mapNotNull { expense ->
                val minutesOfDay = extractMinutesOfDay(expense) ?: return@mapNotNull null
                HabitSample(
                    expenseId = expense.id,
                    latitude = expense.latitude ?: return@mapNotNull null,
                    longitude = expense.longitude ?: return@mapNotNull null,
                    minutesOfDay = minutesOfDay
                )
            }

        if (samples.size < 2) return emptyList()

        val patterns = mutableListOf<HabitPattern>()
        val seenSignatures = mutableSetOf<String>()

        samples.forEach { seed ->
            val candidateCluster = samples.filter { sample ->
                distanceMeters(seed.latitude, seed.longitude, sample.latitude, sample.longitude) <= PATTERN_RADIUS_METERS &&
                    circularMinutesDistance(seed.minutesOfDay, sample.minutesOfDay) <= MAX_TIME_SPAN_MINUTES
            }

            if (candidateCluster.size < 2) return@forEach

            val centroidLat = candidateCluster.map { it.latitude }.average()
            val centroidLon = candidateCluster.map { it.longitude }.average()
            val refinedCluster = candidateCluster.filter { sample ->
                distanceMeters(sample.latitude, sample.longitude, centroidLat, centroidLon) <= PATTERN_RADIUS_METERS
            }

            if (refinedCluster.size < 2) return@forEach

            val meanMinutes = circularMeanMinutes(refinedCluster.map { it.minutesOfDay })
            val deviation = circularStandardDeviation(refinedCluster.map { it.minutesOfDay }, meanMinutes)
            if (deviation > MAX_PATTERN_DEVIATION_MINUTES) return@forEach

            val signature = refinedCluster.map { it.expenseId }.sorted().joinToString("_")
            if (!seenSignatures.add(signature)) return@forEach

            patterns.add(
                HabitPattern(
                    id = "habit_${signature.hashCode().toUInt()}",
                    meanMinutesOfDay = meanMinutes,
                    timeDeviationMinutes = max(1, deviation.roundToInt()),
                    centerLatitude = centroidLat,
                    centerLongitude = centroidLon,
                    radiusMeters = PATTERN_RADIUS_METERS,
                    sampleCount = refinedCluster.size,
                    expenseIds = refinedCluster.map { it.expenseId }.sorted()
                )
            )
        }

        return mergeOverlappingPatterns(patterns)
    }

    private fun mergeOverlappingPatterns(patterns: List<HabitPattern>): List<HabitPattern> {
        val merged = mutableListOf<HabitPattern>()

        patterns.sortedByDescending { it.sampleCount }.forEach { candidate ->
            val overlaps = merged.any { existing ->
                distanceMeters(
                    existing.centerLatitude,
                    existing.centerLongitude,
                    candidate.centerLatitude,
                    candidate.centerLongitude
                ) <= OVERLAP_MERGE_DISTANCE_METERS &&
                    circularMinutesDistance(existing.meanMinutesOfDay, candidate.meanMinutesOfDay) <= MAX_PATTERN_DEVIATION_MINUTES
            }
            if (!overlaps) {
                merged.add(candidate)
            }
        }

        return merged
    }

    private fun savePatterns(userId: Int, patterns: List<HabitPattern>) {
        val array = JSONArray()
        patterns.forEach { pattern ->
            array.put(
                JSONObject().apply {
                    put("id", pattern.id)
                    put("meanMinutesOfDay", pattern.meanMinutesOfDay)
                    put("timeDeviationMinutes", pattern.timeDeviationMinutes)
                    put("centerLatitude", pattern.centerLatitude)
                    put("centerLongitude", pattern.centerLongitude)
                    put("radiusMeters", pattern.radiusMeters)
                    put("sampleCount", pattern.sampleCount)
                    put("expenseIds", JSONArray(pattern.expenseIds))
                }
            )
        }

        prefs.edit()
            .putString(patternsKey(userId), array.toString())
            .apply()
    }

    private fun patternsKey(userId: Int): String = "habit_patterns_$userId"

    private fun inferEventEnd(startAt: Long): Long = startAt + DEFAULT_EVENT_DURATION_MILLIS

    private fun extractMinutesOfDay(expense: ExpenseEntity): Int? {
        val cleanTime = expense.time
            .lowercase(Locale.getDefault())
            .replace(".", "")
            .trim()

        val patterns = listOf(
            SimpleDateFormat("hh:mma", Locale.US),
            SimpleDateFormat("h:mma", Locale.US),
            SimpleDateFormat("HH:mm", Locale.US),
            SimpleDateFormat("H:mm", Locale.US)
        )

        patterns.forEach { format ->
            try {
                val date = format.parse(cleanTime) ?: return@forEach
                val calendar = Calendar.getInstance().apply { time = date }
                return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
            } catch (_: Exception) {
                // Try next format.
            }
        }

        return null
    }

    private fun circularMeanMinutes(values: List<Int>): Int {
        val angles = values.map { 2.0 * Math.PI * (it.toDouble() / MINUTES_IN_DAY) }
        val sinMean = angles.map(::sin).average()
        val cosMean = angles.map(::cos).average()
        var angle = atan2(sinMean, cosMean)
        if (angle < 0) angle += 2.0 * Math.PI
        return ((angle / (2.0 * Math.PI)) * MINUTES_IN_DAY).roundToInt() % MINUTES_IN_DAY
    }

    private fun circularStandardDeviation(values: List<Int>, meanMinutes: Int): Double {
        val squaredDistances = values.map { circularMinutesDistance(it, meanMinutes).toDouble().pow(2) }
        return sqrt(squaredDistances.average())
    }

    private fun circularMinutesDistance(first: Int, second: Int): Int {
        val diff = abs(first - second)
        return min(diff, MINUTES_IN_DAY - diff)
    }

    private fun distanceMeters(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double
    ): Double {
        val earthRadius = 6371000.0
        val dLat = Math.toRadians(endLat - startLat)
        val dLon = Math.toRadians(endLon - startLon)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(startLat)) * cos(Math.toRadians(endLat)) * sin(dLon / 2).pow(2)
        return 2 * earthRadius * atan2(sqrt(a), sqrt(1 - a))
    }

    private data class HabitSample(
        val expenseId: Int,
        val latitude: Double,
        val longitude: Double,
        val minutesOfDay: Int
    )

    private companion object {
        const val PREFS_NAME = "habit_fixer_prefs"
        const val PATTERN_RADIUS_METERS = 200.0
        const val OVERLAP_MERGE_DISTANCE_METERS = 120.0
        const val MAX_PATTERN_DEVIATION_MINUTES = 60.0
        const val MAX_TIME_SPAN_MINUTES = 120
        const val MINUTES_IN_DAY = 24 * 60
        const val MIN_TRIGGER_WINDOW_MINUTES = 15
        const val DEFAULT_EVENT_DURATION_MILLIS = 60L * 60L * 1000L
    }
}
