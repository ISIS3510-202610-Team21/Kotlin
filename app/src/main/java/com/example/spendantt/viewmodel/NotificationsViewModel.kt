package com.example.spendantt.viewmodel

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spendantt.data.local.AppDatabase
import com.example.spendantt.data.local.entity.ExpenseEntity
import com.example.spendantt.data.local.entity.ExpenseSource
import com.example.spendantt.data.notifications.PaymentNotificationParser
import com.example.spendantt.data.repository.AppNotification
import com.example.spendantt.data.repository.ExpenseRepository
import com.example.spendantt.data.repository.LabelRepository
import com.example.spendantt.data.repository.NotificationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationsViewModel(
    context: Context,
    private val userId: Int
) : ViewModel() {
    private val database = AppDatabase.getInstance(context)
    private val repository = NotificationRepository(context)
    private val expenseRepository = ExpenseRepository(database.expenseDao(), database.labelDao())
    private val labelRepository = LabelRepository(database.labelDao())

    private val _notifications = mutableStateOf<List<AppNotification>>(emptyList())
    val notifications: State<List<AppNotification>> = _notifications

    init {
        refresh()
    }

    fun refresh() {
        _notifications.value = repository.getNotifications(userId)
    }

    fun markAllAsRead() {
        repository.markAllAsRead(userId)
        refresh()
    }

    fun simulateGooglePayExpense() {
        viewModelScope.launch {
            val notificationId = "google_pay_sim_${System.currentTimeMillis()}"
            val title = "Compra aprobada por \$51.600"
            val body = "Tu compra en CAFE QUINDIO EXPRESS TITAN PLAZA por \$51.600,00 con tu tarjeta terminada en 3141 ha sido APROBADA."

            repository.postSimulationNotification(
                notificationId = notificationId,
                title = title,
                body = body
            )

            val parsed = PaymentNotificationParser.parse(title, body) ?: return@launch
            val labels = labelRepository.getLabelsByUser(userId).first()
            val foodLabelId = labels.firstOrNull { it.name.equals("Food", ignoreCase = true) }?.id
            val now = System.currentTimeMillis()
            val timeText = SimpleDateFormat("hh:mma", Locale.getDefault()).format(Date(now))

            expenseRepository.insertExpense(
                expense = ExpenseEntity(
                    userId = userId,
                    name = parsed.merchantName,
                    amount = parsed.amount,
                    date = now,
                    time = timeText,
                    locationName = parsed.merchantName,
                    source = ExpenseSource.GOOGLE_PAY
                ),
                labelIds = listOfNotNull(foodLabelId)
            )

            repository.upsertNotification(
                userId = userId,
                notificationId = "google_pay_imported_$notificationId",
                type = "google_pay_imported",
                title = "Payment detected",
                body = "${parsed.merchantName} for COP ${parsed.amount.toInt()} was added as an expense."
            )
            refresh()
        }
    }
}
