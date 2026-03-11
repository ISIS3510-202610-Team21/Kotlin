package com.example.spendantt.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spendantt.data.local.AppDatabase
import com.example.spendantt.data.local.entity.ExpenseEntity
import com.example.spendantt.data.local.entity.ExpenseSource
import com.example.spendantt.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class NewExpenseUiState(
    val name: String = "",
    val amount: String = "",
    val date: String = "",
    val time: String = "",
    val locationName: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val receiptImageUri: Uri? = null,
    val source: ExpenseSource = ExpenseSource.MANUAL,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

class NewExpenseViewModel(
    context: Context,
    private val userId: Int
) : ViewModel() {

    private val db = AppDatabase.getInstance(context)
    private val repository = ExpenseRepository(db.expenseDao(), db.labelDao())

    private val _uiState = MutableStateFlow(NewExpenseUiState())
    val uiState: StateFlow<NewExpenseUiState> = _uiState

    init {
        val now = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mma", Locale.getDefault())
        _uiState.value = _uiState.value.copy(
            date = dateFormat.format(now.time),
            time = timeFormat.format(now.time)
        )
    }

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(name = value)
    }

    fun onAmountChange(value: String) {
        _uiState.value = _uiState.value.copy(amount = value)
    }

    fun onReceiptSelected(uri: Uri) {
        _uiState.value = _uiState.value.copy(
            receiptImageUri = uri,
            source = ExpenseSource.OCR
        )
        // Fase OCR: aquí se llamará processReceiptOcr(uri)
    }

    /**
     * Llamado por el procesador OCR para autocompletar campos.
     * Solo sobreescribe campos que el OCR pudo detectar (non-null).
     */
    fun autoFillFromReceipt(
        name: String? = null,
        amount: String? = null,
        date: String? = null,
        time: String? = null,
        locationName: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        _uiState.value = _uiState.value.copy(
            name = name ?: _uiState.value.name,
            amount = amount ?: _uiState.value.amount,
            date = date ?: _uiState.value.date,
            time = time ?: _uiState.value.time,
            locationName = locationName ?: _uiState.value.locationName,
            latitude = latitude ?: _uiState.value.latitude,
            longitude = longitude ?: _uiState.value.longitude,
            source = ExpenseSource.OCR
        )
    }

    fun saveExpense() {
        val state = _uiState.value
        val name = state.name.trim()
        val amount = state.amount.replace("$", "").replace(",", "").trim().toDoubleOrNull()

        if (name.isEmpty() || amount == null) {
            _uiState.value = state.copy(error = "Name and amount are required")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)
            try {
                val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                val parsedDate = dateFormat.parse(state.date)?.time ?: System.currentTimeMillis()

                val expense = ExpenseEntity(
                    userId = userId,
                    name = name,
                    amount = amount,
                    date = parsedDate,
                    time = state.time,
                    latitude = state.latitude,
                    longitude = state.longitude,
                    locationName = state.locationName.ifEmpty { null },
                    source = state.source,
                    receiptImagePath = state.receiptImageUri?.toString()
                )
                repository.insertExpense(expense)
                _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, error = e.message)
            }
        }
    }
}