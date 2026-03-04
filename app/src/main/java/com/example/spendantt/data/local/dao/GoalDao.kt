package com.example.spendantt.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.spendantt.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

/**
 * NUEVO DAO - para GoalEntity (funcionalidad 7: Set a Goal)
 */
@Dao
interface GoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Query("SELECT * FROM goals WHERE userId = :userId ORDER BY deadline ASC")
    fun getGoalsByUser(userId: Int): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :goalId LIMIT 1")
    suspend fun getGoalById(goalId: Int): GoalEntity?

    // Metas activas (no completadas)
    @Query("SELECT * FROM goals WHERE userId = :userId AND isCompleted = 0 ORDER BY deadline ASC")
    fun getActiveGoals(userId: Int): Flow<List<GoalEntity>>

    // Actualiza el monto ahorrado actual de una meta
    @Query("UPDATE goals SET currentAmount = :newAmount WHERE id = :goalId")
    suspend fun updateCurrentAmount(goalId: Int, newAmount: Double)

    // Marca la meta como completada
    @Query("UPDATE goals SET isCompleted = 1 WHERE id = :goalId")
    suspend fun markAsCompleted(goalId: Int)

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Delete
    suspend fun deleteGoal(goal: GoalEntity)

    // Fase 2: descomentar para sincronización con backend
    // @Query("SELECT * FROM goals WHERE isSynced = 0")
    // fun getUnsyncedGoals(): Flow<List<GoalEntity>>
}