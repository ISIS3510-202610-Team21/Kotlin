package com.example.spendantt.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat

@Composable
fun MonthlyExpensesCard(monthlyExpenses: Double) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "This month you have expended",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(
                text = "\$",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Red
            )
            Text(
                text = DecimalFormat("#,##0").format(monthlyExpenses),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Red,
                modifier = Modifier.padding(start = 4.dp)
            )
            Text(
                text = " ",
                fontSize = 16.sp
            )
            Text(
                text = "COP",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Red,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}