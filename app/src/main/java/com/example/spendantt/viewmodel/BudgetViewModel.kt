package com.example.spendantt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spendantt.data.local.entity.IncomeEntity
import com.example.spendantt.data.local.entity.IncomeType
import com.example.spendantt.data.local.entity.RecurrenceUnit
import com.example.spendantt.data.repository.IncomeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class BudgetUiState(
    val incomes: List<IncomeEntity> = emptyList(),
    val totalIncome: Double = 0.0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class NewIncomeFormState(
    val name: String = "",
    val amount: String = "",
    val incomeType: IncomeType = IncomeType.JUST_ONCE,
    val recurrenceInterval: String = "1",
    val recurrenceUnit: RecurrenceUnit = RecurrenceUnit.MONTHS,
    val startDate: Long = System.currentTimeMillis(),
    val nameError: String? = null,
    val amountError: String? = null,
    val isSubmitting: Boolean = false
)

class BudgetViewModel(
    private val incomeRepository: IncomeRepository,
    private val userId: Int,
    private val firebaseUid: String? = null  // ← NUEVO
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(NewIncomeFormState())
    val formState: StateFlow<NewIncomeFormState> = _formState.asStateFlow()

    init {
        loadIncomes()
        loadTotalIncome()
    }

    private fun loadIncomes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            incomeRepository.getIncomesByUser(userId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
                }
                .collect { incomes ->
                    _uiState.value = _uiState.value.copy(incomes = incomes, isLoading = false)
                }
        }
    }

    private fun loadTotalIncome() {
        viewModelScope.launch {
            incomeRepository.getTotalIncome(userId)
                .catch { }
                .collect { total ->
                    _uiState.value = _uiState.value.copy(totalIncome = total ?: 0.0)
                }
        }
    }

    fun onNameChange(value: String) {
        _formState.value = _formState.value.copy(name = value, nameError = null)
    }

    fun onAmountChange(value: String) {
        val numericValue = value.filter(Char::isDigit)
        _formState.value = _formState.value.copy(amount = numericValue, amountError = null)
    }

    fun onIncomeTypeChange(type: IncomeType) {
        _formState.value = _formState.value.copy(incomeType = type)
    }

    fun onRecurrenceIntervalChange(value: String) {
        _formState.value = _formState.value.copy(recurrenceInterval = value)
    }

    fun onRecurrenceUnitChange(unit: RecurrenceUnit) {
        _formState.value = _formState.value.copy(recurrenceUnit = unit)
    }

    fun onStartDateChange(timestamp: Long) {
        _formState.value = _formState.value.copy(startDate = timestamp)
    }

    fun resetForm() {
        _formState.value = NewIncomeFormState()
    }

    fun saveIncome(onSuccess: () -> Unit) {
        val form = _formState.value

        var hasError = false
        if (form.name.isBlank()) {
            _formState.value = _formState.value.copy(nameError = "El nombre es requerido")
            hasError = true
        }
        val parsedAmount = form.amount.replace(",", "").toDoubleOrNull()
        if (parsedAmount == null || parsedAmount <= 0) {
            _formState.value = _formState.value.copy(amountError = "Ingresa un monto válido")
            hasError = true
        }
        if (hasError) return

        _formState.value = _formState.value.copy(isSubmitting = true)

        viewModelScope.launch {
            val income = IncomeEntity(
                userId = userId,
                name = form.name.trim(),
                amount = parsedAmount!!,
                type = form.incomeType,
                recurrenceInterval = if (form.incomeType == IncomeType.FREQUENTLY)
                    form.recurrenceInterval.toIntOrNull() ?: 1 else null,
                recurrenceUnit = if (form.incomeType == IncomeType.FREQUENTLY)
                    form.recurrenceUnit else null,
                startDate = form.startDate
            )

            // Pasar firebaseUid para sincronizar con Firestore
            val result = incomeRepository.insertIncome(income, firebaseUid)
            _formState.value = _formState.value.copy(isSubmitting = false)

            result.fold(
                onSuccess = {
                    resetForm()
                    _uiState.value = _uiState.value.copy(successMessage = "Ingreso guardado")
                    onSuccess()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(errorMessage = e.message)
                }
            )
        }
    }

    fun deleteIncome(income: IncomeEntity) {
        viewModelScope.launch {
            incomeRepository.deleteIncome(income, firebaseUid)
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}