package com.example.spendantt.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.spendantt.data.local.entity.UserEntity

@Dao
interface UserDao {

    // ── REGISTER ──────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    // ── LOGIN ─────────────────────────────────────────────────
    @Query("SELECT * FROM users WHERE username = :username AND passwordHash = :passwordHash LIMIT 1")
    suspend fun login(username: String, passwordHash: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: Int): UserEntity?

    // ── FINGERPRINT ───────────────────────────────────────────
    // Obtiene el último usuario logueado (para mostrar "Hi Bob, login with fingerprint")
    @Query("SELECT * FROM users ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLastLoggedUser(): UserEntity?

    @Update
    suspend fun updateUser(user: UserEntity)

    // ── VALIDACIONES ──────────────────────────────────────────
    @Query("SELECT COUNT(*) FROM users WHERE username = :username")
    suspend fun usernameExists(username: String): Int

    @Query("SELECT COUNT(*) FROM users WHERE email = :email")
    suspend fun emailExists(email: String): Int

    // Fase 2: descomentar para sincronización con backend
    // @Query("SELECT * FROM users WHERE isSynced = 0")
    // fun getUnsyncedUsers(): Flow<List<UserEntity>>
}