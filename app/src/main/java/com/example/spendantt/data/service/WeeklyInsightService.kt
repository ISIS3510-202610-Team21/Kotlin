package com.example.spendantt.data.service

import com.example.spendantt.data.local.entity.ExpenseWithLabels
import kotlin.random.Random

data class WeeklyInsight(
    val notificationId: String,
    val title: String,
    val body: String
)

class WeeklyInsightService(
    private val autoCategorizationUsageTracker: AutoCategorizationUsageTracker
) {

    fun buildWeeklyInsight(
        userId: Int,
        expenses: List<ExpenseWithLabels>,
        weekStartInclusive: Long,
        weekEndInclusive: Long,
        saturdayStart: Long
    ): WeeklyInsight {
        val weeklyExpenses = expenses.filter { it.expense.date in weekStartInclusive..weekEndInclusive }
        val candidates = listOf(
            buildMicroExpensesInsight(weeklyExpenses, saturdayStart),
            buildMostCommonTimeInsight(weeklyExpenses, saturdayStart),
            buildAutoCategorizationInsight(
                userId = userId,
                fromInclusive = weekStartInclusive,
                toInclusive = weekEndInclusive,
                saturdayStart = saturdayStart
            )
        )

        val random = Random(userId.toLong() + saturdayStart)
        return candidates[random.nextInt(candidates.size)]
    }

    private fun buildMicroExpensesInsight(
        weeklyExpenses: List<ExpenseWithLabels>,
        saturdayStart: Long
    ): WeeklyInsight {
        val total = weeklyExpenses
            .filter { it.expense.amount < MICRO_EXPENSE_THRESHOLD }
            .sumOf { it.expense.amount }

        return WeeklyInsight(
            notificationId = "weekly_insight_$saturdayStart",
            title = "Weekly spending insight",
            body = "This week you spent \$${formatMoney(total)} on small purchases. These can add up quickly."
        )
    }

    private fun buildMostCommonTimeInsight(
        weeklyExpenses: List<ExpenseWithLabels>,
        saturdayStart: Long
    ): WeeklyInsight {
        val period = weeklyExpenses
            .mapNotNull { hourBucket(it.expense.time) }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            ?.let(::hourRangeLabel)
            ?: "2pm to 3pm"

        return WeeklyInsight(
            notificationId = "weekly_insight_$saturdayStart",
            title = "Weekly spending insight",
            body = "You usually spend money around $period. Stay mindful during this period."
        )
    }

    private fun buildAutoCategorizationInsight(
        userId: Int,
        fromInclusive: Long,
        toInclusive: Long,
        saturdayStart: Long
    ): WeeklyInsight {
        val usageCount = autoCategorizationUsageTracker.countBetween(
            userId = userId,
            fromInclusive = fromInclusive,
            toInclusive = toInclusive
        )

        return WeeklyInsight(
            notificationId = "weekly_insight_$saturdayStart",
            title = "Weekly spending insight",
            body = "You relied on auto-categorization $usageCount times this week."
        )
    }

    private fun hourBucket(time: String): Int? {
        val normalized = time
            .lowercase()
            .replace(".", "")
            .trim()

        val timePart = normalized.substringBefore("am").substringBefore("pm").trim()
        val pieces = timePart.split(":")
        val hour = pieces.getOrNull(0)?.toIntOrNull() ?: return null
        val isPm = normalized.contains("pm")
        val isAm = normalized.contains("am")

        val hour24 = when {
            isPm && hour in 1..11 -> hour + 12
            isAm && hour == 12 -> 0
            else -> hour
        }

        return hour24
    }

    private fun hourRangeLabel(hour24: Int): String {
        val startHour = normalizeHour(hour24)
        val endHour = normalizeHour(hour24 + 1)
        return "${formatHour(startHour)} to ${formatHour(endHour)}"
    }

    private fun normalizeHour(hour: Int): Int = ((hour % 24) + 24) % 24

    private fun formatHour(hour24: Int): String {
        val suffix = if (hour24 < 12) "am" else "pm"
        val hour12 = when (val rawHour = hour24 % 12) {
            0 -> 12
            else -> rawHour
        }
        return "$hour12$suffix"
    }

    private fun formatMoney(amount: Double): String {
        return java.text.DecimalFormat("#,##0").format(amount)
    }

    private companion object {
        const val MICRO_EXPENSE_THRESHOLD = 5000.0
    }
}
