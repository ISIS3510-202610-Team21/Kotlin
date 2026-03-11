package com.example.spendantt.ui.screens.goal

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spendantt.R
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

private val GoalBackground = Color(0xFFF5C333)
private val GoalInputBackground = Color(0xFFF7E7AF)

@Composable
fun SetGoalFlowScreen(
    onExit: () -> Unit = {}
) {
    var currentStep by remember { mutableStateOf(0) }
    var amount by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var targetDateMillis by remember { mutableStateOf<Long?>(null) }
    var finished by remember { mutableStateOf(false) }

    val amountValue = amount.toLongOrNull() ?: 0L
    val formattedAmount = "\$${formatNumber(amountValue)}"
    val safeAmount = if (amountValue > 0L) formattedAmount else "$0"
    val safePurpose = purpose.ifBlank { "your goal" }
    val safeDate = formatDate(targetDateMillis).ifBlank { "your target date" }
    val daysRemaining = calculateDaysUntil(targetDateMillis)
    val dailyAmount = if (amountValue > 0L && daysRemaining > 0) {
        amountValue / daysRemaining
    } else {
        0L
    }
    val planText = if (daysRemaining > 0 && dailyAmount > 0L) {
        "To reach your goal of $safeAmount for $safePurpose by $safeDate, you need to save every day for $daysRemaining days."
    } else {
        "Pick a future date to calculate how much you need to save per day."
    }

    when {
        finished -> {
            SetGoalContainer(onBackClick = onExit) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Goal setup completed",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "You can now continue with the rest of the app.",
                    fontSize = 14.sp,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                GoalActionButton(text = "Done", onClick = onExit)
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        currentStep == 0 -> {
            SetGoalAmountScreen(
                amount = amount,
                onAmountChange = { amount = it },
                onBackClick = onExit,
                onContinueClick = { currentStep = 1 }
            )
        }

        currentStep == 1 -> {
            SetGoalPurposeScreen(
                purpose = purpose,
                onPurposeChange = { purpose = it },
                onBackClick = { currentStep = 0 },
                onContinueClick = { currentStep = 2 }
            )
        }

        currentStep == 2 -> {
            SetGoalDateScreen(
                targetDateMillis = targetDateMillis,
                onTargetDateChange = { targetDateMillis = it },
                onBackClick = { currentStep = 1 },
                onContinueClick = { currentStep = 3 }
            )
        }

        else -> {
            SetGoalPlanScreen(
                planText = planText,
                dailyAmountText = "Save $${formatNumber(dailyAmount)} per day",
                onBackClick = { currentStep = 2 },
                onAlrightClick = { finished = true }
            )
        }
    }
}

@Composable
fun SetGoalAmountScreen(
    amount: String,
    onAmountChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    SetGoalQuestionScreen(
        question = "How much money do you want to save?",
        inputValue = amount,
        inputPlaceholder = "30200000",
        antDrawableRes = R.drawable.ant_goal_happy,
        keyboardType = KeyboardType.Number,
        onInputChange = { onAmountChange(it.filter(Char::isDigit)) },
        onBackClick = onBackClick,
        onContinueClick = onContinueClick
    )
}

@Composable
fun SetGoalPurposeScreen(
    purpose: String,
    onPurposeChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    SetGoalQuestionScreen(
        question = "What are you saving for?",
        inputValue = purpose,
        inputPlaceholder = "A new car",
        antDrawableRes = R.drawable.ant_goal_happy_2,
        onInputChange = onPurposeChange,
        onBackClick = onBackClick,
        onContinueClick = onContinueClick
    )
}

@Composable
fun SetGoalDateScreen(
    targetDateMillis: Long?,
    onTargetDateChange: (Long?) -> Unit,
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    SetGoalDatePickerScreen(
        targetDateMillis = targetDateMillis,
        onTargetDateChange = onTargetDateChange,
        onBackClick = onBackClick,
        onContinueClick = onContinueClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetGoalDatePickerScreen(
    targetDateMillis: Long?,
    onTargetDateChange: (Long?) -> Unit,
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = targetDateMillis
    )

    SetGoalContainer(onBackClick = onBackClick) {
        Spacer(modifier = Modifier.height(42.dp))

        Text(
            text = "When do you want to achieve your goal?",
            fontSize = 18.sp,
            color = Color.Black,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(56.dp)
                .clickable { showDatePicker = true },
            color = GoalInputBackground,
            shape = RoundedCornerShape(2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDate(targetDateMillis).ifBlank { "Select a date" },
                    color = if (targetDateMillis == null) Color.DarkGray else Color.Black,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Image(
            painter = painterResource(id = R.drawable.ant_goal_worry),
            contentDescription = "Goal ant",
            modifier = Modifier.size(235.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(10.dp))

        GoalActionButton(text = "Continue", onClick = onContinueClick)

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTargetDateChange(datePickerState.selectedDateMillis)
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun SetGoalPlanScreen(
    planText: String,
    dailyAmountText: String,
    onBackClick: () -> Unit,
    onAlrightClick: () -> Unit
) {
    SetGoalContainer(onBackClick = onBackClick) {
        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "\"We have a plan\"",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = planText,
            fontSize = 12.sp,
            color = Color.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ant_goal_showing),
                contentDescription = "Ant showing plan",
                modifier = Modifier.size(180.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = dailyAmountText,
                fontSize = 26.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black,
                textAlign = TextAlign.Start,
                modifier = Modifier.width(150.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        GoalActionButton(text = "Alright!", onClick = onAlrightClick)

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SetGoalQuestionScreen(
    question: String,
    inputValue: String,
    inputPlaceholder: String,
    antDrawableRes: Int,
    keyboardType: KeyboardType = KeyboardType.Text,
    onInputChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    SetGoalContainer(onBackClick = onBackClick) {
        Spacer(modifier = Modifier.height(42.dp))

        Text(
            text = question,
            fontSize = 18.sp,
            color = Color.Black,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = inputValue,
            onValueChange = onInputChange,
            placeholder = { Text(text = inputPlaceholder, color = Color.DarkGray) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = GoalInputBackground,
                unfocusedContainerColor = GoalInputBackground,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            shape = RoundedCornerShape(2.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Image(
            painter = painterResource(id = antDrawableRes),
            contentDescription = "Goal ant",
            modifier = Modifier.size(235.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(10.dp))

        GoalActionButton(text = "Continue", onClick = onContinueClick)

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SetGoalContainer(
    onBackClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GoalBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "x",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                modifier = Modifier
                    .padding(4.dp)
                    .clickable(onClick = onBackClick)
                    .padding(horizontal = 4.dp)
            )
        }

        Text(
            text = "Set Goal",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(top = 6.dp)
        )

        content()
    }
}

private fun formatDate(dateMillis: Long?): String {
    if (dateMillis == null) return ""
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    return formatter.format(Date(dateMillis))
}

private fun calculateDaysUntil(targetDateMillis: Long?): Long {
    if (targetDateMillis == null) return 0L
    val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = targetDateMillis
    }
    val localTargetDate = Calendar.getInstance().apply {
        set(Calendar.YEAR, utcCalendar.get(Calendar.YEAR))
        set(Calendar.MONTH, utcCalendar.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, utcCalendar.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val todayCalendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val diffMillis = localTargetDate.timeInMillis - todayCalendar.timeInMillis
    return TimeUnit.MILLISECONDS.toDays(diffMillis).coerceAtLeast(0L)
}

private fun formatNumber(value: Long): String {
    return NumberFormat.getNumberInstance(Locale.US).format(value)
}

@Composable
private fun GoalActionButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
        modifier = Modifier
            .width(160.dp)
            .height(52.dp)
    ) {
        Text(text = text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
