package com.example.spendantt.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.example.spendantt.data.local.entity.ExpenseEntity
import com.example.spendantt.data.local.entity.ExpenseWithLabels
import com.example.spendantt.data.local.entity.LabelEntity
import com.example.spendantt.ui.theme.CategoryBlue
import com.example.spendantt.ui.theme.CategoryCoralPink
import com.example.spendantt.ui.theme.CategoryOrange
import com.example.spendantt.ui.theme.CategoryYellow
import com.example.spendantt.ui.theme.LabelIconMapper
import com.example.spendantt.ui.theme.SpendAntFontFamily
import com.example.spendantt.ui.theme.SpendAntGreenv2
import com.example.spendantt.viewmodel.HomeViewModel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.LinkedHashMap
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNotificationsClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onExpenseClick: (Int) -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        viewModel.refreshDailyBudget()
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshDailyBudget()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    HomeScreenContent(
        isLoading = viewModel.isLoading.value,
        dailyBudget = viewModel.dailyBudget.value,
        monthlyExpenses = viewModel.monthlyExpenses.value,
        categoryExpenses = viewModel.categoryExpenses.value,
        allExpenses = viewModel.allExpenses.value,
        unreadNotificationsCount = viewModel.unreadNotificationsCount.value,
        onNotificationsClick = onNotificationsClick,
        onLogoutClick = onLogoutClick,
        onExpenseClick = onExpenseClick
    )
}

