package com.example.spendantt.data.repository

import com.example.spendantt.data.local.dao.UserDao
import com.example.spendantt.data.local.entity.UserEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

/**
 * Repository de usuarios.
 *
 * Fase 1: Todo local con Room
 * Fase 2: Inyectar ApiService aquí y decidir si va local o remoto
 *
 * Patrón:
 *   ViewModel → UserRepository → UserDao (local)
 *                              → ApiService (Fase 2)
 */
class UserRepository(
    private val userDao: UserDao,
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    // ── REGISTER ──────────────────────────────────────────────
    suspend fun register(
        username: String,
        email: String,
        password: String
    ): Result<UserEntity> {
        return try {
            if (userDao.usernameExists(username) > 0) {
                return Result.failure(Exception("El nombre de usuario ya existe"))
            }

            // 1. Crear en Firebase Auth
            val authResult = firebaseAuth
                .createUserWithEmailAndPassword(email, password)
                .await()

            val firebaseUid = authResult.user?.uid
                ?: return Result.failure(Exception("Error al crear usuario en Firebase"))

            // 2. Guardar en Room con firebaseUid
            val user = UserEntity(
                firebaseUid = firebaseUid,
                username = username,
                email = email,
                passwordHash = hashPassword(password),
                displayName = username,
                handle = "@$username"
            )
            val id = userDao.insertUser(user)
            Result.success(user.copy(id = id.toInt()))

        } catch (e: Exception) {
            Result.failure(mapFirebaseError(e))
        }
    }

    // ── LOGIN ─────────────────────────────────────────────────
    suspend fun login(username: String, password: String): Result<UserEntity> {
        return try {
            // 1. Buscar usuario en Room por username para obtener email
            val localUser = userDao.getUserByUsername(username)
                ?: return Result.failure(Exception("Usuario o contraseña incorrectos"))

            // 2. Login en Firebase Auth con email
            val authResult = firebaseAuth
                .signInWithEmailAndPassword(localUser.email, password)
                .await()

            val firebaseUid = authResult.user?.uid
                ?: return Result.failure(Exception("Error de autenticación"))

            // 3. Actualizar firebaseUid si no estaba guardado
            if (localUser.firebaseUid == null) {
                userDao.updateUser(localUser.copy(firebaseUid = firebaseUid))
            }

            Result.success(localUser.copy(firebaseUid = firebaseUid))

        } catch (e: Exception) {
            // Fallback offline: si no hay internet, login local con Room
            val localUser = userDao.login(username, hashPassword(password))
            if (localUser != null) {
                Result.success(localUser)
            } else {
                Result.failure(mapFirebaseError(e))
            }
        }
    }

    // ── HELPERS ───────────────────────────────────────────────
    suspend fun getUserById(userId: Int): UserEntity? = userDao.getUserById(userId)

    suspend fun getLastLoggedUser(): UserEntity? = userDao.getLastLoggedUser()

    suspend fun enableFingerprint(userId: Int, enable: Boolean) {
        val user = userDao.getUserById(userId) ?: return
        userDao.updateUser(user.copy(isFingerprintEnabled = enable))
    }

    fun getCurrentFirebaseUser(): FirebaseUser? = firebaseAuth.currentUser

    fun signOut() = firebaseAuth.signOut()

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(password.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun mapFirebaseError(e: Exception): Exception {
        return when {
            e.message?.contains("email address is already in use") == true ->
                Exception("El email ya está registrado")
            e.message?.contains("password is invalid") == true ||
                    e.message?.contains("no user record") == true ->
                Exception("Usuario o contraseña incorrectos")
            e.message?.contains("network") == true ||
                    e.message?.contains("unable to resolve host") == true ->
                Exception("Sin conexión a internet")
            else -> Exception(e.message ?: "Error desconocido")
        }
    }
}