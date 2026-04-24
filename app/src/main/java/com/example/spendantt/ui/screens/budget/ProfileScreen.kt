package com.example.spendantt.ui.screens.budget

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.spendantt.R
import com.example.spendantt.ui.theme.*
import com.example.spendantt.viewmodel.BudgetViewModel

@Composable
fun ProfileScreen(
    viewModel: BudgetViewModel,
    displayName: String,
    handle: String,
    avatarPath: String?,
    onIncomeClick: () -> Unit,
    onGoalsClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val exportResult by viewModel.exportResult.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

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
                if (!avatarPath.isNullOrBlank()) {
                    // CACHING | Nano | 5pts | Coil AsyncImage: carga el avatar local desde almacenamiento interno y reutiliza su caché de memoria/disco para evitar decodificar la imagen en cada recomposición
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
                        tint = Color(0xFFE57373),
                        modifier = Modifier.size(48.dp)
                    )
                }
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

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.exportUserData(context) },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpendAntGreen,
                    contentColor = SpendAntBlack
                ),
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Export My Data", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
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
