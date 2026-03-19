package com.example.spendantt.ui.theme

import androidx.compose.ui.graphics.Color

// ── COLORES PRINCIPALES SpendAnt ──────────────────────────────────────────────
val SpendAntGreen        = Color(0xFF44C669)   // Verde principal (headers, botones activos)
val SpendAntGreenDark    = Color(0xFF1A9E7D)   // Verde oscuro (pressed states)
val SpendAntGreenLight   = Color(0xFFE2F2E9)   // Verde claro (fondos de cards)

val SpendAntBlack        = Color(0xFF1A1A1A)   // Negro casi puro (botones primarios)
val SpendAntWhite        = Color(0xFFFFFFFF)
val SpendAntBackground   = Color(0xFFF5F5F5)   // Fondo general gris muy claro

// Colores de income cards (del diseño PDF - cards de color pastel)
val IncomeCardYellow     = Color(0xFFFFF9C4)   // Card "Parents Support"
val IncomeCardPink       = Color(0xFFFFCDD2)   // Card "Teaching Assistance"
val IncomeCardGreen      = Color(0xFFE2F2E9)   // Card genérico verde claro
val IncomeCardBlue       = Color(0xFFE3F2FD)   // Card genérico azul claro

// Texto
val SpendAntTextPrimary   = Color(0xFF1A1A1A)
val SpendAntTextSecondary = Color(0xFF757575)
val SpendAntTextOnGreen   = Color(0xFFFFFFFF)

// Error
val SpendAntError         = Color(0xFFE53935)

// Lista de colores para rotar en income cards
val IncomeCardColors = listOf(
    IncomeCardYellow,
    IncomeCardPink,
    IncomeCardGreen,
    IncomeCardBlue
)