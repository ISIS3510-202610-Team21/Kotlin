package com.example.spendantt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spendantt.ui.theme.SpendAntFontFamily

@Composable
fun NoInternetBanner(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    if (!visible) return
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(vertical = 10.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "There is no internet connection available!",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = SpendAntFontFamily,
            textAlign = TextAlign.Center
        )
    }
}
