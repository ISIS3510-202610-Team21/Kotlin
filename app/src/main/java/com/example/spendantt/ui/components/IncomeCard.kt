package com.example.spendantt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spendantt.data.currency.CurrencyProvider
import com.example.spendantt.data.local.entity.IncomeEntity
import com.example.spendantt.data.local.entity.IncomeType
import com.example.spendantt.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Card de ingreso individual.
 * Muestra nombre, frecuencia y monto.
 * El color de fondo rota según el índice (amarillo, rosa, verde, azul)
 *
 * Ejemplo del diseño:
 * ┌─────────────────────────────────────┐
 * │ Parents Support          COP 400,500│
 * │ Every Month on 01                   │
 * └─────────────────────────────────────┘
 */
@Composable
fun IncomeCard(
    income: IncomeEntity,
    index: Int,                         // Para rotar colores
    modifier: Modifier = Modifier
) {
    val cardColor = IncomeCardColors[index % IncomeCardColors.size]

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardColor)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Nombre y frecuencia
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = income.name,
                color = SpendAntTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatFrequency(income),
                color = SpendAntTextSecondary,
                fontSize = 13.sp
            )
        }

        // Monto
        Text(
            text = formatAmount(income.amount),
            color = SpendAntTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Formatea la frecuencia como texto legible.
 * Ej: JUST_ONCE → "One time · 4/03/2026"
 *     FREQUENTLY (2 MONTHS) → "Every 2 Months"
 *     FREQUENTLY (1 MONTH, startDate day=1) → "Every Month on 01"
 */
private fun formatFrequency(income: IncomeEntity): String {
    return when (income.type) {
        IncomeType.JUST_ONCE -> {
            val sdf = SimpleDateFormat("M/dd/yyyy", Locale.getDefault())
            "One time · ${sdf.format(Date(income.startDate))}"
        }
        IncomeType.FREQUENTLY -> {
            val interval = income.recurrenceInterval ?: 1
            val unit = income.recurrenceUnit?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Month"
            if (interval == 1) {
                // Obtener día del mes para "Every Month on 01"
                val cal = Calendar.getInstance().apply { timeInMillis = income.startDate }
                val day = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
                "Every $unit on $day"
            } else {
                "Every $interval ${unit}s"
            }
        }
    }
}

private fun formatAmount(amount: Double): String {
    return CurrencyProvider.formatFromCOP(amount)
}