@Composable
private fun HomeScreenContent(
    isLoading: Boolean,
    dailyBudget: Double,
    monthlyExpenses: Double,
    categoryExpenses: Map<String, Double>,
    allExpenses: List<ExpenseWithLabels>,
    unreadNotificationsCount: Int,
    onNotificationsClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onExpenseClick: (Int) -> Unit = {}
) {
    val expenseSections = rememberExpenseSections(allExpenses)

    // Colores priorizados para las categorías
    val priorityColors = listOf(
        CategoryBlue,
        CategoryOrange,
        CategoryYellow,
        CategoryCoralPink
    )

    // Crear mapa de colores por categoría basado en el ranking de gastos
    val categoryColorMap = remember(categoryExpenses) {
        val sortedCategories = categoryExpenses.entries.sortedByDescending { it.value }
        sortedCategories.mapIndexed { index, (label, _) ->
            label to if (index < priorityColors.size) {
                priorityColors[index]
            } else {
                LabelIconMapper.getConsistentColorForLabel(label)
            }
        }.toMap()
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = SpendAntGreenv2)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentPadding = PaddingValues(bottom = 110.dp)
    ) {
        // Header verde con "Home"
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SpendAntGreenv2)
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .clickable(onClick = onNotificationsClick)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                    if (unreadNotificationsCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 8.dp, y = (-6).dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color.Red),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = unreadNotificationsCount.coerceAtMost(99).toString(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.wrapContentSize(Alignment.Center)
                            )
                        }
                    }
                }
                Text(
                    text = "Home",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpendAntFontFamily,
                    color = Color.Black,
                    modifier = Modifier.align(Alignment.Center)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
                    contentDescription = "Logout",
                    tint = Color.Black,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(24.dp)
                        .clickable(onClick = onLogoutClick)
                )



            }
        }

        // Your Budget for today
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Your Budget for today",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SpendAntFontFamily,
                    color = Color.Gray
                )
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "$${DecimalFormat("#,##0").format(dailyBudget)}",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SpendAntFontFamily,
                        color = SpendAntGreenv2
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "COP",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = SpendAntFontFamily,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }

        // This month you have expended
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp)
            ) {
                Text(
                    text = "This month you have expended",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SpendAntFontFamily,
                    color = Color.Gray
                )
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "$${DecimalFormat("#,##0").format(monthlyExpenses)}",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SpendAntFontFamily,
                        color = Color.Red
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "COP",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = SpendAntFontFamily,
                        color = Color.Red,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }

        // Category Bar Chart
        item {
            CategoryBarChart(
                categoryExpenses = categoryExpenses,
                categoryColorMap = categoryColorMap,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        // Expense Sections
        expenseSections.forEach { section ->
            item(key = "header_${section.title}") {
                Text(
                    text = "${section.title} Expenses",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SpendAntFontFamily,
                    color = Color.Black,
                    modifier = Modifier.padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 12.dp)
                )
            }

            items(
                count = section.expenses.size,
                key = { index -> section.expenses[index].expense.id }
            ) { index ->
                ExpenseCard(
                    expense = section.expenses[index],
                    categoryColorMap = categoryColorMap,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    onClick = { onExpenseClick(section.expenses[index].expense.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun CategoryBarChart(
    categoryExpenses: Map<String, Double>,
    categoryColorMap: Map<String, Color>,
    modifier: Modifier = Modifier
) {
    if (categoryExpenses.isEmpty()) return

    val maxAmount = categoryExpenses.values.maxOrNull() ?: 1.0
    val categories = categoryExpenses.entries.sortedByDescending { it.value }.take(4)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        categories.forEach { (label, amount) ->
            val barColor = categoryColorMap[label] ?: LabelIconMapper.getConsistentColorForLabel(label)
            CategoryBar(
                label = label,
                amount = amount,
                maxAmount = maxAmount,
                barColor = barColor,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            )
        }
    }
}

@Composable
private fun CategoryBar(
    label: String,
    amount: Double,
    maxAmount: Double,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    val heightPercentage = (amount / maxAmount).coerceIn(0.35, 1.0).toFloat()
    val iconRes = LabelIconMapper.getIconForLabel(label)
    val maxBarHeight = 180.dp

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(75.dp)
                .height(maxBarHeight * heightPercentage)
                .clip(RoundedCornerShape(16.dp))
                .background(barColor)
                .padding(vertical = 12.dp, horizontal = 6.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(30.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpendAntFontFamily,
                    color = Color.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    lineHeight = 13.sp
                )
            }
        }
    }
}

@Composable
private fun ExpenseCard(
    expense: ExpenseWithLabels,
    categoryColorMap: Map<String, Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val firstLabel = expense.labels.firstOrNull()
    val labelName = firstLabel?.name ?: "Other"
    val iconRes = LabelIconMapper.getIconForLabel(labelName)
    val cardColor = categoryColorMap[labelName] ?: LabelIconMapper.getConsistentColorForLabel(labelName)
    val pastelCardColor = lerp(cardColor, Color.White, 0.72f)

    Card(
        modifier = modifier
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
                .background(pastelCardColor)
                .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(cardColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = labelName,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(26.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, end = 8.dp)
            ) {
                Text(
                    text = expense.expense.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpendAntFontFamily,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = labelName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SpendAntFontFamily,
                    color = Color.Gray
                )
            }

            Text(
                text = "COP ${DecimalFormat("#,##0").format(expense.expense.amount)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = SpendAntFontFamily,
                color = Color.Black,
                modifier = Modifier.padding(end = 16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    val now = 1_710_720_000_000L
    val allExpenses = listOf(
        ExpenseWithLabels(
            expense = ExpenseEntity(
                id = 1,
                userId = 1,
                name = "Chick & Chips Lunch",
                amount = 23000.0,
                date = now,
                time = "13:10"
            ),
            labels = listOf(
                LabelEntity(
                    id = 1,
                    name = "Food",
                    iconEmoji = "",
                    userId = 1
                )
            )
        ),
        ExpenseWithLabels(
            expense = ExpenseEntity(
                id = 2,
                userId = 1,
                name = "TM To the University",
                amount = 3500.0,
                date = now,
                time = "08:30"
            ),
            labels = listOf(
                LabelEntity(
                    id = 2,
                    name = "Transport",
                    iconEmoji = "",
                    userId = 1
                )
            )
        ),
        ExpenseWithLabels(
            expense = ExpenseEntity(
                id = 3,
                userId = 1,
                name = "Google Drive Month",
                amount = 3500.0,
                date = now - 86_400_000L,
                time = "19:00"
            ),
            labels = listOf(
                LabelEntity(
                    id = 3,
                    name = "Services",
                    iconEmoji = "",
                    userId = 1
                )
            )
        )
    )

    HomeScreenContent(
        isLoading = false,
        dailyBudget = 25500.0,
        monthlyExpenses = 750000.0,
        categoryExpenses = mapOf(
            "Food" to 150000.0,
            "Transport" to 120000.0,
            "Services" to 100000.0,
            "Gifts" to 80000.0
        ),
        allExpenses = allExpenses,
        unreadNotificationsCount = 2
    )
}

private data class ExpenseSection(
    val title: String,
    val expenses: List<ExpenseWithLabels>
)

@Composable
private fun rememberExpenseSections(allExpenses: List<ExpenseWithLabels>): List<ExpenseSection> {
    val todayStart = rememberDayStart(System.currentTimeMillis())
    val yesterdayStart = todayStart - ONE_DAY_MILLIS
    val oneWeekAgoStart = todayStart - (ONE_DAY_MILLIS * 7)
    val groupedByDay = LinkedHashMap<Long, MutableList<ExpenseWithLabels>>()

    allExpenses.forEach { expense ->
        val dayStart = startOfDay(expense.expense.date)
        groupedByDay.getOrPut(dayStart) { mutableListOf() }.add(expense)
    }

    return groupedByDay.entries.map { (dayStart, expenses) ->
        val title = when {
            dayStart == todayStart -> "Today"
            dayStart == yesterdayStart -> "Yesterday"
            dayStart >= oneWeekAgoStart -> formatWeekday(dayStart)
            else -> formatFullDate(dayStart)
        }

        ExpenseSection(
            title = title,
            expenses = expenses.sortedByDescending { it.expense.date }
        )
    }
}

@Composable
private fun rememberDayStart(timeMillis: Long): Long = startOfDay(timeMillis)

private fun startOfDay(timeMillis: Long): Long {
    return Calendar.getInstance().apply {
        this.timeInMillis = timeMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun formatWeekday(timeMillis: Long): String {
    return SimpleDateFormat("EEEE", Locale.ENGLISH).format(Date(timeMillis))
}

private fun formatFullDate(timeMillis: Long): String {
    return SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH).format(Date(timeMillis))
}

private const val ONE_DAY_MILLIS = 86_400_000L
