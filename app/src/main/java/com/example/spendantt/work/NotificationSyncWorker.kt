package com.example.spendantt.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.spendantt.data.local.AppDatabase
import com.example.spendantt.data.local.entity.GoalEntity
import com.example.spendantt.data.preferences.GoalPreferences
import com.example.spendantt.data.repository.ExpenseRepository
import com.example.spendantt.data.repository.GoalRepository
import com.example.spendantt.data.repository.IncomeRepository
import com.example.spendantt.data.repository.NotificationRepository
import com.example.spendantt.util.DailyFinanceCalculator
import kotlinx.coroutines.flow.first
import java.text.DecimalFormat
import java.util.Calendar

class NotificationSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = applicationContext.getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)
            val userId = prefs.getInt(KEY_LAST_USER_ID, -1)
            if (userId == -1) return Result.success()

            val database = AppDatabase.getInstance(applicationContext)
            val expenseRepository = ExpenseRepository(database.expenseDao(), database.labelDao())
            val goalRepository = GoalRepository(database.goalDao())
            val incomeRepository = IncomeRepository(database.incomeDao())
            val notificationRepository = NotificationRepository(applicationContext)
            val goalPreferences = GoalPreferences(applicationContext)

            val now = System.currentTimeMillis()
            val expenses = expenseRepository.getExpensesWithLabels(userId).first()
                .filter { it.expense.date <= now }
            val allGoals = goalRepository.getGoalsByUser(userId).first()
            val incomes = incomeRepository.getIncomesByUser(userId).first()
            val totalDailyIncome = DailyFinanceCalculator.sumDailyIncome(incomes)
            val todayExpenses = DailyFinanceCalculator.calculateTodayExpenses(expenses, now)
            val todayKey = startOfTodayMillis(now)
            notificationRepository.pruneFutureDailyNotifications(userId, todayKey)
            val selectedGoalId = goalPreferences.getSelectedGoalId(userId)
            val activeGoals = allGoals.filter { goal ->
                val currentAmount = DailyFinanceCalculator.calculateDynamicGoalAmount(
                    goal = goal,
                    allGoals = allGoals,
                    expenses = expenses,
                    totalDailyIncome = totalDailyIncome,
                    selectedGoalId = selectedGoalId,
                    now = now
                )
                currentAmount + 0.0001 < goal.targetAmount
            }

            if (DailyFinanceCalculator.isDailyBudgetExceeded(totalDailyIncome, activeGoals, todayExpenses)) {
                val currency = DecimalFormat("#,##0").format(todayExpenses)
                notificationRepository.upsertDailyNotification(
                    userId = userId,
                    type = "budget_exceeded",
                    dayStart = todayKey,
                    title = "Daily budget reached zero",
                    body = "Today's remaining budget after goals is now COP 0. Today's expenses reached COP $currency."
                )
            } else {
                notificationRepository.removeDailyNotification(userId, "budget_exceeded", todayKey)
            }

            val canCoverGoalTargets = DailyFinanceCalculator.canCoverTodayGoalTargets(
                totalDailyIncome = totalDailyIncome,
                todayExpenses = todayExpenses,
                activeGoals = activeGoals
            )
            if (!canCoverGoalTargets && activeGoals.isNotEmpty()) {
                val shortfall = DailyFinanceCalculator.calculateGoalShortfallAmount(
                    totalDailyIncome = totalDailyIncome,
                    todayExpenses = todayExpenses,
                    activeGoals = activeGoals
                )
                val shortfallText = DecimalFormat("#,##0").format(shortfall)
                notificationRepository.upsertDailyNotification(
                    userId = userId,
                    type = "goal_adjustment",
                    dayStart = todayKey,
                    title = "Today's goal target was not met",
                    body = "Today's income after expenses is short by COP $shortfallText for your goals. Consider saving more tomorrow or moving a deadline to a later date."
                )
            } else {
                notificationRepository.removeDailyNotification(userId, "goal_adjustment", todayKey)
            }

            allGoals.forEach { goal ->
                syncGoalCompletedNotification(
                    notificationRepository = notificationRepository,
                    userId = userId,
                    goal = goal,
                    allGoals = allGoals,
                    totalDailyIncome = totalDailyIncome,
                    expenses = expenses,
                    selectedGoalId = selectedGoalId
                )
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun syncGoalCompletedNotification(
        notificationRepository: NotificationRepository,
        userId: Int,
        goal: GoalEntity,
        allGoals: List<GoalEntity>,
        totalDailyIncome: Double,
        expenses: List<com.example.spendantt.data.local.entity.ExpenseWithLabels>,
        selectedGoalId: Int?
    ) {
        val currentAmount = DailyFinanceCalculator.calculateDynamicGoalAmount(
            goal = goal,
            allGoals = allGoals,
            expenses = expenses,
            totalDailyIncome = totalDailyIncome,
            selectedGoalId = selectedGoalId
        )
        val halfwayNotificationId = "goal_half_${goal.id}"
        val notificationId = "goal_completed_${goal.id}"
        if (currentAmount + 0.0001 >= goal.targetAmount / 2.0) {
            notificationRepository.upsertNotification(
                userId = userId,
                notificationId = halfwayNotificationId,
                type = "goal_half",
                title = "Halfway there",
                body = "Congrats! You're halfway to your goal."
            )
        } else {
            notificationRepository.removeNotification(userId, halfwayNotificationId)
        }
        if (currentAmount + 0.0001 >= goal.targetAmount) {
            notificationRepository.upsertNotification(
                userId = userId,
                notificationId = notificationId,
                type = "goal_completed",
                title = "Goal completed",
                body = "You completed your goal \"${goal.name}\"."
            )
        } else {
            notificationRepository.removeNotification(userId, notificationId)
        }
    }

    private fun startOfTodayMillis(now: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    companion object {
        private const val AUTH_PREFS_NAME = "auth_prefs"
        private const val KEY_LAST_USER_ID = "last_user_id"
    }
}
