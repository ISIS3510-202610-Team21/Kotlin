package com.example.spendantt.ui.screens.report

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.spendantt.data.report.CategoryTotal
import com.example.spendantt.data.report.FinancialReport
import com.example.spendantt.data.report.ReportPdfGenerator
import com.example.spendantt.data.report.TopExpense
import com.example.spendantt.ui.theme.SpendAntBlack
import com.example.spendantt.ui.theme.SpendAntGreen
import com.example.spendantt.viewmodel.ReportViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PastelGreen = Color(0xFFE0FFE9)
private val GrayText = Color(0xFF5E5E5E)
private val AmountRed = Color(0xFFF04C4C)
private val CategoryColors = listOf(
    Color(0xFF44C669),
    Color(0xFF4FC3F7),
    Color(0xFFBA68C8),
    Color(0xFFFF8A65),
    Color(0xFFAED581),
)

@Composable
fun ReportViewScreen(
    viewModel: ReportViewModel,
    onClose: () -> Unit,
) {
    val report by viewModel.currentReport.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }

    val r = report ?: return

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.Center),
            ) {
                Text(text = "Spending Report", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(text = r.periodLabel, color = GrayText, fontSize = 11.sp)
            }
            IconButton(
                onClick = {
                    if (isExporting) return@IconButton
                    isExporting = true
                    scope.launch {
                        try {
                            val file = ReportPdfGenerator(context.applicationContext).generate(r)
                            openPdf(context, file)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not export PDF: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isExporting = false
                        }
                    }
                },
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.Black)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item { TotalCard(report = r) }
            item { SectionTitle("Daily Spending") }
            item { DailyHistogram(report = r) }
            item { SectionTitle("Top Expenses") }
            itemsIndexed(r.topExpenses) { _, exp -> TopExpenseRow(expense = exp, report = r) }
            item { SectionTitle("Top Categories") }
            itemsIndexed(r.topCategories) { index, cat ->
                CategoryRow(cat = cat, total = r.topCategories.sumOf { it.amountCop }.coerceAtLeast(1.0), color = CategoryColors[index % CategoryColors.size], report = r)
            }
            if (r.bqInsights.isNotEmpty()) {
                item { Spacer(modifier = Modifier.height(4.dp)) }
                item { InsightsCard(insights = r.bqInsights) }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = SpendAntBlack,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun TotalCard(report: FinancialReport) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PastelGreen)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(text = "Total spent", color = GrayText, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatDisplay(report.totalSpentCop, report),
            color = SpendAntBlack,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
        )
    }
}

@Composable
private fun DailyHistogram(report: FinancialReport) {
    val daily = report.dailySpends
    if (daily.isEmpty()) return
    val maxAmount = daily.maxOf { it.amountCop }.coerceAtLeast(1.0)

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
        ) {
            val n = daily.size
            val spacing = 6f
            val available = size.width - spacing * (n - 1)
            val barWidth = (available / n).coerceAtLeast(2f)
            val baseY = size.height
            daily.forEachIndexed { index, ds ->
                val ratio = (ds.amountCop / maxAmount).toFloat().coerceIn(0f, 1f)
                val barHeight = ratio * size.height
                val x = index * (barWidth + spacing)
                drawRect(
                    color = SpendAntGreen,
                    topLeft = Offset(x, baseY - barHeight),
                    size = Size(barWidth, barHeight),
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            val maxLabels = 6
            val step = (daily.size / maxLabels).coerceAtLeast(1)
            daily.forEachIndexed { index, ds ->
                if (index % step == 0) {
                    Text(
                        text = ds.date.dayOfMonth.toString(),
                        color = GrayText,
                        fontSize = 10.sp,
                        modifier = Modifier.weight(step.toFloat()),
                    )
                }
            }
        }
    }
}

@Composable
private fun TopExpenseRow(expense: TopExpense, report: FinancialReport) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PastelGreen)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.name.ifBlank { "Unnamed expense" },
                color = SpendAntBlack,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 2,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = expense.category, color = GrayText, fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = formatDisplay(expense.amountCop, report),
            color = AmountRed,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun CategoryRow(cat: CategoryTotal, total: Double, color: Color, report: FinancialReport) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = cat.label,
                color = SpendAntBlack,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatDisplay(cat.amountCop, report),
                color = GrayText,
                fontSize = 13.sp,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        val ratio = (cat.amountCop / total).toFloat().coerceIn(0f, 1f)
        Canvas(modifier = Modifier.fillMaxWidth().height(8.dp)) {
            drawRect(color = Color(0xFFE0E0E0), size = size)
            drawRect(color = color, size = Size(size.width * ratio, size.height))
        }
    }
}

@Composable
private fun InsightsCard(insights: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PastelGreen)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = "Insights", color = SpendAntBlack, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        insights.forEach {
            Text(text = it, color = SpendAntBlack, fontSize = 12.sp)
        }
    }
}

private val largeFormatter = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))
private val smallFormatter = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

private fun formatDisplay(amountCop: Double, report: FinancialReport): String {
    val value = amountCop * report.displayRate
    val formatted = if (value >= 100) largeFormatter.format(value) else smallFormatter.format(value)
    return "${report.displayCurrency} $formatted"
}

private fun openPdf(context: Context, file: File) {
    val authority = "${context.packageName}.provider"
    val uri = FileProvider.getUriForFile(context, authority, file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(intent, "Open PDF with").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}
