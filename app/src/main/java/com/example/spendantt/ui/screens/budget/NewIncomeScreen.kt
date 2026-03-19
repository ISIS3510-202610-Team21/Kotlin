package com.example.spendantt.ui.screens.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spendantt.data.local.entity.IncomeType
import com.example.spendantt.data.local.entity.RecurrenceUnit
import com.example.spendantt.ui.components.*
import com.example.spendantt.ui.theme.*
import com.example.spendantt.viewmodel.BudgetViewModel
import java.text.SimpleDateFormat
import java.util.*

// Color gris claro para botones no seleccionados
private val ToggleUnselectedBg = Color(0xFFE0E0E0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewIncomeScreen(
    viewModel: BudgetViewModel,
    onClose: () -> Unit,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formState by viewModel.formState.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = formState.startDate
    )
    val formattedDate = remember(formState.startDate) {
        SimpleDateFormat("M/dd/yyyy", Locale.getDefault()).format(Date(formState.startDate))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpendAntWhite)
    ) {

        // ── HEADER ────────────────────────────────────────────
        SpendAntHeader(
            title = "New Income",
            onClose = onClose,
            onConfirm = { viewModel.saveIncome(onSuccess = onSuccess) }
        )

        // ── FORMULARIO ────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Campo: Nombre
            SpendAntTextField(
                value = formState.name,
                onValueChange = viewModel::onNameChange,
                placeholder = "Income name",
                errorMessage = formState.nameError
            )

            // Campo: Monto
            SpendAntTextField(
                value = formState.amount,
                onValueChange = viewModel::onAmountChange,
                placeholder = "32,000",
                leadingText = "$",
                errorMessage = formState.amountError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // ── TYPE OF INCOME ────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Type of Income",
                    color = SpendAntTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "Just Once" to IncomeType.JUST_ONCE,
                        "Frequently" to IncomeType.FREQUENTLY
                    ).forEach { (label, type) ->
                        ToggleChip(
                            label = label,
                            isSelected = formState.incomeType == type,
                            onClick = { viewModel.onIncomeTypeChange(type) }
                        )
                    }
                }
            }

            // ── RECURRENCIA ───────────────────────────────────
            if (formState.incomeType == IncomeType.FREQUENTLY) {
                RecurrenceSelector(
                    interval = formState.recurrenceInterval,
                    unit = formState.recurrenceUnit,
                    onIntervalChange = viewModel::onRecurrenceIntervalChange,
                    onUnitChange = viewModel::onRecurrenceUnitChange
                )
            }

            // ── FECHA CENTRADA ────────────────────────────────
            Row(
                modifier = Modifier
                    .clickable { showDatePicker = true }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = SpendAntTextSecondary
                )
                Text(
                    text = formattedDate,
                    fontSize = 15.sp,
                    color = SpendAntTextPrimary
                )
            }
        }

        // ── BOTÓN GUARDAR ─────────────────────────────────────
        Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            SpendAntButton(
                text = "Save",
                onClick = { viewModel.saveIncome(onSuccess = onSuccess) },
                isLoading = formState.isSubmitting
            )
        }
    }

    // ── DATE PICKER ───────────────────────────────────────────
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.onStartDateChange(it) }
                    showDatePicker = false
                }) { Text("OK", color = SpendAntGreen) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = SpendAntTextSecondary)
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = SpendAntGreen,
                    todayDateBorderColor = SpendAntGreen
                )
            )
        }
    }
}

// ── CHIP TOGGLE (gris/verde) ──────────────────────────────────────────────────

@Composable
private fun ToggleChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(if (isSelected) SpendAntGreen else ToggleUnselectedBg)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) SpendAntWhite else SpendAntBlack,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── RECURRENCE SELECTOR ───────────────────────────────────────────────────────

@Composable
private fun RecurrenceSelector(
    interval: String,
    unit: RecurrenceUnit,
    onIntervalChange: (String) -> Unit,
    onUnitChange: (RecurrenceUnit) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val unitOptions = listOf(
        RecurrenceUnit.DAYS to "Days",
        RecurrenceUnit.WEEKS to "Weeks",
        RecurrenceUnit.MONTHS to "Months"
    )
    val selectedLabel = unitOptions.first { it.first == unit }.second
    val hasSelection = true // siempre hay uno seleccionado por defecto

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Every", color = SpendAntTextSecondary, fontSize = 15.sp)

        // Campo número más chiquito
        OutlinedTextField(
            value = interval,
            onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) onIntervalChange(it) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SpendAntGreen,
                focusedContainerColor = SpendAntGreenLight,
                unfocusedContainerColor = SpendAntGreenLight,
                unfocusedBorderColor = SpendAntGreenLight,
                focusedTextColor = SpendAntBlack,
                unfocusedTextColor = SpendAntBlack,
                cursorColor = SpendAntBlack
            ),
            modifier = Modifier.width(52.dp)  // más chiquito
        )

        // Dropdown como ToggleChip
        Box {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(SpendAntGreen)  // siempre verde porque siempre hay selección
                    .clickable { expanded = true }
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = selectedLabel,
                    color = SpendAntWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = SpendAntWhite
            ) {
                unitOptions.forEach { (recUnit, label) ->
                    DropdownMenuItem(
                        text = { Text(label, color = SpendAntBlack) },
                        onClick = { onUnitChange(recUnit); expanded = false }
                    )
                }
            }
        }
    }
}

