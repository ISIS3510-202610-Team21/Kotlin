package com.example.spendantt.data.repository

import com.example.spendantt.data.local.dao.ExpenseDao
import com.example.spendantt.data.local.dao.LabelDao
import com.example.spendantt.data.local.entity.ExpenseEntity
import com.example.spendantt.data.local.entity.ExpenseLabelCrossRef
import com.example.spendantt.data.local.entity.ExpenseWithLabels
import com.example.spendantt.data.local.entity.RecurrenceUnit
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val labelDao: LabelDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    // MULTI-THREADING | NANO | 5pts | Corrutina con dispatcher explícito: CoroutineScope(Dispatchers.IO) dedicado para sincronizar gastos con Firestore en segundo plano
    // LOCAL STORAGE | NANO | 10pts | BD Relacional Room: ExpenseEntity + LabelEntity con relación many-to-many (ExpenseLabelCrossRef) persistida en SQLite vía Room DAO
    private val syncScope = CoroutineScope(Dispatchers.IO)

    suspend fun insertExpense(
        expense: ExpenseEntity,
        labelIds: List<Int> = emptyList(),
        firebaseUid: String? = null
    ): Result<Long> {
        return try {
            val expenseToSave = if (expense.isRecurring) {
                expense.copy(
                    nextOccurrenceDate = calculateNextOccurrence(
                        from = expense.date,
                        interval = expense.recurrenceInterval ?: 1,
                        unit = expense.recurrenceUnit ?: RecurrenceUnit.MONTHS
                    )
                )
            } else {
                expense
            }

            val expenseId = expenseDao.insertExpense(expenseToSave)
            labelIds.forEach { labelId ->
                expenseDao.insertExpenseLabelCrossRef(
                    ExpenseLabelCrossRef(expenseId.toInt(), labelId)
                )
            }

            val labelNames = labelIds.mapNotNull { labelDao.getLabelById(it)?.name }
            syncExpenseToFirestoreAsync(
                expense = expenseToSave.copy(id = expenseId.toInt()),
                labelNames = labelNames,
                firebaseUid = firebaseUid
            )

            Result.success(expenseId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun processDueRecurringExpenses(userId: Int, firebaseUid: String? = null): Result<Int> {
        return try {
            val now = System.currentTimeMillis()
            val dueExpenses = expenseDao.getDueRecurringExpenses(userId, now)
            var generated = 0

            dueExpenses.forEach { template ->
                val newExpense = template.copy(id = 0, date = now, createdAt = now, nextOccurrenceDate = null)
                val newId = expenseDao.insertExpense(newExpense)
                syncExpenseToFirestoreAsync(newExpense.copy(id = newId.toInt()), emptyList(), firebaseUid)

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

    fun getExpensesWithLabels(userId: Int): Flow<List<ExpenseWithLabels>> =
        expenseDao.getExpensesWithLabels(userId)

    fun getExpensesByDateRange(userId: Int, from: Long, to: Long): Flow<List<ExpenseWithLabels>> =
        expenseDao.getExpensesByDateRange(userId, from, to)

    suspend fun getExpenseById(expenseId: Int): ExpenseWithLabels? =
        expenseDao.getExpenseWithLabels(expenseId)

    fun getPendingCategoryExpenses(userId: Int): Flow<List<ExpenseEntity>> =
        expenseDao.getPendingCategoryExpenses(userId)

    suspend fun updateExpense(
        expense: ExpenseEntity,
        newLabelIds: List<Int> = emptyList(),
        firebaseUid: String? = null
    ): Result<Unit> {
        return try {
            expenseDao.updateExpense(expense)
            expenseDao.deleteExpenseLabels(expense.id)
            newLabelIds.forEach { labelId ->
                expenseDao.insertExpenseLabelCrossRef(ExpenseLabelCrossRef(expense.id, labelId))
            }
            val labelNames = newLabelIds.mapNotNull { labelDao.getLabelById(it)?.name }
            syncExpenseToFirestoreAsync(expense, labelNames, firebaseUid)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun categorizeExpense(expenseId: Int, labelId: Int, firebaseUid: String? = null): Result<Unit> {
        return try {
            val expenseWithLabels = expenseDao.getExpenseWithLabels(expenseId)
                ?: return Result.failure(Exception("Gasto no encontrado"))
            val updated = expenseWithLabels.expense.copy(isPendingCategory = false)
            expenseDao.updateExpense(updated)
            expenseDao.insertExpenseLabelCrossRef(ExpenseLabelCrossRef(expenseId, labelId))
            val allLabels = expenseDao.getExpenseWithLabels(expenseId)?.labels ?: emptyList()
            val labelNames = allLabels.map { it.name }
            syncExpenseToFirestoreAsync(updated, labelNames, firebaseUid)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(expense: ExpenseEntity, firebaseUid: String? = null): Result<Unit> {
        return try {
            expenseDao.deleteExpenseLabels(expense.id)
            expenseDao.deleteExpense(expense)
            if (firebaseUid != null) {
                firestore.collection("expenses")
                    .document("${firebaseUid}_${expense.id}")
                    .delete()
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getTotalSpent(userId: Int): Flow<Double?> = expenseDao.getTotalSpent(userId)
    fun getTotalSpentInRange(userId: Int, from: Long, to: Long): Flow<Double?> =
        expenseDao.getTotalSpentInRange(userId, from, to)

    private fun syncExpenseToFirestoreAsync(
        expense: ExpenseEntity,
        labelNames: List<String>,
        firebaseUid: String?
    ) {
        if (firebaseUid == null) return

        syncScope.launch {
            try {
                val data = mapOf(
                    "id" to expense.id,
                    "firebaseUid" to firebaseUid,
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
                    "createdAt" to expense.createdAt,
                    "labelNames" to labelNames
                )
                firestore.collection("expenses")
                    .document("${firebaseUid}_${expense.id}")
                    .set(data)
                    .await()
            } catch (_: Exception) {
                // Room remains the source of truth if Firestore sync fails.
            }
        }
    }

    private fun calculateNextOccurrence(from: Long, interval: Int, unit: RecurrenceUnit): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = from }
        when (unit) {
            RecurrenceUnit.DAYS -> cal.add(Calendar.DAY_OF_YEAR, interval)
            RecurrenceUnit.WEEKS -> cal.add(Calendar.WEEK_OF_YEAR, interval)
            RecurrenceUnit.MONTHS -> cal.add(Calendar.MONTH, interval)
        }
        return cal.timeInMillis
    }
}
