package com.example.spendantt.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.spendantt.data.local.entity.IncomeEntity
import kotlinx.coroutines.flow.Flow

/**
 * NUEVO DAO - para IncomeEntity (funcionalidad 6: Set a Budget)
 */
@Dao
interface IncomeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: IncomeEntity): Long

    // Flow → UI se actualiza automáticamente
    @Query("SELECT * FROM incomes WHERE userId = :userId ORDER BY startDate DESC")
    fun getIncomesByUser(userId: Int): Flow<List<IncomeEntity>>

    // Total de ingresos del usuario (para calcular balance)
    @Query("SELECT SUM(amount) FROM incomes WHERE userId = :userId")
    fun getTotalIncome(userId: Int): Flow<Double?>

    // Ingresos recurrentes próximos a ocurrir (para notificaciones)
    @Query("SELECT * FROM incomes WHERE userId = :userId AND nextOccurrenceDate <= :timestamp")
    suspend fun getUpcomingRecurringIncomes(userId: Int, timestamp: Long): List<IncomeEntity>

    @Update
    suspend fun updateIncome(income: IncomeEntity)

    @Delete
    suspend fun deleteIncome(income: IncomeEntity)

    // Fase 2: descomentar para sincronización con backend
    // @Query("SELECT * FROM incomes WHERE isSynced = 0")
    // fun getUnsyncedIncomes(): Flow<List<IncomeEntity>>
}