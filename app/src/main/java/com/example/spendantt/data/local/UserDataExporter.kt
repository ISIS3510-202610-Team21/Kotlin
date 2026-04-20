package com.example.spendantt.data.local

import android.content.Context
import com.example.spendantt.data.local.entity.ExpenseWithLabels
import com.example.spendantt.data.local.entity.GoalEntity
import com.example.spendantt.data.local.entity.IncomeEntity
import com.example.spendantt.data.local.entity.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

// LOCAL STORAGE | Dilan | 5pts | Archivos Locales: escribe el perfil completo del usuario (datos, gastos, metas, ingresos) como JSON en context.filesDir — almacenamiento interno privado de la app, no requiere permisos de escritura externos
class UserDataExporter(private val context: Context, private val database: AppDatabase) {

    suspend fun exportToFile(userId: Int): Result<String> = withContext(Dispatchers.IO) {
        try {
            val user = database.userDao().getUserById(userId)
            val expenses = database.expenseDao().getExpensesWithLabels(userId).first()
            val goals = database.goalDao().getGoalsByUser(userId).first()
            val incomes = database.incomeDao().getIncomesByUser(userId).first()

            val json = JSONObject().apply {
                put("exportedAt", System.currentTimeMillis())
                put("user", userToJson(user))
                put("expenses", expensesToJson(expenses))
                put("goals", goalsToJson(goals))
                put("incomes", incomesToJson(incomes))
            }

            val file = File(context.filesDir, "user_${userId}_export.json")
            file.writeText(json.toString(2))
            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun userToJson(user: UserEntity?): JSONObject = JSONObject().apply {
        user ?: return@apply
        put("id", user.id)
        put("username", user.username)
        put("email", user.email)
        put("displayName", user.displayName ?: JSONObject.NULL)
        put("handle", user.handle ?: JSONObject.NULL)
        put("createdAt", user.createdAt)
    }

    private fun expensesToJson(expenses: List<ExpenseWithLabels>): JSONArray = JSONArray().apply {
        expenses.forEach { ewl ->
            put(JSONObject().apply {
                put("id", ewl.expense.id)
                put("name", ewl.expense.name)
                put("amount", ewl.expense.amount)
                put("date", ewl.expense.date)
                put("time", ewl.expense.time)
                put("locationName", ewl.expense.locationName ?: JSONObject.NULL)
                put("source", ewl.expense.source.name)
                put("labels", JSONArray(ewl.labels.map { it.name }))
            })
        }
    }

    private fun goalsToJson(goals: List<GoalEntity>): JSONArray = JSONArray().apply {
        goals.forEach { goal ->
            put(JSONObject().apply {
                put("id", goal.id)
                put("name", goal.name)
                put("targetAmount", goal.targetAmount)
                put("currentAmount", goal.currentAmount)
                put("deadline", goal.deadline)
                put("isCompleted", goal.isCompleted)
            })
        }
    }

    private fun incomesToJson(incomes: List<IncomeEntity>): JSONArray = JSONArray().apply {
        incomes.forEach { income ->
            put(JSONObject().apply {
                put("id", income.id)
                put("name", income.name)
                put("amount", income.amount)
                put("type", income.type.name)
                put("startDate", income.startDate)
            })
        }
    }
}
