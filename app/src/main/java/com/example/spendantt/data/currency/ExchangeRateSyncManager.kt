package com.example.spendantt.data.currency

import android.content.Context
import com.example.spendantt.data.local.AppDatabase
import com.example.spendantt.data.local.ExchangeRateFileStore
import com.example.spendantt.data.local.entity.ExchangeRateEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object ExchangeRateSyncManager {
    private const val ENDPOINT =
        "https://v6.exchangerate-api.com/v6/e7941c395b69b36c36cad7eb/latest/COP"

    suspend fun triggerIfNeeded(context: Context) {
        val appContext = context.applicationContext
        val database = AppDatabase.getInstance(appContext)
        // Santiago Gomez | Multithreading | 5 pts
        // Dispatchers.IO is used for Room reads so the cached exchange rates are loaded
        // from local storage without blocking the main thread.
        val exchangeRates = withContext(Dispatchers.IO) { database.exchangeRateDao().getAllRates() }

        val requiresUpdate = if (exchangeRates.isEmpty()) {
            true
        } else {
            val lastFetch = Instant.ofEpochMilli(exchangeRates.first().fetchedAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            val today = LocalDate.now()
            val daysSince = ChronoUnit.DAYS.between(lastFetch, today)
            daysSince >= 7 ||
                (today.dayOfWeek == DayOfWeek.SATURDAY && lastFetch.dayOfWeek != DayOfWeek.SATURDAY)
        }

        if (!requiresUpdate) {
            CurrencyProvider.refreshCacheFromDb(appContext)
            return
        }

        val syncResult = runCatching { fetchRatesPayload() }
        syncResult.onSuccess { payload ->
            withContext(Dispatchers.IO) {
                database.exchangeRateDao().upsertRates(payload.rates)
            }
            withContext(Dispatchers.IO) {
                ExchangeRateFileStore.saveRatesBackup(appContext, payload.rawJson)
            }
            CurrencyProvider.refreshCacheFromDb(appContext)
            return
        }

        if (exchangeRates.isEmpty()) {
            val backupRates = withContext(Dispatchers.IO) {
                ExchangeRateFileStore.loadRatesBackup(appContext)?.let(::parseRatesFromJson)
            }
            if (!backupRates.isNullOrEmpty()) {
                withContext(Dispatchers.IO) {
                    database.exchangeRateDao().upsertRates(backupRates)
                }
            }
        }

        CurrencyProvider.refreshCacheFromDb(appContext)
    }

    private suspend fun fetchRatesPayload(): ExchangeRatePayload = withContext(Dispatchers.IO) {
        // Santiago Gomez | Multithreading | 5 pts
        // Dispatchers.IO runs the HTTP request and JSON parsing for exchange rates off the UI thread.
        val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        connection.connect()
        if (connection.responseCode !in 200..299) {
            throw IllegalStateException("Exchange rate request failed with code ${connection.responseCode}")
        }

        val body = connection.inputStream.bufferedReader().use { it.readText() }
        ExchangeRatePayload(
            rates = parseRatesFromJson(body),
            rawJson = body
        )
    }

    private fun parseRatesFromJson(rawJson: String): List<ExchangeRateEntity> {
        val root = JSONObject(rawJson)
        val rates = root.getJSONObject("conversion_rates")
        val fetchedAt = System.currentTimeMillis()

        return buildList {
            val keys = rates.keys()
            while (keys.hasNext()) {
                val currency = keys.next()
                add(
                    ExchangeRateEntity(
                        currency = currency,
                        rate = rates.getDouble(currency),
                        fetchedAt = fetchedAt
                    )
                )
            }
        }
    }

    private data class ExchangeRatePayload(
        val rates: List<ExchangeRateEntity>,
        val rawJson: String
    )
}
