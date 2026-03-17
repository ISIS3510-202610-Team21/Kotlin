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

    val firebaseUid: String? = null,    // ← Firebase Auth UID

    val username: String,
    val email: String,
    val passwordHash: String,

    val displayName: String? = null,
    val handle: String? = null,
    val avatarPath: String? = null,

    val isFingerprintEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()

    // Fase 2: descomentar cuando haya backend
    // val serverId: String? = null,
    // val isSynced: Boolean = false
)

