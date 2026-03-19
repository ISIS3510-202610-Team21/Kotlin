package com.example.spendantt.ui.screens.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spendantt.ui.theme.*
import com.example.spendantt.viewmodel.BudgetViewModel
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.spendantt.R

@Composable
fun ProfileScreen(
    viewModel: BudgetViewModel,
    displayName: String,
    handle: String,
    onIncomeClick: () -> Unit,
    onGoalsClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpendAntBackground),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── HEADER VERDE ──────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpendAntGreen)
                .padding(16.dp)
        ) {
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
                    contentDescription = "Editar perfil",
                    tint = SpendAntBlack
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── AVATAR ROSA ───────────────────────────────────
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFCDD2)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Avatar",
                tint = Color(0xFFE57373),
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── NOMBRE Y HANDLE ───────────────────────────────
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

        Spacer(modifier = Modifier.height(24.dp))

        // ── BOTONES INCOME y GOALS (negros) ───────────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
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


        Spacer(modifier = Modifier.weight(1f))

        Image(
            painter = painterResource(id = R.drawable.ant_goal_happy),
            contentDescription = "SpendAnt",
            modifier = Modifier
                .size(180.dp)
                .padding(bottom = 16.dp)
        )
    }
}

// ── BOTÓN NEGRO ───────────────────────────────────────────────────────────────

@Composable
private fun ProfileActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SpendAntBlack,
            contentColor = SpendAntWhite
        ),
        modifier = modifier.height(44.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = SpendAntWhite
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            color = SpendAntWhite
        )
    }
}