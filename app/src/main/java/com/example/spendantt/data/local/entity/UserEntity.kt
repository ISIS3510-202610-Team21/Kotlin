package com.example.spendantt.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * CAMBIOS vs versión anterior:
 * + displayName: String?   → nombre visible en perfil ("John Doe")
 * + handle: String?        → @JohnDoe1
 * + avatarPath: String?    → ruta local de foto de perfil
 *
 * Fase 2: descomentar serverId y isSynced
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val username: String,
    val email: String,
    val passwordHash: String,

    // ── PERFIL (Set a Budget muestra perfil) ──────────────────
    val displayName: String? = null,    // Ej: "John Doe"
    val handle: String? = null,         // Ej: "@JohnDoe1"
    val avatarPath: String? = null,     // Ruta local de la foto

    val isFingerprintEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),

    // Fase 2: descomentar cuando haya backend
    // val serverId: String? = null,
    // val isSynced: Boolean = false
)