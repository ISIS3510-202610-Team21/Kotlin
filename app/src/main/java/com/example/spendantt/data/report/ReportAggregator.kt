package com.example.spendantt.data.report

import com.example.spendantt.data.local.entity.ExpenseWithLabels
import com.example.spendantt.data.local.entity.GoalEntity
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Builds a [FinancialReport] from raw expenses. Pure aggregation logic; no I/O.
 *
 * Inputs are taken in COP. The active currency is used only to format the
 * BQ insight strings so they read naturally to the user.
 */
object ReportAggregator {

    private const val SMALL_RECURRING_THRESHOLD_COP = 50_000.0
    private const val MICRO_PURCHASE_THRESHOLD_COP = 5_000.0

    fun build(
        allExpensesAllTime: List<ExpenseWithLabels>,
        startDate: LocalDate,
        endDate: LocalDate,
        displayCurrency: String,
        displayRate: Double,
        reportsGeneratedCount: Int,
        activeGoals: List<GoalEntity> = emptyList(),
        totalIncomeCop: Double = 0.0,
        zoneId: ZoneId = ZoneId.systemDefault(),
        now: Instant = Instant.now(),
    ): FinancialReport {

        val periodExpenses = allExpensesAllTime.filter {
            val d = Instant.ofEpochMilli(it.expense.date).atZone(zoneId).toLocalDate()
            !d.isBefore(startDate) && !d.isAfter(endDate)
        }

        val totalSpentCop = periodExpenses.sumOf { it.expense.amount }

        // Fill every day in the range so the histogram has no gaps.
        val dailyMap = linkedMapOf<LocalDate, Double>()
        var cursor = startDate
        while (!cursor.isAfter(endDate)) {
            dailyMap[cursor] = 0.0
            cursor = cursor.plusDays(1)
        }
        periodExpenses.forEach { ewl ->
            val d = Instant.ofEpochMilli(ewl.expense.date).atZone(zoneId).toLocalDate()
            dailyMap[d] = (dailyMap[d] ?: 0.0) + ewl.expense.amount
        }
        val dailySpends = dailyMap.map { (date, amount) -> DailySpend(date, amount) }

        val topExpenses = periodExpenses
            .sortedByDescending { it.expense.amount }
            .take(5)
            .map { ewl ->
                val d = Instant.ofEpochMilli(ewl.expense.date).atZone(zoneId).toLocalDate()
                TopExpense(
                    name = ewl.expense.name.ifBlank { "Unnamed expense" },
                    amountCop = ewl.expense.amount,
                    category = ewl.labels.firstOrNull()?.name ?: "Other",
                    date = d,
                )
            }

        val byCategory = linkedMapOf<String, Pair<Double, Int>>()
        periodExpenses.forEach { ewl ->
            val cat = ewl.labels.firstOrNull()?.name ?: "Other"
            val current = byCategory[cat] ?: (0.0 to 0)
            byCategory[cat] = (current.first + ewl.expense.amount) to (current.second + 1)
        }
        val topCategories = byCategory.entries
            .sortedByDescending { it.value.first }
            .take(5)
            .map { (label, agg) -> CategoryTotal(label, agg.first, agg.second) }

        val weekdayCounts = IntArray(7)
        periodExpenses.forEach { ewl ->
            val d = Instant.ofEpochMilli(ewl.expense.date).atZone(zoneId).toLocalDate()
            weekdayCounts[d.dayOfWeek.value - 1] += 1
        }
        val mostActiveWeekday = weekdayCounts.indices
            .maxByOrNull { weekdayCounts[it] }
            ?.let { if (weekdayCounts[it] > 0) it + 1 else null }

        val bqInsights = buildBqInsights(
            allExpensesAllTime = allExpensesAllTime,
            periodExpenses = periodExpenses,
            startDate = startDate,
            endDate = endDate,
            displayCurrency = displayCurrency,
            displayRate = displayRate,
            reportsGeneratedCount = reportsGeneratedCount,
            activeGoals = activeGoals,
            totalIncomeCop = totalIncomeCop,
            zoneId = zoneId,
            now = now,
        )

        return FinancialReport(
            startDate = startDate,
            endDate = endDate,
            periodLabel = formatPeriodLabel(startDate, endDate),
            generatedAt = now.toEpochMilli(),
            displayCurrency = displayCurrency,
            displayRate = displayRate,
            totalSpentCop = totalSpentCop,
            dailySpends = dailySpends,
            topExpenses = topExpenses,
            topCategories = topCategories,
            reportsGeneratedCount = reportsGeneratedCount,
            mostActiveWeekday = mostActiveWeekday,
            bqInsights = bqInsights,
        )
    }

