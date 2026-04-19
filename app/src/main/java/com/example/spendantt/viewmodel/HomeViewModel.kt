package com.example.spendantt.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spendantt.data.local.AppDatabase
import com.example.spendantt.data.local.entity.ExpenseWithLabels
import com.example.spendantt.data.local.entity.GoalEntity
import com.example.spendantt.data.preferences.GoalPreferences
import com.example.spendantt.data.repository.ExpenseRepository
import com.example.spendantt.data.repository.GoalRepository
import com.example.spendantt.data.repository.IncomeRepository
import com.example.spendantt.data.repository.NotificationRepository
import com.example.spendantt.data.repository.SpendingHistoryRepository
import com.example.spendantt.data.service.SyncService
import com.example.spendantt.util.DailyFinanceCalculator
import com.example.spendantt.util.SpendingAnomalyCalculator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.Calendar

class HomeViewModel(context: Context, private val userId: Int) : ViewModel() {

    private val expenseRepository: ExpenseRepository
    private val goalRepository: GoalRepository
    private val incomeRepository: IncomeRepository
    private val notificationRepository: NotificationRepository
    private val spendingHistoryRepository: SpendingHistoryRepository
    private val goalPreferences: GoalPreferences
    private val database: AppDatabase
    private val syncService: SyncService

    private val _dailyBudget = mutableStateOf(0.0)
    val dailyBudget: State<Double> = _dailyBudget

    private val _monthlyBudget = mutableStateOf(0.0)
    val monthlyBudget: State<Double> = _monthlyBudget

    private val _monthlyExpenses = mutableStateOf(0.0)
    val monthlyExpenses: State<Double> = _monthlyExpenses

    private val _categoryExpenses = mutableStateOf<Map<String, Double>>(emptyMap())
    val categoryExpenses: State<Map<String, Double>> = _categoryExpenses

    private val _allExpenses = mutableStateOf<List<ExpenseWithLabels>>(emptyList())
    val allExpenses: State<List<ExpenseWithLabels>> = _allExpenses

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _isRefreshing = mutableStateOf(false)
    val isRefreshing: State<Boolean> = _isRefreshing

    private val _errorMessage = mutableStateOf("")
    val errorMessage: State<String> = _errorMessage

    private val _userName = mutableStateOf("")
    val userName: State<String> = _userName

    private val _unreadNotificationsCount = mutableStateOf(0)
    val unreadNotificationsCount: State<Int> = _unreadNotificationsCount

    init {
        database = AppDatabase.getInstance(context)
        expenseRepository = ExpenseRepository(database.expenseDao(), database.labelDao())
        goalRepository = GoalRepository(database.goalDao())
        incomeRepository = IncomeRepository(database.incomeDao())
        notificationRepository = NotificationRepository(context)
        spendingHistoryRepository = SpendingHistoryRepository(context)
        goalPreferences = GoalPreferences(context)
        syncService = SyncService(context)
        loadHomeData()
        loadUserName()
    }

    private fun loadUserName() {
        viewModelScope.launch {
            try {
                val user = database.userDao().getUserById(userId)
                _userName.value = user?.displayName ?: user?.username ?: "User"
            } catch (e: Exception) {
                _userName.value = "User"
            }
        }
    }

    fun loadHomeData() {
        _isLoading.value = true
        observeExpenses()
        observeGoals()
        observeIncomes()
    }

