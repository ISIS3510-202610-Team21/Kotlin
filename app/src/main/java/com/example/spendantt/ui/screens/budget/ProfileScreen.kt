package com.example.spendantt.ui.screens.budget

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.spendantt.R
import com.example.spendantt.data.currency.CurrencyProvider
import com.example.spendantt.ui.theme.SpendAntBlack
import com.example.spendantt.ui.theme.SpendAntGreen
import com.example.spendantt.ui.theme.SpendAntTextPrimary
import com.example.spendantt.ui.theme.SpendAntTextSecondary
import com.example.spendantt.ui.theme.SpendAntWhite
import com.example.spendantt.viewmodel.BudgetViewModel

@Composable
fun ProfileScreen(
    viewModel: BudgetViewModel,
    displayName: String,
    handle: String,
    avatarPath: String?,
    onIncomeClick: () -> Unit,
    onGoalsClick: () -> Unit,
    onCurrencyClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val exportResult by viewModel.exportResult.collectAsState()
    val currencyState by CurrencyProvider.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showRatesUnavailableDialog by remember { mutableStateOf(false) }

    LaunchedEffect(exportResult) {
        exportResult?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearExportResult()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(SpendAntGreen)
                    .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 26.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Profile",
                            color = SpendAntBlack,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.align(Alignment.Center)
                        )
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit profile",
                                tint = SpendAntBlack
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFDDD5)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!avatarPath.isNullOrBlank()) {
                            AsyncImage(
                                model = avatarPath,
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Avatar",
                                tint = Color(0xFFFF764C),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = displayName,
                        color = SpendAntTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = handle,
                        color = SpendAntTextSecondary,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 40.dp)
            ) {
                ProfileActionButton(
                    icon = Icons.Default.AccountBalance,
                    label = "Income",
                    onClick = onIncomeClick,
                    modifier = Modifier.weight(1f)
                )
                ProfileActionButton(
                    icon = Icons.Default.Flag,
                    label = "Goals",
                    onClick = onGoalsClick,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 40.dp)
            ) {
                ProfileActionButton(
                    icon = Icons.Default.CurrencyExchange,
                    label = "Currency",
                    onClick = {
                        if (currencyState.ratesCache.size <= 1) {
                            showRatesUnavailableDialog = true
                        } else {
                            onCurrencyClick()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                ProfileActionButton(
                    icon = Icons.AutoMirrored.Filled.Article,
                    label = "Summary",
                    onClick = { viewModel.exportUserData(context) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Image(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.ant_goal_happy),
                contentDescription = "SpendAnt",
                modifier = Modifier
                    .size(180.dp)
                    .padding(bottom = 16.dp)
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showRatesUnavailableDialog) {
        AlertDialog(
            onDismissRequest = { showRatesUnavailableDialog = false },
            containerColor = Color(0xFFE0FFE9),
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Rates not available",
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Exchange rates could not be downloaded. Connect to the internet and reopen the app to enable currency conversion.",
                    color = Color(0xFF5E5E5E),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showRatesUnavailableDialog = false }) {
                    Text(
                        text = "Got it",
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}

@Composable
private fun ProfileActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SpendAntBlack,
            contentColor = SpendAntWhite
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
        modifier = modifier
            .height(34.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = SpendAntWhite
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = SpendAntWhite,
            fontWeight = FontWeight.SemiBold
        )
    }
}
