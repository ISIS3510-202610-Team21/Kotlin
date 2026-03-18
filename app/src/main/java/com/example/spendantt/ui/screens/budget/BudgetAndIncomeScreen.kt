package com.example.spendantt.ui.screens.budget

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spendantt.R
import com.example.spendantt.ui.components.IncomeCard
import com.example.spendantt.ui.components.SpendAntButton
import com.example.spendantt.ui.components.SpendAntHeader
import com.example.spendantt.ui.theme.*
import com.example.spendantt.viewmodel.BudgetViewModel

@Composable
fun BudgetAndIncomeScreen(
    viewModel: BudgetViewModel,
    onClose: () -> Unit,
    onNewIncome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpendAntWhite)
    ) {

        // ── HEADER ────────────────────────────────────────────
        SpendAntHeader(
            title = "Budget and Income",
            onClose = onClose,
            onConfirm = onClose
        )

        // ── LISTA DE INGRESOS ─────────────────────────────────
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(SpendAntWhite),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SpendAntGreen)
            }
        } else if (uiState.incomes.isEmpty()) {
            // ── ESTADO VACÍO CON HORMIGA ──────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(SpendAntWhite),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ant_goal_worry),
                        contentDescription = "No incomes",
                        modifier = Modifier.size(140.dp)
                    )
                    Text(
                        text = "No incomes yet",
                        color = SpendAntTextSecondary,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Add your first income source",
                        color = SpendAntTextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(uiState.incomes) { index, income ->
                    IncomeCard(income = income, index = index)
                }
            }
        }

        // ── BOTÓN NEW INCOME ──────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp)
        ) {
            SpendAntButton(
                text = "New Income",
                onClick = onNewIncome
            )
        }
    }

    // ── MENSAJES DE ERROR ─────────────────────────────────────
    uiState.errorMessage?.let { msg ->
        LaunchedEffect(msg) { viewModel.clearMessages() }
        Snackbar(
            modifier = Modifier.padding(16.dp),
            containerColor = SpendAntError
        ) {
            Text(text = msg, color = SpendAntWhite)
        }
    }
}
