package com.example.spendantt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.spendantt.R
import com.example.spendantt.ui.theme.*
import androidx.compose.ui.res.painterResource

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onProfileClick: () -> Unit,
    onHomeClick: () -> Unit,
    onAddClick: () -> Unit,
    onGoalsClick: () -> Unit,
    onBudgetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.TopCenter
    ) {
        // ── BARRA VERDE ───────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(56.dp)
                .align(Alignment.BottomCenter)
                .background(SpendAntGreen)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavIcon(
                iconRes = R.drawable.ic_profile,
                contentDescription = "Profile",
                onClick = onProfileClick
            )
            NavIcon(
                iconRes = R.drawable.ic_home,
                contentDescription = "Home",
                onClick = onHomeClick
            )
            // Espacio para el botón central
            Spacer(modifier = Modifier.size(56.dp))
            NavIcon(
                iconRes = R.drawable.ic_goals,
                contentDescription = "Goals",
                onClick = onGoalsClick
            )
            NavIcon(
                iconRes = R.drawable.ic_budget,
                contentDescription = "Budget",
                onClick = onBudgetClick
            )
        }

        // ── BOTÓN + CENTRAL ───────────────────────────────────
        Box(
            modifier = Modifier
                .size(56.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-12).dp)
                .shadow(elevation = 8.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(SpendAntBlack)
                .clickable { onAddClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Agregar",
                tint = SpendAntWhite,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun NavIcon(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            tint = SpendAntBlack,
            modifier = Modifier.size(24.dp)
        )
    }
}