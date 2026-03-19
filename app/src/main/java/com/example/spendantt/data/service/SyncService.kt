package com.example.spendantt.data.service

import android.content.Context
import com.example.spendantt.data.local.AppDatabase
import com.example.spendantt.data.local.entity.ExpenseEntity
import com.example.spendantt.data.local.entity.ExpenseLabelCrossRef
import com.example.spendantt.data.local.entity.ExpenseSource
import com.example.spendantt.data.local.entity.GoalEntity
import com.example.spendantt.data.local.entity.IncomeEntity
import com.example.spendantt.data.local.entity.IncomeType
import com.example.spendantt.data.local.entity.RecurrenceUnit
import com.example.spendantt.data.repository.LabelRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

class SyncService(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val firestore = FirebaseFirestore.getInstance()
    private val labelRepository = LabelRepository(db.labelDao())

    suspend fun syncUserData(firebaseUid: String, localUserId: Int) {
        try {
            // PRIMERO: asegurar que los labels existen antes de sincronizar expenses
            val existingLabels = db.labelDao().getLabelsByUser(localUserId).first()
            if (existingLabels.isEmpty()) {
                labelRepository.insertDefaultLabels(localUserId)
            }

            syncExpenses(firebaseUid, localUserId)
            syncIncomes(firebaseUid, localUserId)
            syncGoals(firebaseUid, localUserId)
        } catch (e: Exception) {
            // Fallo silencioso
        }
    }

    // ── EXPENSES ──────────────────────────────────────────────
    private suspend fun syncExpenses(firebaseUid: String, localUserId: Int) {
        try {
            val snapshot = firestore.collection("expenses")
                .whereEqualTo("firebaseUid", firebaseUid)
                .get()
                .await()

            // Labels ya están en Room en este punto
            val allLabels = db.labelDao().getLabelsByUser(localUserId).first()

            snapshot.documents.forEach { doc ->
                try {
                    val expense = ExpenseEntity(
                        id = (doc.getLong("id") ?: 0).toInt(),
                        userId = localUserId,
                        name = doc.getString("name") ?: "",
                        amount = doc.getDouble("amount") ?: 0.0,
                        date = doc.getLong("date") ?: 0L,
                        time = doc.getString("time") ?: "",
                        latitude = doc.getDouble("latitude"),
                        longitude = doc.getDouble("longitude"),
                        locationName = doc.getString("locationName"),
                        source = try {
                            ExpenseSource.valueOf(doc.getString("source") ?: "MANUAL")
                        } catch (e: Exception) { ExpenseSource.MANUAL },
                        receiptImagePath = doc.getString("receiptImagePath"),
                        isPendingCategory = doc.getBoolean("isPendingCategory") ?: false,
                        isRecurring = doc.getBoolean("isRecurring") ?: false,
                        recurrenceInterval = doc.getLong("recurrenceInterval")?.toInt(),
                        recurrenceUnit = doc.getString("recurrenceUnit")?.let {
                            try { RecurrenceUnit.valueOf(it) } catch (e: Exception) { null }
                        },
                        nextOccurrenceDate = doc.getLong("nextOccurrenceDate"),
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                    try { db.expenseDao().insertExpense(expense) } catch (e: Exception) { }

                    // Buscar labels por nombre
                    @Suppress("UNCHECKED_CAST")
                    val labelNames = doc.get("labelNames") as? List<String> ?: emptyList()
                    labelNames.forEach { labelName ->
                        val matchedLabel = allLabels.firstOrNull {
                            it.name.lowercase() == labelName.lowercase()
                        }
                        if (matchedLabel != null) {
                            try {
                                db.expenseDao().insertExpenseLabelCrossRef(
                                    ExpenseLabelCrossRef(expense.id, matchedLabel.id)
                                )
                            } catch (e: Exception) { }
                        }
                    }
                } catch (e: Exception) { }
            }
        } catch (e: Exception) { }
    }

    // ── INCOMES ───────────────────────────────────────────────
    private suspend fun syncIncomes(firebaseUid: String, localUserId: Int) {
        try {
            val snapshot = firestore.collection("incomes")
                .whereEqualTo("firebaseUid", firebaseUid)
                .get()
                .await()

            snapshot.documents.forEach { doc ->
                try {
                    val income = IncomeEntity(
                        id = (doc.getLong("id") ?: 0).toInt(),
                        userId = localUserId,
                        name = doc.getString("name") ?: "",
                        amount = doc.getDouble("amount") ?: 0.0,
                        type = try {
                            IncomeType.valueOf(doc.getString("type") ?: "JUST_ONCE")
                        } catch (e: Exception) { IncomeType.JUST_ONCE },
                        recurrenceInterval = doc.getLong("recurrenceInterval")?.toInt(),
                        recurrenceUnit = doc.getString("recurrenceUnit")?.let {
                            try { RecurrenceUnit.valueOf(it) } catch (e: Exception) { null }
                        },
                        nextOccurrenceDate = doc.getLong("nextOccurrenceDate"),
                        startDate = doc.getLong("startDate") ?: System.currentTimeMillis(),
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                    try { db.incomeDao().insertIncome(income) } catch (e: Exception) { }
                } catch (e: Exception) { }
            }
        } catch (e: Exception) { }
    }

    // ── GOALS ─────────────────────────────────────────────────
    private suspend fun syncGoals(firebaseUid: String, localUserId: Int) {
        try {
            val snapshot = firestore.collection("goals")
                .whereEqualTo("firebaseUid", firebaseUid)
                .get()
                .await()

            snapshot.documents.forEach { doc ->
                try {
                    val goal = GoalEntity(
                        id = (doc.getLong("id") ?: 0).toInt(),
                        userId = localUserId,
                        name = doc.getString("name") ?: "",
                        targetAmount = doc.getDouble("targetAmount") ?: 0.0,
                        currentAmount = doc.getDouble("currentAmount") ?: 0.0,
                        deadline = doc.getLong("deadline") ?: 0L,
                        isCompleted = doc.getBoolean("isCompleted") ?: false,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                    try { db.goalDao().insertGoal(goal) } catch (e: Exception) { }
                } catch (e: Exception) { }
            }
        } catch (e: Exception) { }
    }
}