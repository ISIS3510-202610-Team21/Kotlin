package com.example.spendantt.data.repository

import com.example.spendantt.data.local.dao.IncomeDao
import com.example.spendantt.data.local.entity.IncomeEntity
import com.example.spendantt.data.local.entity.IncomeType
import com.example.spendantt.data.local.entity.RecurrenceUnit
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

/**
 * NUEVO REPOSITORY - Set a Budget (funcionalidad 6)
 *
 * Fase 1: Todo local con Room
 * Fase 2: Inyectar ApiService y sincronizar con backend
 */
class IncomeRepository(
    private val incomeDao: IncomeDao,
    // Fase 2: agregar ApiService
    // private val apiService: ApiService
) {

    suspend fun insertIncome(income: IncomeEntity): Result<Long> {
        return try {
            // Si es recurrente, calcular la primera nextOccurrenceDate
            val incomeToSave = if (income.type == IncomeType.FREQUENTLY) {
                income.copy(nextOccurrenceDate = calculateNextOccurrence(
                    from = income.startDate,
                    interval = income.recurrenceInterval ?: 1,
                    unit = income.recurrenceUnit ?: RecurrenceUnit.MONTHS
                ))
            } else income

            val id = incomeDao.insertIncome(incomeToSave)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getIncomesByUser(userId: Int): Flow<List<IncomeEntity>> {
        return incomeDao.getIncomesByUser(userId)
    }

    fun getTotalIncome(userId: Int): Flow<Double?> {
        return incomeDao.getTotalIncome(userId)
    }

    suspend fun updateIncome(income: IncomeEntity): Result<Unit> {
        return try {
            incomeDao.updateIncome(income)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteIncome(income: IncomeEntity): Result<Unit> {
        return try {
            incomeDao.deleteIncome(income)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calcula el próximo timestamp de ocurrencia dado un intervalo.
     * Ej: from=hoy, interval=2, unit=WEEKS → hoy + 14 días
     */
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