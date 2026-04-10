package com.example.spendantt.ui.screens.goal

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spendantt.R
import com.example.spendantt.ui.components.SpendAntHeader
import com.example.spendantt.ui.theme.SpendAntGreen
import com.example.spendantt.ui.theme.SpendAntTextSecondary
import com.example.spendantt.ui.theme.SpendAntWhite
import com.example.spendantt.viewmodel.GoalListItemUiState
import com.example.spendantt.viewmodel.GoalsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GoalsRoute(
    viewModel: GoalsViewModel,
    onBottomBarVisibilityChange: (Boolean) -> Unit,
    onExit: () -> Unit
) {
    LaunchedEffect(viewModel.isCreatingGoal.value) {
        onBottomBarVisibilityChange(viewModel.isCreatingGoal.value)
    }

    if (viewModel.isCreatingGoal.value) {
        SetGoalFlowScreen(
            onSaveGoal = { name, targetAmount, deadline, dailyAmount ->
                viewModel.saveGoal(name, targetAmount, deadline, dailyAmount)
            },
            onExit = { viewModel.showGoalList() }
        )
        return
    }

    GoalsScreen(
        goals = viewModel.goals.value,
        isLoading = viewModel.isLoading.value,
        onExit = onExit,
        onGoalClick = { viewModel.selectGoal(it) },
        onDeleteGoalClick = { viewModel.deleteGoal(it) },
        onNewGoalClick = { viewModel.showCreateGoal() }
    )
}

@Composable
fun GoalsScreen(
    goals: List<GoalListItemUiState>,
    isLoading: Boolean,
    onExit: () -> Unit,
    onGoalClick: (Int) -> Unit,
    onDeleteGoalClick: (Int) -> Unit,
    onNewGoalClick: () -> Unit
) {
    var goalPendingDeletion by remember { mutableStateOf<GoalListItemUiState?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpendAntWhite)
    ) {
        SpendAntHeader(
            title = "Goals",
            onClose = onExit,
            onConfirm = onExit
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SpendAntGreen)
            }
            return
        }

        if (goals.isEmpty()) {
            EmptyGoalsState(onNewGoalClick = onNewGoalClick)
            return
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(goals, key = { it.id }) { goal ->
                GoalListCard(
                    goal = goal,
                    onClick = { onGoalClick(goal.id) },
                    onDeleteClick = { goalPendingDeletion = goal }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GoalActionButton(
                text = "New Goal",
                onClick = onNewGoalClick
            )
        }
    }

    if (goalPendingDeletion != null) {
        AlertDialog(
            onDismissRequest = { goalPendingDeletion = null },
            title = {
                Text(
                    text = "Delete goal?",
                    color = Color.Black
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${goalPendingDeletion?.name}\"?",
                    color = Color.Black
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        goalPendingDeletion?.let { onDeleteGoalClick(it.id) }
                        goalPendingDeletion = null
                    }
                ) {
                    Text(text = "Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { goalPendingDeletion = null }) {
                    Text(text = "Cancel", color = Color.Black)
                }
            },
            containerColor = Color.White
        )
    }
}

@Composable
private fun EmptyGoalsState(
    onNewGoalClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpendAntWhite)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Image(
            painter = painterResource(id = R.drawable.ant_goal_worry),
            contentDescription = "No goals yet",
            modifier = Modifier.size(180.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Create your first goal",
            color = SpendAntTextSecondary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Add your first goal target",
            color = SpendAntTextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        GoalActionButton(text = "New Goal", onClick = onNewGoalClick)

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun GoalListCard(
    goal: GoalListItemUiState,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val progress = goal.progressPercent / 100f
    val isCompleted = goal.isCompleted
    val baseColor = when {
        isCompleted -> Color(0xFFB8F1C5)
        goal.isSelected -> Color(0xFFB8F1C5)
        else -> Color(0xFFF6D4CA)
    }
    val accentColor = when {
        isCompleted -> Color(0xFF41C463)
        goal.isSelected -> Color(0xFF41C463)
        else -> Color(0xFFFF6A2A)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(86.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0xFFD4D4D4))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(86.dp)
                .background(baseColor)
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )

            Spacer(modifier = Modifier.size(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goal.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                Text(
                    text = "Deadline: ${formatGoalDate(goal.deadline)}",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = "Daily save: $${goal.dailyAmount.toInt()}",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (isCompleted) "Done" else "${goal.progressPercent}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete goal",
                    tint = Color.Red,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(onClick = onDeleteClick)
                )
            }
        }
    }
}

private fun formatGoalDate(deadline: Long): String {
    return SimpleDateFormat("d/MM/yyyy", Locale.getDefault()).format(Date(deadline))
}
