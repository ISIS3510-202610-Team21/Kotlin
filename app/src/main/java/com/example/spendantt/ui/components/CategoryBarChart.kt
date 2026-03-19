package com.example.spendantt.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spendantt.ui.theme.LabelIconMapper
import com.example.spendantt.ui.theme.SpendAntFontFamily

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
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            categories.forEach { (label, amount) ->
                BarItem(
                    label = label,
                    amount = amount,
                    maxAmount = maxAmount,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
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
    modifier: Modifier = Modifier
) {
    val heightPercentage = (amount / maxAmount).coerceIn(0.3, 1.0).toFloat()
    val iconRes = LabelIconMapper.getIconForLabel(label)
    val barColor = LabelIconMapper.getConsistentColorForLabel(label)
    val maxBarHeight = 140.dp

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(65.dp)
                .height(maxBarHeight * heightPercentage)
                .clip(RoundedCornerShape(14.dp))
                .background(barColor),
            contentAlignment = Alignment.TopCenter
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                tint = Color.Unspecified,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(28.dp)
            )
        }

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = SpendAntFontFamily,
            color = Color.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
