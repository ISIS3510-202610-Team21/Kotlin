package com.example.spendantt.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.spendantt.R

private val NunitoFamily = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_italic, FontWeight.Normal, FontStyle.Italic)
)

val SpendAntFontFamily = NunitoFamily

private fun TextStyle.withNunito(): TextStyle = copy(
    fontFamily = NunitoFamily,
    fontSynthesis = FontSynthesis.All
)

val Typography = Typography().copy(
    displayLarge = Typography().displayLarge.withNunito(),
    displayMedium = Typography().displayMedium.withNunito(),
    displaySmall = Typography().displaySmall.withNunito(),
    headlineLarge = Typography().headlineLarge.withNunito(),
    headlineMedium = Typography().headlineMedium.withNunito(),
    headlineSmall = Typography().headlineSmall.withNunito(),
    titleLarge = Typography().titleLarge.withNunito(),
    titleMedium = Typography().titleMedium.withNunito(),
    titleSmall = Typography().titleSmall.withNunito(),
    bodyLarge = TextStyle(
        fontFamily = NunitoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSynthesis = FontSynthesis.All,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = Typography().bodyMedium.withNunito(),
    bodySmall = Typography().bodySmall.withNunito(),
    labelLarge = Typography().labelLarge.withNunito(),
    labelMedium = Typography().labelMedium.withNunito(),
    labelSmall = Typography().labelSmall.withNunito()
)