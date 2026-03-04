package com.example.spendantt.data.local.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * Clase helper para obtener un gasto junto con todas sus etiquetas.
 * NO es una tabla, Room la usa para hacer el JOIN automáticamente.
 *
 * Uso en DAO:
 * @Transaction
 * @Query("SELECT * FROM expenses WHERE id = :expenseId")
 * fun getExpenseWithLabels(expenseId: Int): ExpenseWithLabels
 */
data class ExpenseWithLabels(
    @Embedded
    val expense: ExpenseEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ExpenseLabelCrossRef::class,
            parentColumn = "expenseId",
            entityColumn = "labelId"
        )
    )
    val labels: List<LabelEntity>
)