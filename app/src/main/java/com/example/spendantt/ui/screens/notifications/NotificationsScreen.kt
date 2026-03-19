package com.example.spendantt.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spendantt.ui.components.SpendAntHeader
import com.example.spendantt.ui.theme.SpendAntBlack
import com.example.spendantt.ui.theme.SpendAntFontFamily
import com.example.spendantt.ui.theme.SpendAntGreenLight
import com.example.spendantt.ui.theme.SpendAntTextSecondary
import com.example.spendantt.ui.theme.SpendAntWhite

data class NotificationUiState(
    val id: String,
    val title: String,
    val body: String,
    val timestampLabel: String
)

@Composable
fun NotificationsScreen(
    notifications: List<NotificationUiState>,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpendAntWhite)
    ) {
        SpendAntHeader(
            title = "Notifications",
            onClose = onBackClick
        )

        if (notifications.isEmpty()) {
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
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(notifications, key = { it.id }) { notification ->
                NotificationCard(notification = notification)
            }
        }
    }
}

@Composable
private fun NotificationCard(notification: NotificationUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpendAntGreenLight)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = notification.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = SpendAntBlack,
                fontFamily = SpendAntFontFamily
            )
            Text(
                text = notification.body,
                fontSize = 14.sp,
                color = SpendAntBlack,
                fontFamily = SpendAntFontFamily
            )
            Text(
                text = notification.timestampLabel,
                fontSize = 12.sp,
                color = SpendAntTextSecondary,
                fontFamily = SpendAntFontFamily
            )
        }
    }
}
