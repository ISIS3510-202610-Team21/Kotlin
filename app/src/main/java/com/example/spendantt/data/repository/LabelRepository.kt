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
     * Basado en las categorías del diseño con sus respectivos labels.
     */
    suspend fun insertDefaultLabels(userId: Int) {
        val defaultLabels = listOf(
            // Academic Essentials
            LabelEntity(name = "University Fees", category = "Academic Essentials", userId = userId, iconEmoji = "🎓"),
            LabelEntity(name = "Learning Materials", category = "Academic Essentials", userId = userId, iconEmoji = "📖"),
            LabelEntity(name = "Commute", category = "Academic Essentials", userId = userId, iconEmoji = "🚌"),

            // Lifestyle & Social
            LabelEntity(name = "Food", category = "Lifestyle & Social", userId = userId, iconEmoji = "🍔"),
            LabelEntity(name = "Group Hangouts", category = "Lifestyle & Social", userId = userId, iconEmoji = "👥"),
            LabelEntity(name = "Food Delivery", category = "Lifestyle & Social", userId = userId, iconEmoji = "🛵"),
            LabelEntity(name = "Entertainment", category = "Lifestyle & Social", userId = userId, iconEmoji = "🎬"),
            LabelEntity(name = "Subscriptions", category = "Lifestyle & Social", userId = userId, iconEmoji = "📺"),
            LabelEntity(name = "Gifts", category = "Lifestyle & Social", userId = userId, iconEmoji = "🎁"),

            // Living Expenses
            LabelEntity(name = "Rent", category = "Living Expenses", userId = userId, iconEmoji = "🏠"),
            LabelEntity(name = "Utilities", category = "Living Expenses", userId = userId, iconEmoji = "💡"),
            LabelEntity(name = "Services", category = "Living Expenses", userId = userId, iconEmoji = "🔧"),
            LabelEntity(name = "Groceries", category = "Living Expenses", userId = userId, iconEmoji = "🛒"),
            LabelEntity(name = "Personal Care", category = "Living Expenses", userId = userId, iconEmoji = "💆"),
            LabelEntity(name = "Transport", category = "Living Expenses", userId = userId, iconEmoji = "🚗"),

            // Strategic & Utility Tags
            LabelEntity(name = "Owed", category = "Strategic & Utility Tags", userId = userId, iconEmoji = "💰"),
            LabelEntity(name = "Impulse", category = "Strategic & Utility Tags", userId = userId, iconEmoji = "💫"),
            LabelEntity(name = "Emergency", category = "Strategic & Utility Tags", userId = userId, iconEmoji = "🚨"),
        )
        labelDao.insertLabels(defaultLabels)
    }

    // ── OBTENER ───────────────────────────────────────────────
    fun getLabelsByUser(userId: Int): Flow<List<LabelEntity>> {
        return labelDao.getLabelsByUser(userId)
    }

    fun getLabelsByCategory(userId: Int, category: String): Flow<List<LabelEntity>> {
        return labelDao.getLabelsByCategory(userId, category)
    }

    fun getCategories(userId: Int): Flow<List<String>> {
        return labelDao.getCategories(userId)
    }

    fun searchLabels(userId: Int, query: String): Flow<List<LabelEntity>> {
        return labelDao.searchLabels(userId, query)
    }

    suspend fun getLabelById(labelId: Int): LabelEntity? {
        return labelDao.getLabelById(labelId)
    }

    suspend fun getLabelsByIds(labelIds: List<Int>): List<LabelEntity> {
        return labelDao.getLabelsByIds(labelIds)
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

    suspend fun deleteAllLabelsForUser(userId: Int): Result<Unit> {
        return try {
            labelDao.deleteAllLabelsForUser(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}