package com.example.spendantt.data.voice

import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.ceil

class VoiceExpenseParser {

    companion object {
        private val cache = LruCache<String, VoiceParseResult>(20)

        private val enOnes = mapOf(
            "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4,
            "five" to 5, "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9,
            "ten" to 10, "eleven" to 11, "twelve" to 12, "thirteen" to 13,
            "fourteen" to 14, "fifteen" to 15, "sixteen" to 16, "seventeen" to 17,
            "eighteen" to 18, "nineteen" to 19, "twenty" to 20, "thirty" to 30,
            "forty" to 40, "fifty" to 50, "sixty" to 60, "seventy" to 70,
            "eighty" to 80, "ninety" to 90, "hundred" to 100
        )
        private val enMultipliers = mapOf(
            "thousand" to 1_000L,
            "million" to 1_000_000L,
            "billion" to 1_000_000_000L
        )
        private val currencyWords = setOf(
            "dollars", "dollar", "euros", "euro", "pounds", "pound", "usd", "eur", "gbp",
            "peso", "pesos", "bucks", "quid", "yen", "reais", "real", "cad", "aud", "chf",
            "mxn", "yuan", "rmb", "clp", "soles", "ars"
        )
        private val articles = setOf("a", "an", "the")
        private val currencyPatterns = linkedMapOf(
            Regex("""\bdollar[s]?\b|\busd\b|\bbucks?\b|\$""", RegexOption.IGNORE_CASE) to "USD",
            Regex("""\beuro[s]?\b|\beur\b|€""", RegexOption.IGNORE_CASE) to "EUR",
            Regex("""\bpeso[s]?\b|\bcop\b""", RegexOption.IGNORE_CASE) to "COP",
            Regex("""\bpound[s]?\b|\bsterling\b|\bgbp\b|\bquid\b|£""", RegexOption.IGNORE_CASE) to "GBP",
            Regex("""\byen[s]?\b|\bjpy\b|¥""", RegexOption.IGNORE_CASE) to "JPY",
            Regex("""\breais?\b|\breal[is]?\b|\bbrl\b""", RegexOption.IGNORE_CASE) to "BRL",
            Regex("""\bcad\b|\bcanadian\b""", RegexOption.IGNORE_CASE) to "CAD",
            Regex("""\baud\b|\baustralian\b""", RegexOption.IGNORE_CASE) to "AUD",
            Regex("""\bchf\b|\bswiss\b|\bfranc[s]?\b""", RegexOption.IGNORE_CASE) to "CHF",
            Regex("""\bmxn\b|\bmexican\b""", RegexOption.IGNORE_CASE) to "MXN",
            Regex("""\bcny\b|\byuan[s]?\b|\brenminbi\b|\brmb\b""", RegexOption.IGNORE_CASE) to "CNY",
            Regex("""\bclp\b|\bchilean\b""", RegexOption.IGNORE_CASE) to "CLP",
            Regex("""\bsoles?\b|\bperuvian\b""", RegexOption.IGNORE_CASE) to "PEN",
            Regex("""\bars\b|\bargentin[ae]\b""", RegexOption.IGNORE_CASE) to "ARS"
        )
        private val mixedAmountRegex = Regex(
            """(\d+(?:[.,]\d+)?)\s*(thousand|million|billion)""",
            RegexOption.IGNORE_CASE
        )
        private val numericRegex = Regex("""(?<!\d)(\d{1,3}(?:[.,]\d{3})*|\d+(?:[.,]\d{1,2})?)(?!\d)""")
        private val enDayMap = mapOf(
            "monday" to Calendar.MONDAY,
            "tuesday" to Calendar.TUESDAY,
            "wednesday" to Calendar.WEDNESDAY,
            "thursday" to Calendar.THURSDAY,
            "friday" to Calendar.FRIDAY,
            "saturday" to Calendar.SATURDAY,
            "sunday" to Calendar.SUNDAY
        )

        private val dateFormatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        private val timeFormatter = SimpleDateFormat("hh:mma", Locale.getDefault())
    }

    suspend fun parse(rawText: String, defaultCurrency: String = "COP"): VoiceParseResult =
        withContext(Dispatchers.IO) {
            val key = cacheKey(rawText, defaultCurrency)
            cache.get(key)?.let { return@withContext it }

            val detectedCurrency = detectCurrency(rawText) ?: defaultCurrency
            val result = VoiceParseResult(
                amount = extractAmount(rawText),
                detectedCurrencyCode = detectedCurrency,
                date = extractDate(rawText),
                time = extractTime(rawText),
                locationName = extractLocation(rawText),
                name = extractName(rawText)
            )
            cache.put(key, result)
            result
        }

    fun cacheKey(rawText: String, defaultCurrency: String): String {
        return "${rawText.trim().lowercase(Locale.getDefault())} ${defaultCurrency.uppercase(Locale.getDefault())}"
    }

    fun convertAmountToCop(amount: Double, currencyCode: String, rateProvider: (String) -> Double): Double {
        if (currencyCode == "COP") return amount
        val rate = rateProvider(currencyCode)
        return if (rate == 0.0) 0.0 else ceil(amount / rate)
    }

