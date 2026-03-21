package com.example.spendantt.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spendantt.data.repository.AppNotification
import com.example.spendantt.ui.components.SpendAntHeader
import com.example.spendantt.ui.theme.SpendAntBlack
import com.example.spendantt.ui.theme.SpendAntFontFamily
import com.example.spendantt.ui.theme.SpendAntTextSecondary
import com.example.spendantt.ui.theme.SpendAntWhite
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun NotificationsScreen(
    notifications: List<AppNotification>,
    onBackClick: () -> Unit,
    onSimulateGooglePayClick: () -> Unit
) {
    var selectedNotification by remember { mutableStateOf<AppNotification?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpendAntWhite)
    ) {
        SpendAntHeader(
            title = "Notifications",
            onClose = onBackClick
        )

        Button(
            onClick = onSimulateGooglePayClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = SpendAntBlack,
                contentColor = SpendAntWhite
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Simulate Google Pay expense",
                fontFamily = SpendAntFontFamily
            )
        }

        if (notifications.isEmpty()) {
            EmptyNotificationsState()
        } else {
            val groupedNotifications = notifications.groupBy { notificationSectionLabel(it.createdAt) }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                groupedNotifications.forEach { (section, sectionNotifications) ->
                    item(key = section) {
                        Text(
                            text = section,
                            color = SpendAntBlack,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = SpendAntFontFamily,
                            modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                        )
                    }

                    items(sectionNotifications, key = { it.id }) { notification ->
                        NotificationCard(
                            notification = notification,
                            onClick = { selectedNotification = notification }
                        )
                    }
                }
            }
        }
    }

    selectedNotification?.let { notification ->
        NotificationDetailDialog(
            notification = notification,
            onDismiss = { selectedNotification = null }
        )
    }
}

@Composable
private fun EmptyNotificationsState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsNone,
                contentDescription = "No notifications",
                tint = SpendAntBlack,
                modifier = Modifier.size(84.dp)
            )
            Text(
                text = "No notifications yet",
                color = SpendAntBlack,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = SpendAntFontFamily
            )
            Text(
                text = "Your updates will appear here.",
                color = SpendAntTextSecondary,
                fontSize = 15.sp,
                fontFamily = SpendAntFontFamily
            )
        }
    }
}

@Composable
private fun NotificationCard(
    notification: AppNotification,
    onClick: () -> Unit
) {
    val style = notificationStyle(notification.type)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(0.dp),
                clip = false
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(style.cardColor)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(style.iconContainerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = style.icon,
                    contentDescription = notification.title,
                    tint = SpendAntBlack,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = relativeTimestamp(notification.createdAt),
                    color = SpendAntTextSecondary,
                    fontSize = 12.sp,
                    fontFamily = SpendAntFontFamily
                )
                Text(
                    text = notification.title,
                    color = SpendAntBlack,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SpendAntFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = notification.body,
                    color = SpendAntBlack.copy(alpha = 0.78f),
                    fontSize = 14.sp,
                    fontFamily = SpendAntFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open notification",
                tint = SpendAntBlack,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun NotificationDetailDialog(
    notification: AppNotification,
    onDismiss: () -> Unit
) {
    val style = notificationStyle(notification.type)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpendAntWhite,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(style.iconContainerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = style.icon,
                    contentDescription = notification.title,
                    tint = SpendAntBlack
                )
            }
        },
        title = {
            Text(
                text = notification.title,
                color = SpendAntBlack,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = SpendAntFontFamily
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = formatNotificationTimestamp(notification.createdAt),
                    color = SpendAntTextSecondary,
                    fontSize = 13.sp,
                    fontFamily = SpendAntFontFamily
                )
                Text(
                    text = notification.body,
                    color = SpendAntBlack,
                    fontSize = 15.sp,
                    fontFamily = SpendAntFontFamily
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Close",
                    color = SpendAntBlack,
                    fontFamily = SpendAntFontFamily,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

private data class NotificationVisualStyle(
    val cardColor: Color,
    val iconContainerColor: Color,
    val icon: ImageVector
)

private fun notificationStyle(type: String): NotificationVisualStyle {
    val normalizedType = type.lowercase(Locale.getDefault())
    val isNegative = normalizedType in setOf(
        "budget_exceeded",
        "goal_adjustment",
        "spending_anomaly",
        "habit_fixer",
        "warning",
        "daily_budget"
    )

    return if (isNegative) {
        NotificationVisualStyle(
            cardColor = Color(0xFFD9E8FF),
            iconContainerColor = Color(0xFF4D8EF7),
            icon = when (normalizedType) {
                "budget_exceeded", "daily_budget" -> Icons.Default.AccountBalanceWallet
                "spending_anomaly" -> Icons.Default.WarningAmber
                "habit_fixer" -> Icons.Default.LocationOn
                else -> Icons.Default.Flag
            }
        )
    } else {
        NotificationVisualStyle(
            cardColor = Color(0xFFD7F8DF),
            iconContainerColor = Color(0xFF4DCD73),
            icon = when {
                normalizedType.contains("goal_completed") -> Icons.Default.EmojiEvents
                normalizedType.contains("goal_half") -> Icons.Default.Celebration
                normalizedType.contains("weekly_insight") -> Icons.Default.NotificationsNone
                normalizedType.contains("goal") -> Icons.Default.Savings
                else -> Icons.Default.NotificationsNone
            }
        )
    }
}

private fun notificationSectionLabel(timestamp: Long): String {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = timestamp }

    return when {
        isSameDay(now, target) -> "Today"
        isSameDay(
            Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) },
            target
        ) -> "Yesterday"
        else -> SimpleDateFormat("MMMM d", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun relativeTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diffMinutes = ((now - timestamp) / 60000L).coerceAtLeast(0L)

    return when {
        diffMinutes < 1L -> "Just now"
        diffMinutes < 60L -> "$diffMinutes minute${if (diffMinutes == 1L) "" else "s"} ago"
        diffMinutes < 24L * 60L -> {
            val diffHours = diffMinutes / 60L
            "$diffHours hour${if (diffHours == 1L) "" else "s"} ago"
        }
        else -> formatNotificationTimestamp(timestamp)
    }
}

private fun formatNotificationTimestamp(timestamp: Long): String {
    return SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
        .format(Date(timestamp))
}

private fun isSameDay(first: Calendar, second: Calendar): Boolean {
    return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
        first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
}
