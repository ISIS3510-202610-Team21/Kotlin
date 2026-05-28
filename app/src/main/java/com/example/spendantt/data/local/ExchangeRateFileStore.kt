package com.example.spendantt.data.local

import android.content.Context

object ExchangeRateFileStore {
    // Santiago Gomez | Local Storage | 5 pts
    // Stores a JSON backup of the latest successful exchange-rate payload in internal storage
    // so the currency feature can recover data even if the main database is empty.
    private const val FILE_NAME = "exchange_rates_backup.json"

    fun saveRatesBackup(context: Context, rawJson: String) {
        context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE).bufferedWriter().use { writer ->
            writer.write(rawJson)
        }
    }

    fun loadRatesBackup(context: Context): String? {
        return runCatching {
            context.openFileInput(FILE_NAME).bufferedReader().use { it.readText() }
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }
}
