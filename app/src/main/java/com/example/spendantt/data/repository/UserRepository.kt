package com.example.spendantt.data.repository

import com.example.spendantt.data.local.dao.UserDao
import com.example.spendantt.data.local.entity.UserEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

class UserRepository(
    private val userDao: UserDao,
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
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

            val authResult = firebaseAuth
                .createUserWithEmailAndPassword(email, password)
                .await()

            val firebaseUid = authResult.user?.uid
                ?: return Result.failure(Exception("Error al crear usuario en Firebase"))

            val user = UserEntity(
                firebaseUid = firebaseUid,
                username = username,
                email = email,
                passwordHash = hashPassword(password),
                displayName = username,
                handle = "@$username"
            )
            val id = userDao.insertUser(user)

            // Guardar en Firestore para que otros dispositivos puedan hacer login
            saveUserToFirestore(user.copy(id = id.toInt()), firebaseUid)

            Result.success(user.copy(id = id.toInt()))

        } catch (e: Exception) {
            Result.failure(mapFirebaseError(e))
        }
    }

    // ── LOGIN ─────────────────────────────────────────────────
    suspend fun login(username: String, password: String): Result<UserEntity> {
        return try {
            // 1. Buscar en Room local primero
            var localUser = userDao.getUserByUsername(username)

            // 2. Si no está en Room → buscar email en Firestore
            if (localUser == null) {
                val email = findEmailByUsernameInFirestore(username)
                    ?: return Result.failure(Exception("Usuario o contraseña incorrectos"))

                // 3. Login en Firebase Auth con el email de Firestore
                val authResult = firebaseAuth
                    .signInWithEmailAndPassword(email, password)
                    .await()

                val firebaseUid = authResult.user?.uid
                    ?: return Result.failure(Exception("Error de autenticación"))

                // 4. Guardar usuario en Room local para próximos logins
                val newLocalUser = UserEntity(
                    firebaseUid = firebaseUid,
                    username = username,
                    email = email,
                    passwordHash = hashPassword(password),
                    displayName = username,
                    handle = "@$username"
                )
                val id = userDao.insertUser(newLocalUser)
                return Result.success(newLocalUser.copy(id = id.toInt()))
            }

            // 5. Usuario ya está en Room → login normal con Firebase
            val authResult = firebaseAuth
                .signInWithEmailAndPassword(localUser.email, password)
                .await()

            val firebaseUid = authResult.user?.uid
                ?: return Result.failure(Exception("Error de autenticación"))

            if (localUser.firebaseUid == null) {
                userDao.updateUser(localUser.copy(firebaseUid = firebaseUid))
            }

            Result.success(localUser.copy(firebaseUid = firebaseUid))

        } catch (e: Exception) {
            // Fallback offline: login local con Room
            val localUser = userDao.login(username, hashPassword(password))
            if (localUser != null) {
                Result.success(localUser)
            } else {
                Result.failure(mapFirebaseError(e))
            }
        }
    }

    // ── FIRESTORE ─────────────────────────────────────────────
    private suspend fun saveUserToFirestore(user: UserEntity, firebaseUid: String) {
        try {
            val data = mapOf(
                "uid" to firebaseUid,
                "username" to user.username,
                "email" to user.email,
                "displayName" to user.displayName,
                "handle" to user.handle,
                "createdAt" to user.createdAt
            )
            firestore.collection("users")
                .document(firebaseUid)
                .set(data)
                .await()
        } catch (e: Exception) {
            // Fallo silencioso
        }
    }

    private suspend fun findEmailByUsernameInFirestore(username: String): String? {
        return try {
            val result = firestore.collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .await()
            result.documents.firstOrNull()?.getString("email")
        } catch (e: Exception) {
            null
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