    private fun buildBqInsights(
        allExpensesAllTime: List<ExpenseWithLabels>,
        periodExpenses: List<ExpenseWithLabels>,
        startDate: LocalDate,
        endDate: LocalDate,
        displayCurrency: String,
        displayRate: Double,
        reportsGeneratedCount: Int,
        activeGoals: List<GoalEntity>,
        totalIncomeCop: Double,
        zoneId: ZoneId,
        now: Instant,
    ): List<String> {
        val insights = mutableListOf<String>()
        val today = now.atZone(zoneId).toLocalDate()

        // 1) Days since last expense (all-time)
        val lastExpenseMillis = allExpensesAllTime.maxOfOrNull { it.expense.date }
        if (lastExpenseMillis != null) {
            val lastDate = Instant.ofEpochMilli(lastExpenseMillis).atZone(zoneId).toLocalDate()
            val days = (java.time.temporal.ChronoUnit.DAYS.between(lastDate, today).toInt() + 1).coerceAtLeast(1)
            insights.add(
                when (days) {
                    1 -> "You registered an expense today"
                    else -> "$days days since your last registered expense"
                }
            )
        }

        // 2) OCR usage rate (period)
        val ocrCount = periodExpenses.count { it.expense.source.name == "OCR" }
        val totalCount = periodExpenses.size
        if (totalCount > 0) {
            val pct = ((ocrCount.toDouble() / totalCount.toDouble()) * 100.0).roundToInt()
            insights.add("$pct% of expenses in this period were captured with OCR scanning ($ocrCount of $totalCount)")
        }

        // 3) Most common registration time (period)
        if (periodExpenses.isNotEmpty()) {
            val hourCounts = mutableMapOf<Int, Int>()
            periodExpenses.forEach { ewl ->
                val h = parseHour(ewl.expense.time) ?: return@forEach
                hourCounts[h] = (hourCounts[h] ?: 0) + 1
            }
            val mostHour = hourCounts.maxByOrNull { it.value }?.key
            if (mostHour != null) {
                insights.add("Most common registration time in this period: ${formatHourOfDay(mostHour)}")
            }
        }

        // 4) Small recurring expenses last 3 months (all-time)
        val threeMonthsAgo = Calendar.getInstance().apply {
            timeInMillis = now.toEpochMilli()
            add(Calendar.MONTH, -3)
        }.timeInMillis
        val smallRecurring = allExpensesAllTime
            .map { it.expense }
            .filter { it.isRecurring && it.amount < SMALL_RECURRING_THRESHOLD_COP && it.date in threeMonthsAgo..now.toEpochMilli() }
        if (smallRecurring.isNotEmpty()) {
            insights.add("${smallRecurring.size} small recurring expenses in the last 3 months")
        }

        // 5) Highest single expense in period
        val highest = periodExpenses.maxByOrNull { it.expense.amount }
        if (highest != null) {
            val name = highest.expense.name.ifBlank { "Unnamed expense" }
            insights.add("Highest expense: $name — ${formatCurrency(highest.expense.amount, displayCurrency, displayRate)}")
        }

        // 6) Micro-purchases in period (< 5,000 COP equivalent)
        val microThresholdDisplay = MICRO_PURCHASE_THRESHOLD_COP * displayRate
        val microCount = periodExpenses.count { it.expense.amount < MICRO_PURCHASE_THRESHOLD_COP }
        if (microCount > 0) {
            insights.add("$microCount small purchases under ${formatCurrencyValue(microThresholdDisplay, displayCurrency)} in this period")
        }

        // 7) Average daily spending in period
        val totalCop = periodExpenses.sumOf { it.expense.amount }
        val days = max(1, java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1)
        if (totalCop > 0) {
            val avgCop = totalCop / days
            insights.add("Average daily spending: ${formatCurrency(avgCop, displayCurrency, displayRate)}")
        }

        // Bonus: reports generated counter
        if (reportsGeneratedCount > 0) {
            insights.add("You have generated $reportsGeneratedCount reports so far")
        }

        // BQ7: Savings goal progress
        try {
            if (activeGoals.isNotEmpty()) {
                val totalTarget = activeGoals.sumOf { it.targetAmount }
                val totalCurrent = activeGoals.sumOf { it.currentAmount }
                if (totalTarget > 0) {
                    val avgPct = (totalCurrent / totalTarget * 100).roundToInt()
                    val goalWord = if (activeGoals.size == 1) "goal" else "goals"
                    val status = if (avgPct >= 50) "on track" else "behind target"
                    insights.add("Savings goals: $avgPct% achieved on average across ${activeGoals.size} active $goalWord ($status)")
                }
            }
        } catch (_: Exception) {}

        // BQ8: Budget midpoint consumption
        try {
            if (totalIncomeCop > 0) {
                val startOfMonth = today.withDayOfMonth(1)
                val currentMonthSpent = allExpensesAllTime.filter {
                    val d = Instant.ofEpochMilli(it.expense.date).atZone(zoneId).toLocalDate()
                    !d.isBefore(startOfMonth) && !d.isAfter(today)
                }.sumOf { it.expense.amount }
                val consumedPct = (currentMonthSpent / totalIncomeCop * 100).roundToInt()
                val daysInMonth = today.lengthOfMonth()
                val pastMidpoint = today.dayOfMonth >= daysInMonth / 2
                val text = if (pastMidpoint && consumedPct > 50) {
                    "Monthly budget: $consumedPct% consumed past the midpoint — overspending risk"
                } else {
                    "Monthly budget: $consumedPct% consumed so far this month"
                }
                insights.add(text)
            }
        } catch (_: Exception) {}

        // BQ9: Highest category spending growth vs previous month
        try {
            val startOfCurrentMonth = today.withDayOfMonth(1)
            val startOfPrevMonth = startOfCurrentMonth.minusMonths(1)
            val prevMonthDay = minOf(today.dayOfMonth, startOfPrevMonth.lengthOfMonth())
            val endOfPrevPeriod = startOfPrevMonth.withDayOfMonth(prevMonthDay)

            fun groupByCategory(expenses: List<ExpenseWithLabels>): Map<String, Double> =
                expenses.groupBy { it.labels.firstOrNull()?.name ?: "Other" }
                    .mapValues { (_, list) -> list.sumOf { it.expense.amount } }

            val currentByCategory = groupByCategory(
                allExpensesAllTime.filter {
                    val d = Instant.ofEpochMilli(it.expense.date).atZone(zoneId).toLocalDate()
                    !d.isBefore(startOfCurrentMonth) && !d.isAfter(today)
                }
            )
            val prevByCategory = groupByCategory(
                allExpensesAllTime.filter {
                    val d = Instant.ofEpochMilli(it.expense.date).atZone(zoneId).toLocalDate()
                    !d.isBefore(startOfPrevMonth) && !d.isAfter(endOfPrevPeriod)
                }
            )

            if (currentByCategory.isNotEmpty()) {
                data class GrowthEntry(val cat: String, val current: Double, val prev: Double, val growth: Double)
                val top = currentByCategory.entries.mapNotNull { (cat, current) ->
                    val prev = prevByCategory[cat] ?: 0.0
                    when {
                        prev > 0 -> GrowthEntry(cat, current, prev, (current - prev) / prev * 100)
                        current > 0 -> GrowthEntry(cat, current, 0.0, Double.MAX_VALUE)
                        else -> null
                    }
                }.maxByOrNull { it.growth }

                if (top != null) {
                    val text = if (top.prev == 0.0) {
                        "New spending category this month: ${top.cat} (${formatCurrency(top.current, displayCurrency, displayRate)})"
                    } else {
                        "Highest category growth: ${top.cat} +${top.growth.roundToInt()}% vs last month (${formatCurrency(top.current, displayCurrency, displayRate)})"
                    }
                    insights.add(text)
                }
            }
        } catch (_: Exception) {}

        return insights
    }