    fun refreshDailyBudget() {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val latestExpenses = expenseRepository.getExpensesWithLabels(userId).first()
                    .filter { it.expense.date <= now }
                    .sortedByDescending { it.expense.date }
                _allExpenses.value = latestExpenses
                _monthlyExpenses.value = DailyFinanceCalculator.calculateCurrentMonthExpenses(latestExpenses, now)
                _categoryExpenses.value = calculateCurrentMonthCategoryMap(latestExpenses, now)
                notificationRepository.pruneInvalidScheduledNotifications(userId, now)
                notificationRepository.pruneFutureDailyNotifications(userId, startOfTodayMillis(now))
                recalculateFinancialState(latestExpenses, now)
                _unreadNotificationsCount.value = notificationRepository.getUnreadCount(userId)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error refreshing home data"
            }
        }
    }

    fun refreshUnreadNotificationsCount() {
        _unreadNotificationsCount.value = notificationRepository.getUnreadCount(userId)
        Log.d(TAG, "refreshUnreadNotificationsCount userId=$userId unread=${_unreadNotificationsCount.value}")
    }

    fun syncFromRemote() {
        if (_isRefreshing.value) return

        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val firebaseUid = database.userDao().getUserById(userId)?.firebaseUid
                if (!firebaseUid.isNullOrBlank()) {
                    syncService.syncUserData(firebaseUid, userId)
                }
                refreshDailyBudget()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error syncing data"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun markNotificationsAsRead() {
        notificationRepository.markAllAsRead(userId)
        _unreadNotificationsCount.value = 0
        Log.d(TAG, "markNotificationsAsRead userId=$userId unread=${_unreadNotificationsCount.value}")
    }

    private fun observeExpenses() {
        viewModelScope.launch {
            try {
                expenseRepository.getExpensesWithLabels(userId).collectLatest { expenses ->
                    val now = System.currentTimeMillis()
                    val sortedExpenses = expenses
                        .filter { it.expense.date <= now }
                        .sortedByDescending { it.expense.date }
                    _allExpenses.value = sortedExpenses

                    val monthRange = getMonthDateRange(now)
                    val monthlyExpenses = sortedExpenses.filter {
                        it.expense.date in monthRange.first..monthRange.second && it.expense.date <= now
                    }
                    _monthlyExpenses.value = monthlyExpenses.sumOf { it.expense.amount }

                    val categoryMap = mutableMapOf<String, Double>()
                    monthlyExpenses.forEach { expenseWithLabels ->
                        expenseWithLabels.labels.forEach { label ->
                            categoryMap[label.name] =
                                (categoryMap[label.name] ?: 0.0) + expenseWithLabels.expense.amount
                        }
                    }

                    if (categoryMap.isEmpty()) {
                        categoryMap["Food"] = 250000.0
                        categoryMap["Transport"] = 150000.0
                        categoryMap["Services"] = 120000.0
                        categoryMap["Other"] = 80000.0
                        _monthlyExpenses.value = 600000.0
                    }
                    _categoryExpenses.value = categoryMap
                    notificationRepository.pruneInvalidScheduledNotifications(userId, now)
                    notificationRepository.pruneFutureDailyNotifications(userId, startOfTodayMillis(now))

                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error loading expenses"
                _isLoading.value = false
            }
        }
    }

    private fun observeGoals() {
        viewModelScope.launch {
            try {
                goalRepository.getGoalsByUser(userId).collectLatest {
                    refreshDailyBudget()
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error loading goals"
            }
        }
    }

    private fun observeIncomes() {
        viewModelScope.launch {
            try {
                incomeRepository.getIncomesByUser(userId).collectLatest {
                    refreshDailyBudget()
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error loading incomes"
            }
        }
    }

    private suspend fun recalculateFinancialState(
        expenses: List<ExpenseWithLabels>,
        now: Long = System.currentTimeMillis()
    ) {
        try {
            val incomes = incomeRepository.getIncomesByUser(userId).first()
            val allGoals = goalRepository.getGoalsByUser(userId).first()
            val totalDailyIncome = DailyFinanceCalculator.sumDailyIncome(incomes)
            val todayExpenses = DailyFinanceCalculator.calculateTodayExpenses(expenses, now)
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

            _dailyBudget.value = DailyFinanceCalculator.calculateRemainingDailyBudget(
                totalDailyIncome = totalDailyIncome,
                activeGoals = activeGoals,
                todayExpenses = todayExpenses
            ).coerceAtLeast(0.0)

            updateNotifications(
                allGoals = allGoals,
                activeGoals = activeGoals,
                totalDailyIncome = totalDailyIncome,
                todayExpenses = todayExpenses,
                expenses = expenses,
                now = now
            )
            _unreadNotificationsCount.value = notificationRepository.getUnreadCount(userId)
            Log.d(
                TAG,
                "recalculateFinancialState userId=$userId todayExpenses=$todayExpenses activeGoals=${activeGoals.size} unread=${_unreadNotificationsCount.value}"
            )
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Error recalculating home data"
        }
    }

    private fun calculateCurrentMonthCategoryMap(
        expenses: List<ExpenseWithLabels>,
        now: Long = System.currentTimeMillis()
    ): Map<String, Double> {
        val monthRange = getMonthDateRange(now)
        val categoryMap = mutableMapOf<String, Double>()

        expenses
            .filter { it.expense.date in monthRange.first..monthRange.second && it.expense.date <= now }
            .forEach { expenseWithLabels ->
                expenseWithLabels.labels.forEach { label ->
                    categoryMap[label.name] =
                        (categoryMap[label.name] ?: 0.0) + expenseWithLabels.expense.amount
                }
            }

        return categoryMap
    }

    private suspend fun updateNotifications(
        allGoals: List<GoalEntity>,
        activeGoals: List<GoalEntity>,
        totalDailyIncome: Double,
        todayExpenses: Double,
        expenses: List<ExpenseWithLabels>,
        now: Long
    ) {
        val todayKey = startOfTodayMillis(now)
        val currency = DecimalFormat("#,##0").format(todayExpenses)
        val user = database.userDao().getUserById(userId)
        val userCreatedAt = user?.createdAt ?: now
        Log.d(
            TAG,
            "updateNotifications userId=$userId day=$todayKey todayExpenses=$todayExpenses activeGoals=${activeGoals.size} totalDailyIncome=$totalDailyIncome"
        )

        syncWelcomeNotification(
            allGoals = allGoals,
            totalDailyIncome = totalDailyIncome,
            expenses = expenses
        )

        syncSpendingAnomalyNotification(
            expenses = expenses,
            userCreatedAt = userCreatedAt,
            now = now
        )

        if (totalDailyIncome > 0.0 &&
            DailyFinanceCalculator.isDailyBudgetExceeded(totalDailyIncome, activeGoals, todayExpenses)
        ) {
            notificationRepository.upsertDailyNotification(
                userId = userId,
                type = "budget_exceeded",
                dayStart = todayKey,
                title = "Daily budget reached zero",
                body = "Today's remaining budget after goals is now COP 0. Today's expenses reached COP $currency."
            )
        } else {
            notificationRepository.removeDailyNotification(
                userId = userId,
                type = "budget_exceeded",
                dayStart = todayKey
            )
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
            notificationRepository.removeDailyNotification(
                userId = userId,
                type = "goal_adjustment",
                dayStart = todayKey
            )
        }

        val selectedGoalId = goalPreferences.getSelectedGoalId(userId)
        allGoals.forEach { goal ->
            val currentAmount = DailyFinanceCalculator.calculateDynamicGoalAmount(
                goal = goal,
                allGoals = allGoals,
                expenses = expenses,
                totalDailyIncome = totalDailyIncome,
                selectedGoalId = selectedGoalId,
                now = now
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
                notificationRepository.removeNotification(
                    userId = userId,
                    notificationId = halfwayNotificationId
                )
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
                notificationRepository.removeNotification(
                    userId = userId,
                    notificationId = notificationId
                )
            }
        }
    }

    private fun syncWelcomeNotification(
        allGoals: List<GoalEntity>,
        totalDailyIncome: Double,
        expenses: List<ExpenseWithLabels>
    ) {
        if (totalDailyIncome <= 0.0 && expenses.isEmpty() && allGoals.isEmpty()) {
            notificationRepository.upsertNotification(
                userId = userId,
                notificationId = "welcome_$userId",
                type = "welcome",
                title = "Welcome to SpendAnt",
                body = "Thanks for using SpendAnt. Start by adding your income and goals."
            )
        }
    }

    private fun syncSpendingAnomalyNotification(
        expenses: List<ExpenseWithLabels>,
        userCreatedAt: Long,
        now: Long
    ) {
        if (!hasMinimumAccountAgeForSpendingAnomaly(userCreatedAt, now)) {
            val yesterdayStart = startOfTodayMillis(now) - ONE_DAY_MILLIS
            notificationRepository.removeDailyNotification(
                userId = userId,
                type = "spending_anomaly",
                dayStart = yesterdayStart
            )
            return
        }

        val recentClosedDays = if (spendingHistoryRepository.isCacheValid(userId)) {
            Log.d(TAG, "syncSpendingAnomalyNotification userId=$userId using cached spending history")
            spendingHistoryRepository.getRecentClosedDays(userId)
        } else {
            val todayStart = startOfTodayMillis(now)
            val calculated = (1..6).map { dayOffset ->
                val dayStart = todayStart - (dayOffset * ONE_DAY_MILLIS)
                val dayEndExclusive = dayStart + ONE_DAY_MILLIS
                com.example.spendantt.data.repository.DailyExpenseTotal(
                    dayStart = dayStart,
                    totalExpense = SpendingAnomalyCalculator.sumExpensesForDay(
                        expenses = expenses,
                        dayStart = dayStart,
                        dayEndExclusive = dayEndExclusive
                    )
                )
            }
            spendingHistoryRepository.saveRecentClosedDays(userId, calculated)
            Log.d(TAG, "syncSpendingAnomalyNotification userId=$userId cache expired, recalculated and saved")
            calculated
        }

        val analyzedDay = recentClosedDays.firstOrNull() ?: return
        val baseline = recentClosedDays.drop(1).take(5)
        val stats = SpendingAnomalyCalculator.calculateStats(baseline)

        if (stats != null && SpendingAnomalyCalculator.isAnomalous(analyzedDay.totalExpense, stats)) {
            notificationRepository.upsertDailyNotification(
                userId = userId,
                type = "spending_anomaly",
                dayStart = analyzedDay.dayStart,
                title = "Spending anomaly detected",
                body = "Yesterday you spent more than expected. Be careful with your spending."
            )
        } else {
            notificationRepository.removeDailyNotification(
                userId = userId,
                type = "spending_anomaly",
                dayStart = analyzedDay.dayStart
            )
        }
    }

    private fun hasMinimumAccountAgeForSpendingAnomaly(userCreatedAt: Long, now: Long): Boolean {
        val accountStart = startOfTodayMillis(userCreatedAt)
        val todayStart = startOfTodayMillis(now)
        val ageInDays = ((todayStart - accountStart) / ONE_DAY_MILLIS).coerceAtLeast(0L)
        return ageInDays >= 5L
    }

    private fun getMonthDateRange(now: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val firstDay = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val lastDay = calendar.timeInMillis

        return Pair(firstDay, lastDay)
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
        private const val TAG = "SpendAntHomeNotif"
        private const val ONE_DAY_MILLIS = 24L * 60L * 60L * 1000L
    }
}
