package com.example.spendantt.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * CAMBIOS vs versión anterior:
 * + isRecurring: Boolean         → gasto recurrente (Manual Logging)
 * + recurrenceInterval: Int?     → cada cuántas unidades se repite
 * + recurrenceUnit: String?      → "Days", "Weeks", "Months"
 * + nextOccurrenceDate: Long?    → cuándo vuelve a ocurrir
 *
 * Fase 2: descomentar serverId y isSynced
 */
@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val userId: Int,

    val name: String,
    val amount: Double,

    val date: Long,
    val time: String,

    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null,

    val source: ExpenseSource = ExpenseSource.MANUAL,
    val receiptImagePath: String? = null,
    val isPendingCategory: Boolean = false,

    // ── RECURRENCIA (Manual Logging) ──────────────────────────
    val isRecurring: Boolean = false,
    val recurrenceInterval: Int? = null,    // Ej: 2
    val recurrenceUnit: RecurrenceUnit? = null, // Ej: WEEKS → "cada 2 semanas"
    val nextOccurrenceDate: Long? = null,   // Timestamp del próximo gasto automático

    val createdAt: Long = System.currentTimeMillis(),

    // Fase 2: descomentar cuando haya backend
    // val serverId: String? = null,
    // val isSynced: Boolean = false
)

enum class ExpenseSource {
    MANUAL,
    OCR,
    GOOGLE_PAY   // Fase 2
}

enum class RecurrenceUnit {
    DAYS,
    WEEKS,
    MONTHS
}