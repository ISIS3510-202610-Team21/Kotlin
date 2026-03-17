package com.example.spendantt.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spendantt.data.local.entity.ExpenseWithLabels
import com.example.spendantt.ui.theme.SpendAntFontFamily
import java.text.DecimalFormat

@Composable
fun ExpenseListItem(expense: ExpenseWithLabels, modifier: Modifier = Modifier) {
    val firstLabel = expense.labels.firstOrNull()
    val labelName = firstLabel?.name ?: "Other"
    val labelEmoji = firstLabel?.iconEmoji ?: "💰"
    val backgroundColor = stringToColor(labelName)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(backgroundColor, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = labelEmoji,
                fontSize = 22.sp
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = expense.expense.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = SpendAntFontFamily,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = labelName,
                fontSize = 13.sp,
                fontFamily = SpendAntFontFamily,
                color = Color(0xFF5F6368)
            )
        }

        Text(
            text = "COP ${DecimalFormat("#,##0").format(expense.expense.amount)}",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = SpendAntFontFamily,
            color = Color.Black
        )
    }
}

// Función auxiliar para generar colores basados en el nombre
fun stringToColor(input: String): Color {
    val colors = listOf(
        Color(0xFF4A90E2),  // Azul
        Color(0xFFFF6B6B),  // Rojo
        Color(0xFFFFA500),  // Naranja
        Color(0xFFE89DD6),  // Rosa
        Color(0xFFFFD700),  // Amarillo
        Color(0xFF7FB069),  // Verde
    )
    val hash = input.hashCode()
    return colors[kotlin.math.abs(hash) % colors.size]
}