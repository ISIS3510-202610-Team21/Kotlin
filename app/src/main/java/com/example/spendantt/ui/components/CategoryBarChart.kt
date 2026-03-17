package com.example.spendantt.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spendantt.ui.theme.SpendAntFontFamily

// Colores para las barras
private val categoryColors = listOf(
    Color(0xFF4A90E2),  // Azul (Food)
    Color(0xFFFF6B6B),  // Rojo (Transport)
    Color(0xFFFFA500),  // Naranja (Services)
    Color(0xFFE89DD6),  // Rosa (Other)
    Color(0xFFFFD700),  // Amarillo
    Color(0xFF7FB069),  // Verde
)

@Composable
fun CategoryBarChart(categoryExpenses: Map<String, Double>, modifier: Modifier = Modifier) {
    if (categoryExpenses.isEmpty()) {
        return
    }

    val maxAmount = categoryExpenses.values.maxOrNull() ?: 1.0
    val categories = categoryExpenses.entries.sortedByDescending { it.value }.take(4)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(24.dp))
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Text(
            text = "Where you spend most",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = SpendAntFontFamily,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            categories.forEachIndexed { index, (label, amount) ->
                BarItem(
                    label = label,
                    amount = amount,
                    maxAmount = maxAmount,
                    color = categoryColors[index % categoryColors.size],
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
fun BarItem(
    label: String,
    amount: Double,
    maxAmount: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    val heightPercentage = (amount / maxAmount).coerceIn(0.0, 1.0).toFloat()
    val emoji = when (label.lowercase()) {
        "food" -> "\uD83C\uDF54"
        "transport" -> "\uD83D\uDE8C"
        "services" -> "\uD83D\uDCA1"
        "other" -> "\uD83D\uDED2"
        else -> "\uD83D\uDCB0"
    }

    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(56.dp)
                .fillMaxHeight(heightPercentage)
                .background(color, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = emoji,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 14.dp)
            )
        }

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = SpendAntFontFamily,
            modifier = Modifier
                .padding(top = 8.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
}

