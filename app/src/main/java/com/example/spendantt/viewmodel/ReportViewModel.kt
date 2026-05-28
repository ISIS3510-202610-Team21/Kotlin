package com.example.spendantt.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spendantt.data.currency.CurrencyProvider
import com.example.spendantt.data.local.entity.ReportCacheEntity
import com.example.spendantt.data.report.FinancialReport
import com.example.spendantt.data.report.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class ReportSetupState(
    val startDate: LocalDate = LocalDate.now().minusDays(7),
    val endDate: LocalDate = LocalDate.now(),
    val firstExpenseDate: LocalDate? = null,
    val cachedRowForCurrentRange: ReportCacheEntity? = null,
    val recentReports: List<ReportCacheEntity> = emptyList(),
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
)

class ReportViewModel(
    appContext: Context,
    private val userId: Int,
) : ViewModel() {

    private val repo = ReportRepository(appContext.applicationContext)

    private val _setup = MutableStateFlow(ReportSetupState())
    val setup: StateFlow<ReportSetupState> = _setup.asStateFlow()

    private val _currentReport = MutableStateFlow<FinancialReport?>(null)
    val currentReport: StateFlow<FinancialReport?> = _currentReport.asStateFlow()

    init {
        viewModelScope.launch {
            val first = repo.getFirstExpenseDate(userId)
            val today = LocalDate.now()
            val rawStart = first ?: today.minusDays(7)
            val clampedStart = if (rawStart.isAfter(today)) today else rawStart
            _setup.value = _setup.value.copy(
                firstExpenseDate = first,
                startDate = clampedStart,
                endDate = today,
            )
            refreshCacheLookup()
            loadRecent()
        }
    }

    fun onStartDateChange(date: LocalDate) {
        val today = LocalDate.now()
        val first = _setup.value.firstExpenseDate
        var s = date
        if (first != null && s.isBefore(first)) s = first
        if (s.isAfter(today)) s = today
        val e = if (_setup.value.endDate.isBefore(s)) s else _setup.value.endDate
        _setup.value = _setup.value.copy(startDate = s, endDate = e)
        refreshCacheLookup()
    }

    fun onEndDateChange(date: LocalDate) {
        val today = LocalDate.now()
        var e = date
        if (e.isAfter(today)) e = today
        val s = if (_setup.value.startDate.isAfter(e)) e else _setup.value.startDate
        _setup.value = _setup.value.copy(startDate = s, endDate = e)
        refreshCacheLookup()
    }

    private fun refreshCacheLookup() {
        viewModelScope.launch {
            val s = _setup.value.startDate
            val e = _setup.value.endDate
            val row = repo.peekCacheRow(userId, s, e)
            _setup.value = _setup.value.copy(cachedRowForCurrentRange = row)
        }
    }

    private fun loadRecent() {
        viewModelScope.launch {
            _setup.value = _setup.value.copy(recentReports = repo.getRecentReports(userId))
        }
    }

    /** Generate (or load from cache) and open the report. */
    fun generate() {
        viewModelScope.launch {
            val s = _setup.value.startDate
            val e = _setup.value.endDate
            _setup.value = _setup.value.copy(isGenerating = true, errorMessage = null)
            try {
                val currency = CurrencyProvider.activeCurrency
                val rate = CurrencyProvider.activeRate
                val cached = repo.getCached(userId, s, e)
                val report = cached ?: repo.generate(userId, s, e, currency, rate)
                _currentReport.value = report
                refreshCacheLookup()
                loadRecent()
            } catch (ex: Exception) {
                _setup.value = _setup.value.copy(errorMessage = ex.message ?: "Failed to generate report")
            } finally {
                _setup.value = _setup.value.copy(isGenerating = false)
            }
        }
    }

    /** Open an existing cached report row directly (no aggregation). */
    fun openCached(row: ReportCacheEntity) {
        viewModelScope.launch {
            val report = repo.decodeRow(row)
            if (report != null) {
                _setup.value = _setup.value.copy(
                    startDate = LocalDate.ofEpochDay(row.startEpochDay),
                    endDate = LocalDate.ofEpochDay(row.endEpochDay),
                )
                _currentReport.value = report
                refreshCacheLookup()
            }
        }
    }

    fun closeReport() {
        _currentReport.value = null
    }

    fun clearError() {
        _setup.value = _setup.value.copy(errorMessage = null)
    }

    companion object {
        fun millisToLocalDate(millis: Long): LocalDate =
            java.time.Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()

        fun localDateToMillis(date: LocalDate): Long =
            date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
