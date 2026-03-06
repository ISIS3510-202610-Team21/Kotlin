package com.example.spendantt.data.repository

import com.example.spendantt.data.local.dao.UserDao
import com.example.spendantt.data.local.entity.UserEntity
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
    // Fase 2: agregar ApiService
    // private val apiService: ApiService
) {
    companion object {
        const val TEST_USERNAME = "testuser"
        const val TEST_EMAIL = "testuser@spendantt.local"
        const val TEST_PASSWORD = "123456"
    }

    // ── REGISTER ──────────────────────────────────────────────
    /**
     * Registra un nuevo usuario.
     * Retorna Result.success con el usuario creado o Result.failure con el error.
     */
    suspend fun register(
        username: String,
        email: String,
        password: String
    ): Result<UserEntity> {
        return try {
            // Validar que no exista el username
            if (userDao.usernameExists(username) > 0) {
                return Result.failure(Exception("El nombre de usuario ya existe"))
            }
            // Validar que no exista el email
            if (userDao.emailExists(email) > 0) {
                return Result.failure(Exception("El email ya está registrado"))
            }

            val user = UserEntity(
                username = username,
                email = email,
                passwordHash = hashPassword(password)
            )

            val id = userDao.insertUser(user)
            Result.success(user.copy(id = id.toInt()))

            // Fase 2: también registrar en backend
            // val response = apiService.register(RegisterRequest(username, email, password))
            // userDao.insertUser(user.copy(serverId = response.id, isSynced = true))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── LOGIN ─────────────────────────────────────────────────
    /**
     * Login por usuario y contraseña.
     * Retorna el usuario si las credenciales son correctas, null si no.
     */
    suspend fun login(username: String, password: String): Result<UserEntity> {
        return try {
            val user = userDao.login(username, hashPassword(password))
                ?: return Result.failure(Exception("Usuario o contraseña incorrectos"))
            Result.success(user)

            // Fase 2: verificar también con backend
            // val response = apiService.login(LoginRequest(username, password))
            // Guardar token en DataStore/SharedPreferences

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── FINGERPRINT ───────────────────────────────────────────
    /**
     * Obtiene el último usuario logueado para mostrar en la pantalla de huella.
     * La verificación de huella real se hace con BiometricPrompt en el ViewModel.
     */
    suspend fun getLastLoggedUser(): UserEntity? {
        return userDao.getLastLoggedUser()
    }

    suspend fun enableFingerprint(userId: Int, enable: Boolean) {
        val user = userDao.getUserById(userId) ?: return
        userDao.updateUser(user.copy(isFingerprintEnabled = enable))
    }

    /**
     * Crea un usuario de prueba si no existe.
     * Util para entorno de desarrollo y pruebas manuales.
     */
    suspend fun ensureTestUser() {
        if (userDao.usernameExists(TEST_USERNAME) > 0) return

        val user = UserEntity(
            username = TEST_USERNAME,
            email = TEST_EMAIL,
            passwordHash = hashPassword(TEST_PASSWORD),
            displayName = "Usuario Prueba",
            handle = "@testuser"
        )
        userDao.insertUser(user)
    }

    // ── HELPERS ───────────────────────────────────────────────
    suspend fun getUserById(userId: Int): UserEntity? {
        return userDao.getUserById(userId)
    }

    /**
     * Hash SHA-256 de la contraseña.
     * Fase 2: El backend hará su propio hashing (bcrypt), esto es solo para local.
     */
    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
