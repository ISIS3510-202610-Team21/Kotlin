package com.example.spendantt.data.repository

import com.example.spendantt.data.local.dao.GoalDao
import com.example.spendantt.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

/**
 * NUEVO REPOSITORY - Set a Goal (funcionalidad 7)
 *
 * Fase 1: Todo local con Room
 * Fase 2: Inyectar ApiService y sincronizar con backend
 */
class GoalRepository(
    private val goalDao: GoalDao,
    // Fase 2: agregar ApiService
    // private val apiService: ApiService
) {

    suspend fun insertGoal(goal: GoalEntity): Result<Long> {
        return try {
            val id = goalDao.insertGoal(goal)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getGoalsByUser(userId: Int): Flow<List<GoalEntity>> {
        return goalDao.getGoalsByUser(userId)
    }

    fun getActiveGoals(userId: Int): Flow<List<GoalEntity>> {
        return goalDao.getActiveGoals(userId)
    }

    suspend fun getGoalById(goalId: Int): GoalEntity? {
        return goalDao.getGoalById(goalId)
    }

    /**
     * Agrega dinero al progreso de una meta.
     * Automáticamente la marca como completada si llega al target.
     */
    suspend fun addProgress(goalId: Int, amount: Double): Result<Unit> {
        return try {
            val goal = goalDao.getGoalById(goalId)
                ?: return Result.failure(Exception("Meta no encontrada"))

            val newAmount = (goal.currentAmount + amount).coerceAtMost(goal.targetAmount)
            goalDao.updateCurrentAmount(goalId, newAmount)

            // Si llegó al 100%, marcar como completada
            if (newAmount >= goal.targetAmount) {
                goalDao.markAsCompleted(goalId)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateGoal(goal: GoalEntity): Result<Unit> {
        return try {
            goalDao.updateGoal(goal)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteGoal(goal: GoalEntity): Result<Unit> {
        return try {
            goalDao.deleteGoal(goal)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}