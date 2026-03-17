package com.example.spendantt.data.repository

import com.example.spendantt.data.local.dao.IncomeDao
import com.example.spendantt.data.local.entity.IncomeEntity
import com.example.spendantt.data.local.entity.IncomeType
import com.example.spendantt.data.local.entity.RecurrenceUnit
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class IncomeRepository(
    private val incomeDao: IncomeDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun insertIncome(income: IncomeEntity): Result<Long> {
        return try {
            val incomeToSave = if (income.type == IncomeType.FREQUENTLY) {
                income.copy(nextOccurrenceDate = calculateNextOccurrence(
                    from = income.startDate,
                    interval = income.recurrenceInterval ?: 1,
                    unit = income.recurrenceUnit ?: RecurrenceUnit.MONTHS
                ))
            } else income

            // 1. Guardar en Room
            val id = incomeDao.insertIncome(incomeToSave)

            // 2. Sincronizar con Firestore
            syncIncomeToFirestore(incomeToSave.copy(id = id.toInt()))

            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getIncomesByUser(userId: Int): Flow<List<IncomeEntity>> =
        incomeDao.getIncomesByUser(userId)

    fun getTotalIncome(userId: Int): Flow<Double?> =
        incomeDao.getTotalIncome(userId)

    suspend fun updateIncome(income: IncomeEntity): Result<Unit> {
        return try {
            incomeDao.updateIncome(income)
            syncIncomeToFirestore(income)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteIncome(income: IncomeEntity): Result<Unit> {
        return try {
            incomeDao.deleteIncome(income)
            firestore.collection("incomes")
                .document("${income.userId}_${income.id}")
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── FIRESTORE SYNC ────────────────────────────────────────
    private suspend fun syncIncomeToFirestore(income: IncomeEntity) {
        try {
            val data = mapOf(
                "id" to income.id,
                "userId" to income.userId,
                "name" to income.name,
                "amount" to income.amount,
                "type" to income.type.name,
                "recurrenceInterval" to income.recurrenceInterval,
                "recurrenceUnit" to income.recurrenceUnit?.name,
                "nextOccurrenceDate" to income.nextOccurrenceDate,
                "startDate" to income.startDate,
                "createdAt" to income.createdAt
            )
            firestore.collection("incomes")
                .document("${income.userId}_${income.id}")
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