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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.spendantt.ui.theme.SpendAntGreen
import com.example.spendantt.ui.theme.SpendAntGreenLight
import com.example.spendantt.viewmodel.GoalListItemUiState
import com.example.spendantt.viewmodel.GoalsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val GoalsEmptyBackground = Color(0xFFF5C333)

@Composable
fun GoalsRoute(
    viewModel: GoalsViewModel,
    onExit: () -> Unit
) {
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
        onNewGoalClick = { viewModel.showCreateGoal() }
    )
}

@Composable
fun GoalsScreen(
    goals: List<GoalListItemUiState>,
    isLoading: Boolean,
    onExit: () -> Unit,
    onGoalClick: (Int) -> Unit,
    onNewGoalClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEAEAEA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpendAntGreen)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Text(
                text = "x",
                fontSize = 24.sp,
                color = Color.Black,
                modifier = Modifier.clickable(onClick = onExit)
            )
            Text(
                text = "Goals",
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
            )
        }

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
                GoalListCard(goal = goal, onClick = { onGoalClick(goal.id) })
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
}

@Composable
private fun EmptyGoalsState(
    onNewGoalClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GoalsEmptyBackground)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Image(
            painter = painterResource(id = R.drawable.ant_goal_worry),
            contentDescription = "No goals yet",
            modifier = Modifier.size(240.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Create your first goal",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black,
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
    onClick: () -> Unit
) {
    val progress = goal.progressPercent / 100f
    val isCompleted = goal.progressPercent >= 100
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

            Text(
                text = if (isCompleted) "Done" else "${goal.progressPercent}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
        }
    }
}

private fun formatGoalDate(deadline: Long): String {
    return SimpleDateFormat("d/MM/yyyy", Locale.getDefault()).format(Date(deadline))
}
