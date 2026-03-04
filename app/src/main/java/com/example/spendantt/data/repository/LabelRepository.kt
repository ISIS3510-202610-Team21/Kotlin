package com.example.spendantt.data.repository

import com.example.spendantt.data.local.dao.LabelDao
import com.example.spendantt.data.local.entity.LabelEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository de etiquetas.
 *
 * Fase 1: Todo local con Room
 * Fase 2: Inyectar ApiService y sincronizar con backend
 */
class LabelRepository(
    private val labelDao: LabelDao,
    // Fase 2: agregar ApiService
    // private val apiService: ApiService
) {

    // ── INSERTAR ──────────────────────────────────────────────
    suspend fun insertLabel(label: LabelEntity): Result<Long> {
        return try {
            val id = labelDao.insertLabel(label)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Inserta las etiquetas por defecto para un usuario nuevo.
     * Basado en las etiquetas del diseño: Academic Essentials, University Fees, etc.
     */
    suspend fun insertDefaultLabels(userId: Int) {
        val defaultLabels = listOf(
            LabelEntity(name = "Academic Essentials", userId = userId, iconEmoji = "📚"),
            LabelEntity(name = "University Fees",     userId = userId, iconEmoji = "🎓"),
            LabelEntity(name = "Learning Materials",  userId = userId, iconEmoji = "📖"),
            LabelEntity(name = "Commute",             userId = userId, iconEmoji = "🚌"),
            LabelEntity(name = "Lifestyle & Social",  userId = userId, iconEmoji = "🎉"),
            LabelEntity(name = "Living Expenses",     userId = userId, iconEmoji = "🏠"),
            LabelEntity(name = "Strategic & Utility", userId = userId, iconEmoji = "⚡"),
            LabelEntity(name = "Food",                userId = userId, iconEmoji = "🍔"),
            LabelEntity(name = "Transport",           userId = userId, iconEmoji = "🚗"),
        )
        labelDao.insertLabels(defaultLabels)
    }

    // ── OBTENER ───────────────────────────────────────────────
    fun getLabelsByUser(userId: Int): Flow<List<LabelEntity>> {
        return labelDao.getLabelsByUser(userId)
    }

    fun searchLabels(userId: Int, query: String): Flow<List<LabelEntity>> {
        return labelDao.searchLabels(userId, query)
    }

    suspend fun getLabelById(labelId: Int): LabelEntity? {
        return labelDao.getLabelById(labelId)
    }

    // ── ACTUALIZAR / ELIMINAR ─────────────────────────────────
    suspend fun updateLabel(label: LabelEntity): Result<Unit> {
        return try {
            labelDao.updateLabel(label)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteLabel(label: LabelEntity): Result<Unit> {
        return try {
            labelDao.deleteLabel(label)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}