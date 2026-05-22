package com.example.spendantt.data.voice

import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// CACHING | LRU | Avoids re-parsing identical transcripts (mic retries, draft reload)
// MULTI-THREADING | parse() switches to Dispatchers.IO — regex work off the main thread

class VoiceExpenseParser {

    companion object {
        private val cache = LruCache<String, VoiceParseResult>(20)

        private val EN_ONES = mapOf(
            "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4,
            "five" to 5, "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9,
            "ten" to 10, "eleven" to 11, "twelve" to 12, "thirteen" to 13,
            "fourteen" to 14, "fifteen" to 15, "sixteen" to 16, "seventeen" to 17,
            "eighteen" to 18, "nineteen" to 19, "twenty" to 20, "thirty" to 30,
            "forty" to 40, "fifty" to 50, "sixty" to 60, "seventy" to 70,
            "eighty" to 80, "ninety" to 90, "hundred" to 100
        )
        private val EN_MULTIPLIERS = mapOf(
            "thousand" to 1_000L, "million" to 1_000_000L, "billion" to 1_000_000_000L
        )

        private val CURRENCY_WORDS = setOf(
            "dollars", "dollar", "euros", "euro", "pounds", "pound", "usd", "eur", "gbp"
        )
        private val ARTICLES = setOf("a", "an", "the")

        // "45 thousand", "2.5 million" — digit + word multiplier
        private val MIXED_AMOUNT_REGEX = Regex(
            """(\d+(?:[.,]\d+)?)\s*(thousand|million|billion)""",
            RegexOption.IGNORE_CASE
        )
        private val NUMERIC_REGEX = Regex("""(?<!\d)(\d{1,3}(?:[.,]\d{3})*|\d+(?:[.,]\d{1,2})?)(?!\d)""")

        private val EN_DAY_MAP = mapOf(
            "monday" to Calendar.MONDAY, "tuesday" to Calendar.TUESDAY,
            "wednesday" to Calendar.WEDNESDAY, "thursday" to Calendar.THURSDAY,
            "friday" to Calendar.FRIDAY, "saturday" to Calendar.SATURDAY,
            "sunday" to Calendar.SUNDAY
        )

        private val DATE_FMT = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        private val TIME_FMT = SimpleDateFormat("hh:mma", Locale.getDefault())
    }

    suspend fun parse(rawText: String): VoiceParseResult = withContext(Dispatchers.IO) {
        val key = rawText.trim().lowercase(Locale.getDefault())
        cache.get(key)?.let { return@withContext it }
        val result = VoiceParseResult(
            amount               = extractAmount(rawText),
            detectedCurrencyCode = detectCurrency(rawText),
            date                 = extractDate(rawText),
            time                 = extractTime(rawText),
            locationName         = extractLocation(rawText),
            name                 = extractName(rawText)
        )
        cache.put(key, result)
        result
    }

    // ── Amount ───────────────────────────────────────────────────────────────

    private fun extractAmount(text: String): String? {
        val lower = text.lowercase(Locale.getDefault())

        // 1. "45 thousand", "2.5 million"
        MIXED_AMOUNT_REGEX.find(lower)?.let { m ->
            val num  = normalizeNumber(m.groupValues[1]) ?: return@let
            val mult = EN_MULTIPLIERS[m.groupValues[2].lowercase()] ?: return@let
            return (num * mult).toLong().toString()
        }

        // 2. Plain numeric: "25000", "25,000", "25.000"
        NUMERIC_REGEX.findAll(lower).toList()
            .maxByOrNull { it.value.replace(Regex("[.,]"), "").length }
            ?.let { m ->
                val v = normalizeNumber(m.value)
                if (v != null && v > 0) return v.toLong().toString()
            }

        // 3. Written: "fifteen thousand", "forty five"
        parseWrittenAmount(lower)?.let { return it.toString() }

        return null
    }

    private fun normalizeNumber(raw: String): Double? = when {
        raw.contains(',') && raw.contains('.') ->
            if (raw.lastIndexOf('.') > raw.lastIndexOf(','))
                raw.replace(",", "").toDoubleOrNull()
            else
                raw.replace(".", "").replace(",", ".").toDoubleOrNull()
        raw.contains('.') ->
            if (raw.substringAfterLast('.').length == 3) raw.replace(".", "").toDoubleOrNull()
            else raw.toDoubleOrNull()
        raw.contains(',') ->
            if (raw.substringAfterLast(',').length == 3) raw.replace(",", "").toDoubleOrNull()
            else raw.replace(",", ".").toDoubleOrNull()
        else -> raw.toDoubleOrNull()
    }