    private fun extractAmount(text: String): String? {
        val lower = text.lowercase(Locale.getDefault())

        mixedAmountRegex.find(lower)?.let { match ->
            val number = normalizeNumber(match.groupValues[1]) ?: return@let
            val multiplier = enMultipliers[match.groupValues[2].lowercase()] ?: return@let
            return (number * multiplier).toLong().toString()
        }

        numericRegex.findAll(lower).toList()
            .maxByOrNull { it.value.replace(Regex("[.,]"), "").length }
            ?.let { match ->
                val value = normalizeNumber(match.value)
                if (value != null && value > 0) return value.toLong().toString()
            }

        parseWrittenAmount(lower)?.let { return it.toString() }
        return null
    }

    private fun normalizeNumber(raw: String): Double? = when {
        raw.contains(',') && raw.contains('.') ->
            if (raw.lastIndexOf('.') > raw.lastIndexOf(',')) raw.replace(",", "").toDoubleOrNull()
            else raw.replace(".", "").replace(",", ".").toDoubleOrNull()
        raw.contains('.') ->
            if (raw.substringAfterLast('.').length == 3) raw.replace(".", "").toDoubleOrNull()
            else raw.toDoubleOrNull()
        raw.contains(',') ->
            if (raw.substringAfterLast(',').length == 3) raw.replace(",", "").toDoubleOrNull()
            else raw.replace(",", ".").toDoubleOrNull()
        else -> raw.toDoubleOrNull()
    }

    private fun parseWrittenAmount(text: String): Long? {
        val words = text.split(Regex("[\\s,]+")).filter { it.isNotBlank() && it != "and" }
        var current = 0L
        var total = 0L
        var found = false

        for (word in words) {
            val onesValue = enOnes[word]
            val multiplier = enMultipliers[word]
            when {
                onesValue != null -> {
                    current += onesValue
                    found = true
                }
                multiplier != null && found -> {
                    if (multiplier >= 1_000L) {
                        total += (if (current == 0L) 1L else current) * multiplier
                        current = 0L
                    } else {
                        current *= multiplier
                    }
                }
                word in currencyWords -> continue
                found -> break
            }
        }

        val result = total + current
        return if (result > 0L) result else null
    }

    private fun detectCurrency(text: String): String? {
        return currencyPatterns.entries.firstOrNull { (pattern, _) -> pattern.containsMatchIn(text) }?.value
    }

    private fun extractDate(text: String): String? {
        val lower = text.lowercase(Locale.getDefault())
        val calendar = Calendar.getInstance()
        return when {
            "today" in lower -> dateFormatter.format(calendar.time)
            "yesterday" in lower -> {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                dateFormatter.format(calendar.time)
            }
            "tomorrow" in lower -> {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                dateFormatter.format(calendar.time)
            }
            else -> enDayMap.entries.firstOrNull { it.key in lower }?.let { (_, dayConst) ->
                val diff = ((calendar.get(Calendar.DAY_OF_WEEK) - dayConst) + 7) % 7
                calendar.add(Calendar.DAY_OF_YEAR, -if (diff == 0) 7 else diff)
                dateFormatter.format(calendar.time)
            }
        }
    }

    private fun extractTime(text: String): String? {
        val withPrep = Regex("""at\s+(\d{1,2})(?::(\d{2}))?\s*(am|pm)?""", RegexOption.IGNORE_CASE).find(text)
        val standalone = Regex("""(\d{1,2})(?::(\d{2}))?\s*(am|pm)""", RegexOption.IGNORE_CASE).find(text)
        val match = withPrep ?: standalone ?: return null

        var hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: 0
        val ampm = match.groupValues[3].lowercase(Locale.getDefault())

        if (hour > 23) return null
        when {
            ampm == "pm" && hour != 12 -> hour += 12
            ampm == "am" && hour == 12 -> hour = 0
            ampm.isEmpty() && hour in 1..6 -> hour += 12
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        return timeFormatter.format(calendar.time)
    }

    private fun extractLocation(text: String): String? {
        val pattern = Regex(
            """(?:at the|at)\s+([A-Za-z][A-Za-z\s'\-]{1,40}?)(?=\s+(?:today|yesterday|tomorrow|on|at|\d)|$)""",
            RegexOption.IGNORE_CASE
        )
        val candidate = pattern.find(text)?.groupValues?.getOrNull(1)?.trim() ?: return null
        if (candidate.length < 2 || candidate.lowercase() in setOf("home", "work")) return null
        return candidate.replaceFirstChar { it.uppercase() }
    }

    private fun extractName(text: String): String? {
        listOf(
            Regex(
                """(?:paid|spent|bought|purchased|charged)\s+.+?\s+(?:for|on)\s+(.+?)(?=\s+(?:today|yesterday|tomorrow|at|on|\d)|$)""",
                RegexOption.IGNORE_CASE
            ),
            Regex(
                """(?:^|\s)for\s+([A-Za-z][A-Za-z\s'\-]{1,40}?)(?=\s+(?:today|yesterday|tomorrow|at|on|\d)|$)""",
                RegexOption.IGNORE_CASE
            )
        ).forEach { pattern ->
            val raw = pattern.find(text)?.groupValues?.getOrNull(1)?.trim() ?: return@forEach
            val cleaned = raw.split(Regex("\\s+"))
                .dropWhile { it.lowercase() in articles }
                .filter { it.lowercase() !in currencyWords }
                .joinToString(" ")
                .trim()
            if (cleaned.length >= 2) return cleaned.replaceFirstChar { it.uppercase() }
        }
        return null
    }
}
