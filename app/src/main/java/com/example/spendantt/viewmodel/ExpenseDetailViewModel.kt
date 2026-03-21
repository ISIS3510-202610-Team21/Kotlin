package com.example.spendantt.viewmodel

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spendantt.data.local.AppDatabase
import com.example.spendantt.data.local.entity.ExpenseEntity
import com.example.spendantt.data.local.entity.ExpenseWithLabels
import com.example.spendantt.data.repository.ExpenseRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ExpenseDetailUiState(
    val expense: ExpenseWithLabels? = null,
    val name: String = "",
    val amount: String = "",
    val date: String = "",
    val time: String = "",
    val locationName: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val regretExpense: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

class ExpenseDetailViewModel(
    context: Context,
    private val expenseId: Int,
    private val firebaseUid: String?
) : ViewModel() {

    private val database = AppDatabase.getInstance(context)
    private val repository = ExpenseRepository(database.expenseDao(), database.labelDao())

    private val _uiState = mutableStateOf(ExpenseDetailUiState())
    val uiState: State<ExpenseDetailUiState> = _uiState

    init {
        loadExpense()
    }

    fun loadExpense() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val expense = repository.getExpenseById(expenseId)
            _uiState.value = _uiState.value.copy(
                expense = expense,
                name = expense?.expense?.name.orEmpty(),
                amount = expense?.expense?.amount?.let { formatAmount(it) }.orEmpty(),
                date = expense?.expense?.date?.let(::formatDate).orEmpty(),
                time = expense?.expense?.time.orEmpty(),
                locationName = expense?.expense?.locationName.orEmpty(),
                latitude = expense?.expense?.latitude,
                longitude = expense?.expense?.longitude,
                regretExpense = expense?.expense?.isRecurring ?: false,
                isLoading = false,
                error = if (expense == null) "Expense not found" else null
            )
        }
    }

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(name = value)
    }

    fun onAmountChange(value: String) {
        val sanitized = value.filter { it.isDigit() || it == '.' || it == ',' }
        _uiState.value = _uiState.value.copy(amount = sanitized)
    }

    fun onRegretExpenseChange(value: Boolean) {
        _uiState.value = _uiState.value.copy(regretExpense = value)
    }

    fun onDateSelected(timestamp: Long) {
        _uiState.value = _uiState.value.copy(date = formatDate(timestamp))
    }

    fun onTimeSelected(hourOfDay: Int, minute: Int) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
        }
        val timeFormat = SimpleDateFormat("hh:mma", Locale.getDefault())
        _uiState.value = _uiState.value.copy(time = timeFormat.format(calendar.time))
    }

    fun saveExpense() {
        val state = _uiState.value
        val original = state.expense?.expense ?: return
        val amount = state.amount.replace("$", "").replace(",", "").trim().toDoubleOrNull()
        if (state.name.trim().isEmpty() || amount == null) {
            _uiState.value = state.copy(error = "Name and amount are required")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)
            val parsedDate = parseDate(state.date) ?: original.date
            val updatedExpense = original.copy(
                name = state.name.trim(),
                amount = amount,
                date = parsedDate,
                time = state.time,
                locationName = state.locationName.ifEmpty { null },
                latitude = state.latitude,
                longitude = state.longitude,
                isRecurring = state.regretExpense
            )
            val labelIds = state.expense?.labels?.map { it.id } ?: emptyList()
            val result = repository.updateExpense(updatedExpense, labelIds, firebaseUid)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        expense = state.expense?.copy(expense = updatedExpense),
                        isSaving = false,
                        isSaved = true
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = error.message ?: "Unable to update expense"
                    )
                }
            )
        }
    }

    fun deleteExpense() {
        val expense = _uiState.value.expense?.expense ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true, error = null)
            val result = repository.deleteExpense(expense, firebaseUid)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        isDeleted = true
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        error = error.message ?: "Unable to delete expense"
                    )
                }
            )
        }
    }

    fun consumeSaved() {
        _uiState.value = _uiState.value.copy(isSaved = false)
    }

    private fun formatAmount(amount: Double): String =
        java.text.DecimalFormat("#,##0").format(amount)

    private fun formatDate(timestamp: Long): String =
        SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(timestamp)

    private fun parseDate(value: String): Long? {
        return try {
            SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).parse(value)?.time
        } catch (_: Exception) {
            null
        }
    }
}
