package com.example.spendantt.ui.screens.report

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spendantt.data.local.entity.ReportCacheEntity
import com.example.spendantt.ui.theme.SpendAntBlack
import com.example.spendantt.ui.theme.SpendAntGreen
import com.example.spendantt.ui.theme.SpendAntTextSecondary
import com.example.spendantt.ui.theme.SpendAntWhite
import com.example.spendantt.viewmodel.ReportViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FieldBg = Color(0xFFE0FFE9)
private val GrayText = Color(0xFF5E5E5E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportSetupScreen(
    viewModel: ReportViewModel,
    onClose: () -> Unit,
) {
    val state by viewModel.setup.collectAsState()

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpendAntWhite),
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpendAntGreen)
                .padding(horizontal = 8.dp, vertical = 14.dp),
        ) {
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Black)
            }
            Text(
                text = "Spending Report",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Select period",
                color = SpendAntBlack,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )

            DateField(
                label = "Start",
                date = state.startDate,
                onClick = { showStartPicker = true },
            )

            DateField(
                label = "End",
                date = state.endDate,
                onClick = { showEndPicker = true },
            )

            val cachedRow = state.cachedRowForCurrentRange
            if (cachedRow != null) {
                ViewThisReportCard(
                    row = cachedRow,
                    onClick = { viewModel.openCached(cachedRow) },
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = { viewModel.generate() },
                enabled = !state.isGenerating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpendAntBlack,
                    contentColor = SpendAntWhite,
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isGenerating) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "Generate Report", fontWeight = FontWeight.Bold)
                }
            }

            val others = state.recentReports.filter {
                it.startEpochDay != state.startDate.toEpochDay() ||
                    it.endEpochDay != state.endDate.toEpochDay()
            }
            if (others.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Other recent reports",
                    color = GrayText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    others.forEach { row ->
                        RecentReportCard(
                            row = row,
                            onClick = { viewModel.openCached(row) },
                        )
                    }
                }
            }
        }
    }

    if (showStartPicker) {
        val initial = state.startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        viewModel.onStartDateChange(ReportViewModel.millisToLocalDate(millis))
                    }
                    showStartPicker = false
                }) { Text("OK", color = SpendAntGreen) }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) {
                    Text("Cancel", color = SpendAntTextSecondary)
                }
            },
        ) {
            DatePicker(
                state = pickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = SpendAntGreen,
                    todayDateBorderColor = SpendAntGreen,
                ),
            )
        }
    }

    if (showEndPicker) {
        val initial = state.endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        viewModel.onEndDateChange(ReportViewModel.millisToLocalDate(millis))
                    }
                    showEndPicker = false
                }) { Text("OK", color = SpendAntGreen) }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) {
                    Text("Cancel", color = SpendAntTextSecondary)
                }
            },
        ) {
            DatePicker(
                state = pickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = SpendAntGreen,
                    todayDateBorderColor = SpendAntGreen,
                ),
            )
        }
    }
}

@Composable
private fun DateField(label: String, date: LocalDate, onClick: () -> Unit) {
    val text = date.format(DateTimeFormatter.ofPattern("EEE, MMM d yyyy", Locale.ENGLISH))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FieldBg, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = GrayText, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = GrayText, fontSize = 11.sp)
            Text(text = text, color = SpendAntBlack, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = GrayText)
    }
}

@Composable
private fun ViewThisReportCard(row: ReportCacheEntity, onClick: () -> Unit) {
    val time = java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date(row.generatedAt))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, SpendAntGreen), RoundedCornerShape(10.dp))
            .background(Color(0x1A44C669), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SpendAntGreen)
        Spacer(modifier = Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "View this report", color = SpendAntBlack, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(text = "Generated at $time · tap to open", color = GrayText, fontSize = 11.sp)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = SpendAntBlack)
    }
}

@Composable
private fun RecentReportCard(row: ReportCacheEntity, onClick: () -> Unit) {
    val start = LocalDate.ofEpochDay(row.startEpochDay)
    val end = LocalDate.ofEpochDay(row.endEpochDay)
    val periodLabel = "${start.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))} – ${end.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH))}"
    val time = java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date(row.generatedAt))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FieldBg, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.AutoMirrored.Filled.Article, contentDescription = null, tint = SpendAntBlack, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = periodLabel, color = SpendAntBlack, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(text = "Generated at $time", color = GrayText, fontSize = 11.sp)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = GrayText)
    }
}

