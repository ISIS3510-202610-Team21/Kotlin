package com.example.spendantt.data.currency

import android.content.Context
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
    // Santiago Gomez | Cache
    // Keeps the active currency and exchange rates in memory so currency reads stay instant across the app.
    private val mutex = Mutex()
    private val decimalSymbols = DecimalFormatSymbols(Locale.US)
    private val largeFormatter = DecimalFormat("#,##0", decimalSymbols)
    private val compactFormatter = DecimalFormat("0.##", decimalSymbols)

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
            compactFormatter.format(value).trimEnd('0').trimEnd('.')
        }
    }

    fun formatAmountForInput(value: Double): String {
        return compactFormatter.format(value).trimEnd('0').trimEnd('.')
    }

    fun rateFor(iso: String): Double = ratesCache[iso.uppercase(Locale.US)] ?: 1.0
}
