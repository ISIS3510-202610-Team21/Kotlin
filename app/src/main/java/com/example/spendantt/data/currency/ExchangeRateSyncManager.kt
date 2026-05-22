package com.example.spendantt.data.currency

import android.content.Context
import com.example.spendantt.data.local.AppDatabase
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

        val fetchedRates = fetchRates()
        withContext(Dispatchers.IO) {
            database.exchangeRateDao().upsertRates(fetchedRates)
        }
        CurrencyProvider.refreshCacheFromDb(appContext)
    }

    private suspend fun fetchRates(): List<ExchangeRateEntity> = withContext(Dispatchers.IO) {
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
        val root = JSONObject(body)
        val rates = root.getJSONObject("conversion_rates")
        val fetchedAt = System.currentTimeMillis()

        buildList {
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
}
