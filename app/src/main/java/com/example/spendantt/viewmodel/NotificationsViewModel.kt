package com.example.spendantt.viewmodel

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.spendantt.data.repository.AppNotification
import com.example.spendantt.data.repository.NotificationRepository

class NotificationsViewModel(
    context: Context,
    private val userId: Int
) : ViewModel() {
    private val repository = NotificationRepository(context)

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
}
