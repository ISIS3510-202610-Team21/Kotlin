package com.example.spendantt.data.repository

import com.example.spendantt.data.local.dao.GoalDao
import com.example.spendantt.data.local.entity.GoalEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GoalRepository(
    private val goalDao: GoalDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val syncScope = CoroutineScope(Dispatchers.IO)

    suspend fun insertGoal(goal: GoalEntity, firebaseUid: String? = null): Result<Long> {
        return try {
            val id = goalDao.insertGoal(goal)
            syncGoalToFirestoreAsync(goal.copy(id = id.toInt()), firebaseUid)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getGoalsByUser(userId: Int): Flow<List<GoalEntity>> = goalDao.getGoalsByUser(userId)
    fun getActiveGoals(userId: Int): Flow<List<GoalEntity>> = goalDao.getActiveGoals(userId)
    suspend fun getGoalById(goalId: Int): GoalEntity? = goalDao.getGoalById(goalId)

    suspend fun addProgress(goalId: Int, amount: Double, firebaseUid: String? = null): Result<Unit> {
        return try {
            val goal = goalDao.getGoalById(goalId)
                ?: return Result.failure(Exception("Meta no encontrada"))

            val newAmount = (goal.currentAmount + amount).coerceAtMost(goal.targetAmount)
            goalDao.updateCurrentAmount(goalId, newAmount)

            if (newAmount >= goal.targetAmount) {
                goalDao.markAsCompleted(goalId)
            }

            syncGoalToFirestoreAsync(
                goal.copy(
                    currentAmount = newAmount,
                    isCompleted = newAmount >= goal.targetAmount
                ),
                firebaseUid
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateGoal(goal: GoalEntity, firebaseUid: String? = null): Result<Unit> {
        return try {
            goalDao.updateGoal(goal)
            syncGoalToFirestoreAsync(goal, firebaseUid)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteGoal(goal: GoalEntity, firebaseUid: String? = null): Result<Unit> {
        return try {
            goalDao.deleteGoal(goal)
            if (firebaseUid != null) {
                firestore.collection("goals")
                    .document("${firebaseUid}_${goal.id}")
                    .delete()
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun syncGoalToFirestoreAsync(goal: GoalEntity, firebaseUid: String?) {
        if (firebaseUid == null) return

        syncScope.launch {
            try {
                val data = mapOf(
                    "id" to goal.id,
                    "firebaseUid" to firebaseUid,
                    "userId" to goal.userId,
                    "name" to goal.name,
                    "targetAmount" to goal.targetAmount,
                    "currentAmount" to goal.currentAmount,
                    "deadline" to goal.deadline,
                    "isCompleted" to goal.isCompleted,
                    "createdAt" to goal.createdAt
                )

                firestore.collection("goals")
                    .document("${firebaseUid}_${goal.id}")
                    .set(data)
                    .await()
            } catch (_: Exception) {
                // Room remains the source of truth if Firestore sync fails.
            }
        }
    }
}
