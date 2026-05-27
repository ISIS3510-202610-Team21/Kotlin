package com.example.spendantt.data.currency

object CurrencyMinimums {
    private val goalMinimumByCurrency = mapOf(
        "COP" to 50.0,
        "USD" to 1.0,
        "EUR" to 1.0,
        "GBP" to 1.0,
        "JPY" to 1.0,
        "CAD" to 1.0,
        "AUD" to 1.0,
        "MXN" to 1.0,
        "BRL" to 1.0,
        "CLP" to 1.0,
        "PEN" to 1.0,
        "ARS" to 1.0,
        "CHF" to 1.0,
        "CNY" to 1.0
    )

    fun goalMinimumFor(currencyIso: String): Double {
        return goalMinimumByCurrency[currencyIso.uppercase()] ?: 1.0
    }
}
