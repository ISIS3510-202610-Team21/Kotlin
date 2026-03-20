package com.example.spendantt.data.repository

import com.example.spendantt.data.local.dao.IncomeDao
import com.example.spendantt.data.local.entity.IncomeEntity
import com.example.spendantt.data.local.entity.IncomeType
import com.example.spendantt.data.local.entity.RecurrenceUnit
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class IncomeRepository(
    private val incomeDao: IncomeDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val syncScope = CoroutineScope(Dispatchers.IO)

    suspend fun insertIncome(income: IncomeEntity, firebaseUid: String? = null): Result<Long> {
        return try {
            val incomeToSave = if (income.type == IncomeType.FREQUENTLY) {
                income.copy(
                    nextOccurrenceDate = calculateNextOccurrence(
                        from = income.startDate,
                        interval = income.recurrenceInterval ?: 1,
                        unit = income.recurrenceUnit ?: RecurrenceUnit.MONTHS
                    )
                )
            } else {
                income
            }

            val id = incomeDao.insertIncome(incomeToSave)
            syncIncomeToFirestoreAsync(incomeToSave.copy(id = id.toInt()), firebaseUid)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getIncomesByUser(userId: Int): Flow<List<IncomeEntity>> =
        incomeDao.getIncomesByUser(userId)

    fun getTotalIncome(userId: Int): Flow<Double?> =
        incomeDao.getTotalIncome(userId)

    suspend fun updateIncome(income: IncomeEntity, firebaseUid: String? = null): Result<Unit> {
        return try {
            incomeDao.updateIncome(income)
            syncIncomeToFirestoreAsync(income, firebaseUid)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteIncome(income: IncomeEntity, firebaseUid: String? = null): Result<Unit> {
        return try {
            incomeDao.deleteIncome(income)
            if (firebaseUid != null) {
                firestore.collection("incomes")
                    .document("${firebaseUid}_${income.id}")
                    .delete()
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun syncIncomeToFirestoreAsync(income: IncomeEntity, firebaseUid: String?) {
        if (firebaseUid == null) return

        syncScope.launch {
            try {
                val data = mapOf(
                    "id" to income.id,
                    "firebaseUid" to firebaseUid,
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
                    .document("${firebaseUid}_${income.id}")
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
