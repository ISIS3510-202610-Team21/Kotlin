package com.example.spendantt.data.currency

import android.content.Context
import android.util.LruCache
import com.example.spendantt.data.local.AppDatabase
import com.example.spendantt.data.local.CurrencyDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.ceil

data class CurrencyUiState(
    val activeCurrency: String = "COP",
    val activeRate: Double = 1.0,
    val ratesCache: Map<String, Double> = mapOf("COP" to 1.0)
)

object CurrencyProvider {
    // Santiago Gomez | Cache | 15 pts
    // This LruCache is sized to the bounded currency set used by the feature, so every supported
    // ISO rate can stay in memory and repeated conversions avoid extra local reads after warm-up.
    private val mutex = Mutex()
    private val decimalSymbols = DecimalFormatSymbols(Locale.US)
    private val largeFormatter = DecimalFormat("#,##0", decimalSymbols)
    private val compactFormatter = DecimalFormat("0.##", decimalSymbols)
    private const val SUPPORTED_CURRENCY_CACHE_SIZE = 14
    private val ratesLruCache = object : LruCache<String, Double>(SUPPORTED_CURRENCY_CACHE_SIZE) {
        override fun sizeOf(key: String, value: Double): Int = 1
    }

    private val _uiState = MutableStateFlow(CurrencyUiState())
    val uiState: StateFlow<CurrencyUiState> = _uiState.asStateFlow()

    val activeCurrency: String
        get() = _uiState.value.activeCurrency

    val activeRate: Double
        get() = _uiState.value.activeRate

    val ratesCache: Map<String, Double>
        get() = _uiState.value.ratesCache

    suspend fun loadFromDb(context: Context) {
        mutex.withLock {
            val db = AppDatabase.getInstance(context.applicationContext)
            val persistedRates = db.exchangeRateDao().getAllRates()
            val cache = linkedMapOf("COP" to 1.0)
            persistedRates.forEach { cache[it.currency] = it.rate }
            primeRatesCache(cache)

            val savedCurrency = CurrencyDataStore.loadActiveCurrency(context.applicationContext)
                ?.uppercase(Locale.US)
                ?: "COP"
            val savedRate = cache[savedCurrency] ?: 1.0

            _uiState.value = CurrencyUiState(
                activeCurrency = if (cache.containsKey(savedCurrency)) savedCurrency else "COP",
                activeRate = if (cache.containsKey(savedCurrency)) savedRate else 1.0,
                ratesCache = cache
            )
        }
    }

    suspend fun setActiveCurrency(context: Context, iso: String, rate: Double) {
        val normalizedIso = iso.uppercase(Locale.US)
        val normalizedRate = if (rate > 0.0) rate else 1.0
        CurrencyDataStore.saveActiveCurrency(context.applicationContext, normalizedIso)
        _uiState.value = _uiState.value.copy(
            activeCurrency = normalizedIso,
            activeRate = normalizedRate
        )
    }

    suspend fun refreshCacheFromDb(context: Context) {
        loadFromDb(context)
    }

    fun convertToLocal(amountInCOP: Double): Double = amountInCOP * activeRate

    fun convertToCOP(amountInLocal: Double): Double {
        return if (activeRate == 0.0) 0.0 else ceil(amountInLocal / activeRate)
    }

    fun formatFromCOP(amountInCOP: Double): String {
        return "${activeCurrency} ${formatValue(convertToLocal(amountInCOP))}"
    }

    fun formatValue(value: Double): String {
        return if (value >= 10.0) {
            largeFormatter.format(value)
        } else {
            compactFormatter.format(value).trimEnd('0').trimEnd('.').ifEmpty { "0" }
        }
    }

    fun formatAmountForInput(value: Double): String {
        return compactFormatter.format(value).trimEnd('0').trimEnd('.')
    }

    // Santiago Gomez | Cache | 5 pts
    // rateFor uses a read-through cache policy: it checks the LruCache first, falls back to the
    // current in-memory snapshot if needed, and then rehydrates the LRU entry for future reads.
    fun rateFor(iso: String): Double {
        val normalizedIso = iso.uppercase(Locale.US)
        ratesLruCache.get(normalizedIso)?.let { return it }
        val snapshotRate = _uiState.value.ratesCache[normalizedIso] ?: 1.0
        ratesLruCache.put(normalizedIso, snapshotRate)
        return snapshotRate
    }

    private fun primeRatesCache(rates: Map<String, Double>) {
        ratesLruCache.evictAll()
        rates.forEach { (iso, rate) ->
            ratesLruCache.put(iso.uppercase(Locale.US), rate)
        }
    }
}