    private fun parseWrittenAmount(text: String): Long? {
        val words = text.split(Regex("[\\s,]+"))
            .filter { it.isNotBlank() && it != "and" }
        var current = 0L
        var total   = 0L
        var found   = false
        for (word in words) {
            val onesVal = EN_ONES[word]
            val multVal = EN_MULTIPLIERS[word]
            when {
                onesVal != null -> { current += onesVal; found = true }
                multVal != null && found -> {
                    if (multVal >= 1_000L) {
                        total += (if (current == 0L) 1L else current) * multVal
                        current = 0L
                    } else {
                        current *= multVal
                    }
                }
                word in CURRENCY_WORDS -> continue
                found -> break
            }
        }
        val result = total + current
        return if (result > 0L) result else null
    }

    // ── Currency ─────────────────────────────────────────────────────────────

    private fun detectCurrency(text: String): String? {
        val lower = text.lowercase(Locale.getDefault())
        return when {
            "dollar" in lower || "usd" in lower -> "USD"
            "euro"   in lower || "eur" in lower -> "EUR"
            "pound"  in lower || "gbp" in lower -> "GBP"
            else -> null
        }
    }

    // ── Date ─────────────────────────────────────────────────────────────────

    private fun extractDate(text: String): String? {
        val lower = text.lowercase(Locale.getDefault())
        val cal   = Calendar.getInstance()
        return when {
            "today"     in lower -> DATE_FMT.format(cal.time)
            "yesterday" in lower -> { cal.add(Calendar.DAY_OF_YEAR, -1); DATE_FMT.format(cal.time) }
            "tomorrow"  in lower -> { cal.add(Calendar.DAY_OF_YEAR,  1); DATE_FMT.format(cal.time) }
            else -> EN_DAY_MAP.entries.firstOrNull { it.key in lower }?.let { (_, dayConst) ->
                val diff = ((cal.get(Calendar.DAY_OF_WEEK) - dayConst) + 7) % 7
                cal.add(Calendar.DAY_OF_YEAR, -if (diff == 0) 7 else diff)
                DATE_FMT.format(cal.time)
            }
        }
    }

    // ── Time ─────────────────────────────────────────────────────────────────

    private fun extractTime(text: String): String? {
        val withPrep = Regex(
            """at\s+(\d{1,2})(?::(\d{2}))?\s*(am|pm)?""",
            RegexOption.IGNORE_CASE
        ).find(text)
        val standalone = Regex(
            """(\d{1,2})(?::(\d{2}))?\s*(am|pm)""",
            RegexOption.IGNORE_CASE
        ).find(text)
        val m = withPrep ?: standalone ?: return null

        var hour   = m.groupValues[1].toIntOrNull() ?: return null
        val minute = m.groupValues[2].toIntOrNull() ?: 0
        val ampm   = m.groupValues[3].lowercase(Locale.getDefault())

        if (hour > 23) return null
        when {
            ampm == "pm" && hour != 12 -> hour += 12
            ampm == "am" && hour == 12 -> hour = 0
            ampm.isEmpty() && hour in 1..6 -> hour += 12
        }
        val c = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
        }
        return TIME_FMT.format(c.time)
    }

    // ── Location ─────────────────────────────────────────────────────────────

    private fun extractLocation(text: String): String? {
        val pattern = Regex(
            """(?:at the|at)\s+([A-Za-z][A-Za-z\s'\-]{1,40}?)(?=\s+(?:today|yesterday|tomorrow|on|at|\d)|$)""",
            RegexOption.IGNORE_CASE
        )
        val candidate = pattern.find(text)?.groupValues?.getOrNull(1)?.trim() ?: return null
        if (candidate.length < 2 || candidate.lowercase() in setOf("home", "work")) return null
        return candidate.replaceFirstChar { it.uppercase() }
    }

    // ── Name ─────────────────────────────────────────────────────────────────

    private fun extractName(text: String): String? {
        listOf(
            // With verb: "paid/spent/bought ... for [NAME]"
            Regex(
                """(?:paid|spent|bought|purchased|charged)\s+.+?\s+(?:for|on)\s+(.+?)(?=\s+(?:today|yesterday|tomorrow|at|on|\d)|$)""",
                RegexOption.IGNORE_CASE
            ),
            // Fallback: bare "for [NAME]"
            Regex(
                """(?:^|\s)for\s+([A-Za-z][A-Za-z\s'\-]{1,40}?)(?=\s+(?:today|yesterday|tomorrow|at|on|\d)|$)""",
                RegexOption.IGNORE_CASE
            )
        ).forEach { pattern ->
            val raw = pattern.find(text)?.groupValues?.getOrNull(1)?.trim() ?: return@forEach
            val cleaned = raw.split(Regex("\\s+"))
                .dropWhile { it.lowercase() in ARTICLES }
                .filter { it.lowercase() !in CURRENCY_WORDS }
                .joinToString(" ").trim()
            if (cleaned.length >= 2) return cleaned.replaceFirstChar { it.uppercase() }
        }
        return null
    }
}