    private fun parseHour(timeStr: String): Int? = try {
        val cleaned = timeStr.trim().uppercase(Locale.US)
        val isPm = cleaned.contains("PM")
        val noPeriod = cleaned.replace("AM", "").replace("PM", "").trim()
        val parts = noPeriod.split(":")
        var hour = parts[0].trim().toInt()
        if (isPm && hour != 12) hour += 12
        if (!isPm && hour == 12) hour = 0
        hour
    } catch (_: Exception) {
        null
    }

    private fun formatHourOfDay(hour: Int): String {
        val period = if (hour < 12) "AM" else "PM"
        val h12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return "$h12:00 $period"
    }

    private fun formatPeriodLabel(start: LocalDate, end: LocalDate): String {
        val sameYear = start.year == end.year
        val monthDay = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
        val monthDayYear = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
        val startStr = if (sameYear) start.format(monthDay) else start.format(monthDayYear)
        val endStr = end.format(monthDayYear)
        return "$startStr – $endStr"
    }

    private val largeFormatter = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))
    private val smallFormatter = DecimalFormat("0.##", DecimalFormatSymbols(Locale.US))

    private fun formatCurrencyValue(value: Double, currency: String): String {
        val v = if (value >= 10.0) largeFormatter.format(value)
                else smallFormatter.format(value).trimEnd('0').trimEnd('.')
        return "$currency $v"
    }

    private fun formatCurrency(amountCop: Double, currency: String, rate: Double): String {
        return formatCurrencyValue(amountCop * rate, currency)
    }
}
