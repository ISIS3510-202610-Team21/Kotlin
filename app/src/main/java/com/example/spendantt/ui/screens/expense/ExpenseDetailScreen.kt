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
import coil.compose.AsyncImage
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spendantt.ui.screens.labels.LabelsScreen
import com.example.spendantt.ui.theme.SpendAntBlack
import com.example.spendantt.ui.theme.SpendAntFontFamily
import com.example.spendantt.ui.theme.SpendAntGreen
import com.example.spendantt.ui.theme.SpendAntWhite
import com.example.spendantt.viewmodel.ExpenseDetailViewModel
import com.google.android.gms.maps.model.LatLng
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
    var isMapPickerOpen by remember { mutableStateOf(false) }
    var showLabelEditor by remember { mutableStateOf(false) }

    // Label editor (accessible via tapping the label badge)
    if (showLabelEditor) {
        LabelsScreen(
            labelsGroupedByCategory = uiState.labelsGroupedByCategory,
            selectedLabelIds = uiState.selectedLabelIds,
            onLabelToggle = { viewModel.toggleLabel(it) },
            onDone = { showLabelEditor = false },
            onClose = { showLabelEditor = false }
        )
        return
    }

    // Show label picker when expense has no category (e.g. imported from Bold notification)
    if (uiState.showLabelPicker) {
        BoldLabelPromptScreen(
            expenseName = uiState.name,
            labelsGroupedByCategory = uiState.labelsGroupedByCategory,
            selectedLabelIds = uiState.selectedLabelIds,
            isSaving = uiState.isSavingLabel,
            onLabelToggle = { viewModel.toggleLabel(it) },
            onDone = { viewModel.savePendingLabel() },
            onDismiss = { viewModel.dismissLabelPicker() }
        )
        return
    }

    if (isMapPickerOpen) {
        MapPickerScreen(
            initialLatitude = uiState.latitude,
            initialLongitude = uiState.longitude,
            onClose = { isMapPickerOpen = false },
            onConfirm = { latLng: LatLng ->
                viewModel.onLocationSelected(latLng.latitude, latLng.longitude)
                isMapPickerOpen = false
            }
        )
        return
    }

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
                    val labelsText = uiState.allLabels
                        .filter { it.id in uiState.selectedLabelIds }
                        .joinToString { it.name }
                        .ifEmpty { "No label — tap to add" }

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

                        // CACHING | William | 5pts | Coil AsyncImage: muestra el recibo adjunto al
                        // gasto (ruta local o URL de Cloudinary). Coil mantiene un cache de memoria y
                        // disco, evitando decodificar o re-descargar la imagen en cada recomposición.
                        uiState.receiptImagePath?.let { path ->
                            AsyncImage(
                                model = path,
                                contentDescription = "Receipt image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .background(
                                        Color(0xFFF5F5F5),
                                        RoundedCornerShape(12.dp)
                                    )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clickable { showLabelEditor = true }
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
            onLocationClick = { isMapPickerOpen = true }
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
private fun BoldLabelPromptScreen(
    expenseName: String,
    labelsGroupedByCategory: Map<String, List<com.example.spendantt.data.local.entity.LabelEntity>>,
    selectedLabelIds: Set<Int>,
    isSaving: Boolean,
    onLabelToggle: (com.example.spendantt.data.local.entity.LabelEntity) -> Unit,
    onDone: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpendAntWhite)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SpendAntGreen)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = SpendAntBlack
                    )
                }
                Text(
                    text = "Add Label",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SpendAntFontFamily,
                    color = SpendAntBlack,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Hey! We couldn't categorize \"$expenseName\".",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SpendAntFontFamily,
                    color = SpendAntBlack,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Add a label so we can learn for next time!",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    fontFamily = SpendAntFontFamily,
                    textAlign = TextAlign.Center
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                LabelsScreen(
                    labelsGroupedByCategory = labelsGroupedByCategory,
                    selectedLabelIds = selectedLabelIds,
                    onLabelToggle = onLabelToggle,
                    onDone = onDone,
                    onClose = onDismiss,
                    showHeader = false
                )
            }
        }

        if (isSaving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SpendAntGreen)
            }
        }
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
