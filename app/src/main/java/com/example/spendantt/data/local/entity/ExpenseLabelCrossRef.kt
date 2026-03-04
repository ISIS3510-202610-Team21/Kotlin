package com.example.spendantt.data.local.entity

import androidx.room.Entity

/**
 * Tabla intermedia: relación muchos a muchos entre Expense y Label
 * Un gasto puede tener varias etiquetas → "Uber Ride" puede ser "Transport" y "Daily"
 * Una etiqueta puede estar en varios gastos
 */
@Entity(
    tableName = "expense_label_cross_ref",
    primaryKeys = ["expenseId", "labelId"]
)
data class ExpenseLabelCrossRef(
    val expenseId: Int,
    val labelId: Int
)