package com.example.spendantt.data.repository

import com.example.spendantt.data.local.dao.ExpenseDao
import com.example.spendantt.data.local.dao.LabelDao
import com.example.spendantt.data.local.entity.ExpenseEntity
import com.example.spendantt.data.local.entity.ExpenseLabelCrossRef
import com.example.spendantt.data.local.entity.ExpenseWithLabels
import com.example.spendantt.data.local.entity.RecurrenceUnit
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

/**
 * CAMBIOS vs versión anterior:
 * + insertRecurringExpense()     → guarda gasto recurrente con nextOccurrenceDate
 * + processDueRecurringExpenses()→ genera automáticamente los gastos recurrentes pendientes
 * + getRecurringExpenses()       → lista de gastos recurrentes activos
 */
class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val labelDao: LabelDao,
    // Fase 2: agregar ApiService
    // private val apiService: ApiService
) {

    // ── INSERTAR ──────────────────────────────────────────────
    suspend fun insertExpense(
        expense: ExpenseEntity,
        labelIds: List<Int> = emptyList()
    ): Result<Long> {
        return try {
            // Si es recurrente, calcular nextOccurrenceDate
            val expenseToSave = if (expense.isRecurring) {
                expense.copy(nextOccurrenceDate = calculateNextOccurrence(
                    from = expense.date,
                    interval = expense.recurrenceInterval ?: 1,
                    unit = expense.recurrenceUnit ?: RecurrenceUnit.MONTHS
                ))
            } else expense

            val expenseId = expenseDao.insertExpense(expenseToSave)
            labelIds.forEach { labelId ->
                expenseDao.insertExpenseLabelCrossRef(
                    ExpenseLabelCrossRef(expenseId.toInt(), labelId)
                )
            }
            Result.success(expenseId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── RECURRENCIA (NUEVO) ───────────────────────────────────
    /**
     * Revisa y genera automáticamente los gastos recurrentes que ya vencieron.
     * Llamar al abrir la app o con un WorkManager en background.
     */
    suspend fun processDueRecurringExpenses(userId: Int): Result<Int> {
        return try {
            val now = System.currentTimeMillis()
            val dueExpenses = expenseDao.getDueRecurringExpenses(userId, now)
            var generated = 0

            dueExpenses.forEach { template ->
                // Crear el nuevo gasto con la fecha de hoy
                val newExpense = template.copy(
                    id = 0,
                    date = now,
                    createdAt = now,
                    nextOccurrenceDate = null   // El nuevo no es el template
                )
                expenseDao.insertExpense(newExpense)

                // Actualizar el template con la próxima fecha
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

    fun getRecurringExpenses(userId: Int): Flow<List<ExpenseEntity>> {
        return expenseDao.getRecurringExpenses(userId)
    }

    // ── OBTENER ───────────────────────────────────────────────
    fun getExpensesWithLabels(userId: Int): Flow<List<ExpenseWithLabels>> {
        return expenseDao.getExpensesWithLabels(userId)
    }

    fun getExpensesByDateRange(userId: Int, from: Long, to: Long): Flow<List<ExpenseWithLabels>> {
        return expenseDao.getExpensesByDateRange(userId, from, to)
    }

    suspend fun getExpenseById(expenseId: Int): ExpenseWithLabels? {
        return expenseDao.getExpenseWithLabels(expenseId)
    }

    fun getPendingCategoryExpenses(userId: Int): Flow<List<ExpenseEntity>> {
        return expenseDao.getPendingCategoryExpenses(userId)
    }

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
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun categorizeExpense(expenseId: Int, labelId: Int): Result<Unit> {
        return try {
            val expenseWithLabels = expenseDao.getExpenseWithLabels(expenseId)
                ?: return Result.failure(Exception("Gasto no encontrado"))
            expenseDao.updateExpense(expenseWithLabels.expense.copy(isPendingCategory = false))
            expenseDao.insertExpenseLabelCrossRef(ExpenseLabelCrossRef(expenseId, labelId))
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
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── TOTALES ───────────────────────────────────────────────
    fun getTotalSpent(userId: Int): Flow<Double?> = expenseDao.getTotalSpent(userId)
    fun getTotalSpentInRange(userId: Int, from: Long, to: Long): Flow<Double?> =
        expenseDao.getTotalSpentInRange(userId, from, to)

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