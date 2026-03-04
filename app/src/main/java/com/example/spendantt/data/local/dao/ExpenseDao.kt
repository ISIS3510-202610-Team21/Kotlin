package com.example.spendantt.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.spendantt.data.local.entity.ExpenseEntity
import com.example.spendantt.data.local.entity.ExpenseLabelCrossRef
import com.example.spendantt.data.local.entity.ExpenseWithLabels
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseLabelCrossRef(crossRef: ExpenseLabelCrossRef)

    @Transaction
    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY date DESC")
    fun getExpensesWithLabels(userId: Int): Flow<List<ExpenseWithLabels>>

    @Transaction
    @Query("SELECT * FROM expenses WHERE id = :expenseId LIMIT 1")
    suspend fun getExpenseWithLabels(expenseId: Int): ExpenseWithLabels?

    @Transaction
    @Query("SELECT * FROM expenses WHERE userId = :userId AND date BETWEEN :from AND :to ORDER BY date DESC")
    fun getExpensesByDateRange(userId: Int, from: Long, to: Long): Flow<List<ExpenseWithLabels>>

    @Query("SELECT * FROM expenses WHERE userId = :userId AND isPendingCategory = 1")
    fun getPendingCategoryExpenses(userId: Int): Flow<List<ExpenseEntity>>

    // ── RECURRENCIA (NUEVO) ───────────────────────────────────
    // Gastos recurrentes cuya próxima ocurrencia ya llegó (para generar automáticamente)
    @Query("SELECT * FROM expenses WHERE userId = :userId AND isRecurring = 1 AND nextOccurrenceDate <= :timestamp")
    suspend fun getDueRecurringExpenses(userId: Int, timestamp: Long): List<ExpenseEntity>

    // Todos los gastos recurrentes del usuario (para mostrar en lista)
    @Query("SELECT * FROM expenses WHERE userId = :userId AND isRecurring = 1 ORDER BY nextOccurrenceDate ASC")
    fun getRecurringExpenses(userId: Int): Flow<List<ExpenseEntity>>

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expense_label_cross_ref WHERE expenseId = :expenseId")
    suspend fun deleteExpenseLabels(expenseId: Int)

    @Query("SELECT SUM(amount) FROM expenses WHERE userId = :userId")
    fun getTotalSpent(userId: Int): Flow<Double?>

    @Query("SELECT SUM(amount) FROM expenses WHERE userId = :userId AND date BETWEEN :from AND :to")
    fun getTotalSpentInRange(userId: Int, from: Long, to: Long): Flow<Double?>

    // Fase 2: descomentar para sincronización con backend
    // @Query("SELECT * FROM expenses WHERE isSynced = 0")
    // fun getUnsyncedExpenses(): Flow<List<ExpenseEntity>>
}