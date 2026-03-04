package com.example.spendantt.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * NUEVA ENTIDAD - Set a Goal (funcionalidad 7)
 *
 * El usuario define una meta de ahorro con:
 * - Cuánto quiere ahorrar
 * - Para qué (nombre)
 * - Cuándo quiere lograrlo (deadline)
 * La app calcula automáticamente cuánto debe ahorrar por día.
 *
 * Ejemplos del diseño:
 *   - "FEP 2026" → Deadline 4/03/2026 → 50% progreso
 *   - "New Car"  → Deadline 4/03/2026 → 20% progreso
 *
 * Fase 2: descomentar serverId y isSynced
 */
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val userId: Int,

    val name: String,               // Ej: "A new Car", "FEP 2026"
    val targetAmount: Double,       // Ej: 30200000.0 (cuánto quiere ahorrar)
    val currentAmount: Double = 0.0,// Cuánto lleva ahorrado (se actualiza con el tiempo)

    val deadline: Long,             // Timestamp de la fecha límite

    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),

    // Fase 2: descomentar cuando haya backend
    // val serverId: String? = null,
    // val isSynced: Boolean = false
) {
    /**
     * Calcula el progreso como porcentaje (0-100)
     * Usado en la pantalla de Goals para la barra de progreso
     */
    val progressPercent: Int
        get() = if (targetAmount > 0)
            ((currentAmount / targetAmount) * 100).toInt().coerceIn(0, 100)
        else 0

    /**
     * Calcula cuánto debe ahorrar por día para llegar a la meta
     * Usado en la pantalla "We have a plan"
     */
    fun dailySavingsNeeded(): Double {
        val remaining = targetAmount - currentAmount
        val daysLeft = ((deadline - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
        return if (daysLeft > 0) remaining / daysLeft else remaining
    }
}