package com.example.spendantt.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * NUEVA ENTIDAD - Set a Budget (funcionalidad 6)
 *
 * El usuario registra sus fuentes de ingreso.
 * Puede ser un ingreso único ("Just Once") o recurrente ("Frequently")
 * Ejemplos del diseño:
 *   - "Parents Support" → Every Month on 01 → COP 400,500
 *   - "Teaching Assistance" → Every two months → COP 800,500
 *
 * Fase 2: descomentar serverId y isSynced
 */
@Entity(tableName = "incomes")
data class IncomeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val userId: Int,

    val name: String,               // Ej: "Parents Support"
    val amount: Double,             // Ej: 400500.0

    val type: IncomeType,           // JUST_ONCE o FREQUENTLY

    // Solo aplica si type == FREQUENTLY
    val recurrenceInterval: Int? = null,        // Ej: 2
    val recurrenceUnit: RecurrenceUnit? = null, // Ej: MONTHS → "cada 2 meses"
    val nextOccurrenceDate: Long? = null,       // Timestamp del próximo ingreso

    val startDate: Long,            // Fecha de inicio del ingreso
    val createdAt: Long = System.currentTimeMillis(),

    // Fase 2: descomentar cuando haya backend
    // val serverId: String? = null,
    // val isSynced: Boolean = false
)

/**
 * JUST_ONCE   → ingreso único, no se repite
 * FREQUENTLY  → ingreso recurrente (semanal, mensual, etc.)
 */
enum class IncomeType {
    JUST_ONCE,
    FREQUENTLY
}
