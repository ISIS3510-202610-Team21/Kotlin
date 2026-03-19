package com.example.spendantt.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spendantt.data.local.entity.ExpenseWithLabels
import com.example.spendantt.ui.theme.LabelIconMapper
import com.example.spendantt.ui.theme.SpendAntFontFamily
import java.text.DecimalFormat

@Composable
fun ExpenseListItem(expense: ExpenseWithLabels, modifier: Modifier = Modifier) {
    val firstLabel = expense.labels.firstOrNull()
    val labelName = firstLabel?.name ?: "Other"
    val category = firstLabel?.category
    val iconRes = LabelIconMapper.getIconForLabel(labelName)
    val backgroundColor = LabelIconMapper.getColorForCategory(category)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Icono circular con fondo de color
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(backgroundColor.copy(alpha = 0.15f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = labelName,
                tint = backgroundColor,
                modifier = Modifier.size(24.dp)
            )
        }

        // Información del gasto
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp, end = 12.dp)
        ) {
            Text(
                text = expense.expense.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = SpendAntFontFamily,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = labelName,
                fontSize = 13.sp,
                fontFamily = SpendAntFontFamily,
                color = Color(0xFF6B7280),
                fontWeight = FontWeight.Normal
            )
        }

        // Monto
        Text(
            text = "$${DecimalFormat("#,##0").format(expense.expense.amount)}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SpendAntFontFamily,
            color = Color.Black
        )
    }
}