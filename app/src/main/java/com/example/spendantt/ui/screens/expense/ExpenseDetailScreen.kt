package com.example.spendantt.ui.screens.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spendantt.ui.theme.SpendAntBlack
import com.example.spendantt.ui.theme.SpendAntFontFamily
import com.example.spendantt.ui.theme.SpendAntGreen
import com.example.spendantt.ui.theme.SpendAntWhite
import com.example.spendantt.viewmodel.ExpenseDetailViewModel
import java.util.Locale

@Composable
fun ExpenseDetailScreen(
    viewModel: ExpenseDetailViewModel,
    onBack: () -> Unit,
    onDeleted: () -> Unit
) {
    val uiState by viewModel.uiState
    val context = LocalContext.current
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onDeleted()
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            viewModel.consumeSaved()
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpendAntWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 92.dp)
        ) {
            DetailTopBar(
                title = "Expense Detail",
                onClose = onBack,
                onConfirm = { viewModel.saveExpense() }
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SpendAntGreen)
                }
            } else {
                val expenseWithLabels = uiState.expense
                if (expenseWithLabels == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.error ?: "Expense not found",
                            color = SpendAntBlack,
                            fontFamily = SpendAntFontFamily
                        )
                    }
                } else {
                    val labelsText = expenseWithLabels.labels.joinToString { it.name }.ifEmpty { "No label" }

                    Spacer(modifier = Modifier.height(96.dp))

                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EditableDetailField(
                            value = uiState.name,
                            placeholder = "Expense name",
                            onValueChange = viewModel::onNameChange
                        )

                        EditableDetailField(
                            value = uiState.amount,
                            placeholder = "$ 0",
                            onValueChange = viewModel::onAmountChange
                        )

                        RegretExpenseReadonlyRow(
                            checked = uiState.regretExpense,
                            onCheckedChange = viewModel::onRegretExpenseChange
                        )

                        Box(
                            modifier = Modifier
                                .clickable(enabled = false) {}
                                .background(SpendAntGreen, RoundedCornerShape(50.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = labelsText,
                                color = SpendAntBlack,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = SpendAntFontFamily,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(40.dp))

                        Button(
                            onClick = { showDeleteConfirmation = true },
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SpendAntBlack,
                                contentColor = SpendAntWhite
                            ),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                text = "Delete Expense",
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = SpendAntFontFamily
                            )
                        }

                        uiState.error?.let { error ->
                            Text(
                                text = error,
                                color = Color.Red,
                                fontFamily = SpendAntFontFamily,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        if (uiState.isDeleting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SpendAntGreen)
            }
        }

        ExpenseBottomMetaBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            date = uiState.date.ifEmpty { "Select date" },
            time = uiState.time.ifEmpty { "Select time" },
            location = uiState.locationName.ifEmpty { "Pick location" },
            onDateClick = {
                showDatePicker(
                    context = context,
                    initialDate = uiState.date,
                    onDateSelected = viewModel::onDateSelected
                )
            },
            onTimeClick = {
                showTimePicker(
                    context = context,
                    initialTime = uiState.time,
                    onTimeSelected = viewModel::onTimeSelected
                )
            },
            onLocationClick = {}
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            containerColor = SpendAntWhite,
            title = {
                Text(
                    text = "Delete expense?",
                    color = SpendAntBlack,
                    fontFamily = SpendAntFontFamily
                )
            },
            text = {
                Text(
                    text = "This action will permanently remove the selected expense.",
                    color = SpendAntBlack,
                    fontFamily = SpendAntFontFamily
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.deleteExpense()
                    }
                ) {
                    Text("Delete", color = Color.Red, fontFamily = SpendAntFontFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel", color = SpendAntBlack, fontFamily = SpendAntFontFamily)
                }
            }
        )
    }
}

@Composable
private fun EditableDetailField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(0.dp),
                clip = false
            )
            .background(Color(0xFFE2F8E4))
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    placeholder,
                    color = Color(0xFF6D6D6D),
                    fontFamily = SpendAntFontFamily
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp),
            colors = expenseEntryFieldColors()
        )
    }
}

@Composable
private fun RegretExpenseReadonlyRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = SpendAntGreen,
                uncheckedColor = SpendAntBlack,
                checkmarkColor = SpendAntBlack,
            )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Do you regret this expense?",
            color = SpendAntBlack,
            fontFamily = SpendAntFontFamily,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun DetailTopBar(
    title: String,
    onClose: () -> Unit,
    onConfirm: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SpendAntGreen)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = SpendAntBlack
            )
        }
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = SpendAntFontFamily,
            color = SpendAntBlack,
            modifier = Modifier.align(Alignment.Center)
        )
        IconButton(
            onClick = onConfirm,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Text(
                text = "✓",
                color = SpendAntBlack,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
