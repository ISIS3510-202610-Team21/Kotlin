package com.example.spendantt.data.repository

import com.example.spendantt.data.local.dao.ExpenseDao
import com.example.spendantt.data.local.dao.LabelDao
import com.example.spendantt.data.local.entity.ExpenseEntity
import com.example.spendantt.data.local.entity.ExpenseLabelCrossRef
import com.example.spendantt.data.local.entity.ExpenseWithLabels
import com.example.spendantt.data.local.entity.RecurrenceUnit
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val labelDao: LabelDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // ── INSERTAR ──────────────────────────────────────────────
    suspend fun insertExpense(
        expense: ExpenseEntity,
        labelIds: List<Int> = emptyList()
    ): Result<Long> {
        return try {
            val expenseToSave = if (expense.isRecurring) {
                expense.copy(nextOccurrenceDate = calculateNextOccurrence(
                    from = expense.date,
                    interval = expense.recurrenceInterval ?: 1,
                    unit = expense.recurrenceUnit ?: RecurrenceUnit.MONTHS
                ))
            } else expense

            // 1. Guardar en Room
            val expenseId = expenseDao.insertExpense(expenseToSave)
            labelIds.forEach { labelId ->
                expenseDao.insertExpenseLabelCrossRef(
                    ExpenseLabelCrossRef(expenseId.toInt(), labelId)
                )
            }

            // 2. Sincronizar con Firestore
            syncExpenseToFirestore(expenseToSave.copy(id = expenseId.toInt()))

            Result.success(expenseId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── RECURRENCIA ───────────────────────────────────────────
    suspend fun processDueRecurringExpenses(userId: Int): Result<Int> {
        return try {
            val now = System.currentTimeMillis()
            val dueExpenses = expenseDao.getDueRecurringExpenses(userId, now)
            var generated = 0

            dueExpenses.forEach { template ->
                val newExpense = template.copy(
                    id = 0,
                    date = now,
                    createdAt = now,
                    nextOccurrenceDate = null
                )
                val newId = expenseDao.insertExpense(newExpense)
                syncExpenseToFirestore(newExpense.copy(id = newId.toInt()))

                val nextDate = calculateNextOccurrence(
                    from = template.nextOccurrenceDate ?: now,
                    interval = template.recurrenceInterval ?: 1,
                    unit = template.recurrenceUnit ?: RecurrenceUnit.MONTHS
                )
                expenseDao.updateExpense(template.copy(nextOccurrenceDate = nextDate))
                generated++
            }

            Result.success(generated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getRecurringExpenses(userId: Int): Flow<List<ExpenseEntity>> =
        expenseDao.getRecurringExpenses(userId)

    // ── OBTENER ───────────────────────────────────────────────
    fun getExpensesWithLabels(userId: Int): Flow<List<ExpenseWithLabels>> =
        expenseDao.getExpensesWithLabels(userId)

    fun getExpensesByDateRange(userId: Int, from: Long, to: Long): Flow<List<ExpenseWithLabels>> =
        expenseDao.getExpensesByDateRange(userId, from, to)

    suspend fun getExpenseById(expenseId: Int): ExpenseWithLabels? =
        expenseDao.getExpenseWithLabels(expenseId)

    fun getPendingCategoryExpenses(userId: Int): Flow<List<ExpenseEntity>> =
        expenseDao.getPendingCategoryExpenses(userId)

    // ── ACTUALIZAR ────────────────────────────────────────────
    suspend fun updateExpense(
        expense: ExpenseEntity,
        newLabelIds: List<Int> = emptyList()
    ): Result<Unit> {
        return try {
            expenseDao.updateExpense(expense)
            expenseDao.deleteExpenseLabels(expense.id)
            newLabelIds.forEach { labelId ->
                expenseDao.insertExpenseLabelCrossRef(
                    ExpenseLabelCrossRef(expense.id, labelId)
                )
            }
            syncExpenseToFirestore(expense)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun categorizeExpense(expenseId: Int, labelId: Int): Result<Unit> {
        return try {
            val expenseWithLabels = expenseDao.getExpenseWithLabels(expenseId)
                ?: return Result.failure(Exception("Gasto no encontrado"))
            val updated = expenseWithLabels.expense.copy(isPendingCategory = false)
            expenseDao.updateExpense(updated)
            expenseDao.insertExpenseLabelCrossRef(ExpenseLabelCrossRef(expenseId, labelId))
            syncExpenseToFirestore(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── ELIMINAR ──────────────────────────────────────────────
    suspend fun deleteExpense(expense: ExpenseEntity): Result<Unit> {
        return try {
            expenseDao.deleteExpenseLabels(expense.id)
            expenseDao.deleteExpense(expense)
            // Eliminar de Firestore
            firestore.collection("expenses")
                .document("${expense.userId}_${expense.id}")
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── TOTALES ───────────────────────────────────────────────
    fun getTotalSpent(userId: Int): Flow<Double?> = expenseDao.getTotalSpent(userId)
    fun getTotalSpentInRange(userId: Int, from: Long, to: Long): Flow<Double?> =
        expenseDao.getTotalSpentInRange(userId, from, to)

    // ── FIRESTORE SYNC ────────────────────────────────────────
    private suspend fun syncExpenseToFirestore(expense: ExpenseEntity) {
        try {
            val data = mapOf(
                "id" to expense.id,
                "userId" to expense.userId,
                "name" to expense.name,
                "amount" to expense.amount,
                "date" to expense.date,
                "time" to expense.time,
                "latitude" to expense.latitude,
                "longitude" to expense.longitude,
                "locationName" to expense.locationName,
                "source" to expense.source.name,
                "receiptImagePath" to expense.receiptImagePath,
                "isPendingCategory" to expense.isPendingCategory,
                "isRecurring" to expense.isRecurring,
                "recurrenceInterval" to expense.recurrenceInterval,
                "recurrenceUnit" to expense.recurrenceUnit?.name,
                "nextOccurrenceDate" to expense.nextOccurrenceDate,
                "createdAt" to expense.createdAt
            )
            firestore.collection("expenses")
                .document("${expense.userId}_${expense.id}")
                .set(data)
                .await()
        } catch (e: Exception) {
            // Fallo silencioso — Room ya tiene los datos
        }
    }

    // ── HELPER ────────────────────────────────────────────────
    private fun calculateNextOccurrence(from: Long, interval: Int, unit: RecurrenceUnit): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = from }
        when (unit) {
            RecurrenceUnit.DAYS   -> cal.add(Calendar.DAY_OF_YEAR, interval)
            RecurrenceUnit.WEEKS  -> cal.add(Calendar.WEEK_OF_YEAR, interval)
            RecurrenceUnit.MONTHS -> cal.add(Calendar.MONTH, interval)
        }
        return cal.timeInMillis
    }
}