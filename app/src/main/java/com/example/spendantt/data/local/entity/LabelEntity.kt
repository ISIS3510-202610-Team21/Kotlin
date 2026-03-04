package com.example.spendantt.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tabla de etiquetas/categorías de gastos.
 * Ejemplos: "Food", "Transport", "Academic Essentials", "Lifestyle & Social"
 * Fase 2: Sincronizar con backend → agregar "serverId" y "isSynced"
 */
@Entity(tableName = "labels")
data class LabelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,               // Ej: "Food", "Transport"
    val iconEmoji: String? = null,  // Ej: "🍔", "🚗" (opcional)
    val colorHex: String? = null,   // Ej: "#FF5733" (opcional)

    val userId: Int,                // A qué usuario pertenece esta etiqueta

    val createdAt: Long = System.currentTimeMillis(),

    // Fase 2: descomentar cuando haya backend
    // val serverId: String? = null,
    // val isSynced: Boolean = false
)
