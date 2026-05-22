package com.example.spendantt.util

object CurrencyAmountParser {
    fun parse(value: String): Double? {
        val sanitized = value.trim().replace(" ", "")
        if (sanitized.isEmpty()) return null

        val normalized = when {
            sanitized.contains(",") && sanitized.contains(".") -> {
                if (sanitized.lastIndexOf('.') > sanitized.lastIndexOf(',')) {
                    sanitized.replace(",", "")
                } else {
                    sanitized.replace(".", "").replace(",", ".")
                }
            }
            sanitized.contains(",") -> {
                if (sanitized.substringAfterLast(',').length == 3) {
                    sanitized.replace(",", "")
                } else {
                    sanitized.replace(",", ".")
                }
            }
            else -> sanitized
        }

        return normalized.toDoubleOrNull()
    }
}
