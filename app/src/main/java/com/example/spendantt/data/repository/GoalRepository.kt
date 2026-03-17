package com.example.spendantt.data.repository

import com.example.spendantt.data.local.dao.GoalDao
import com.example.spendantt.data.local.entity.GoalEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class GoalRepository(
    private val goalDao: GoalDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun insertGoal(goal: GoalEntity): Result<Long> {
        return try {
            // 1. Guardar en Room
            val id = goalDao.insertGoal(goal)

            // 2. Sincronizar con Firestore
            syncGoalToFirestore(goal.copy(id = id.toInt()))

            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getGoalsByUser(userId: Int): Flow<List<GoalEntity>> =
        goalDao.getGoalsByUser(userId)

    fun getActiveGoals(userId: Int): Flow<List<GoalEntity>> =
        goalDao.getActiveGoals(userId)

    suspend fun getGoalById(goalId: Int): GoalEntity? =
        goalDao.getGoalById(goalId)

    suspend fun addProgress(goalId: Int, amount: Double): Result<Unit> {
        return try {
            val goal = goalDao.getGoalById(goalId)
                ?: return Result.failure(Exception("Meta no encontrada"))

            val newAmount = (goal.currentAmount + amount).coerceAtMost(goal.targetAmount)
            goalDao.updateCurrentAmount(goalId, newAmount)

            if (newAmount >= goal.targetAmount) {
                goalDao.markAsCompleted(goalId)
            }

            syncGoalToFirestore(goal.copy(currentAmount = newAmount))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateGoal(goal: GoalEntity): Result<Unit> {
        return try {
            goalDao.updateGoal(goal)
            syncGoalToFirestore(goal)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteGoal(goal: GoalEntity): Result<Unit> {
        return try {
            goalDao.deleteGoal(goal)
            firestore.collection("goals")
                .document("${goal.userId}_${goal.id}")
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── FIRESTORE SYNC ────────────────────────────────────────
    private suspend fun syncGoalToFirestore(goal: GoalEntity) {
        try {
            val data = mapOf(
                "id" to goal.id,
                "userId" to goal.userId,
                "name" to goal.name,
                "targetAmount" to goal.targetAmount,
                "currentAmount" to goal.currentAmount,
                "deadline" to goal.deadline,
                "isCompleted" to goal.isCompleted,
                "createdAt" to goal.createdAt
            )
            firestore.collection("goals")
                .document("${goal.userId}_${goal.id}")
                .set(data)
                .await()
        } catch (e: Exception) {
            // Fallo silencioso — Room ya tiene los datos
        }
    }
}