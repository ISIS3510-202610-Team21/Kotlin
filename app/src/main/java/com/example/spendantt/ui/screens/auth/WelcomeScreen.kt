package com.example.spendantt.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spendantt.R
import com.example.spendantt.ui.theme.*

@Composable
fun WelcomeScreen(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    hasLoggedInOnce: Boolean = false,
    lastUserDisplayName: String = "",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SpendAntGreen)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(160.dp))

            Text(
                text = "SpendAnt",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SpendAntBlack
            )
            Text(
                text = "Your Finance Pal",
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic,
                color = SpendAntBlack
            )

            Spacer(modifier = Modifier.height(60.dp))

            if (hasLoggedInOnce && lastUserDisplayName.isNotEmpty()) {
                Text(
                    text = "Hi, $lastUserDisplayName",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = SpendAntBlack
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── LOGIN ─────────────────────────────────────────
            Button(
                onClick = onLoginClick,
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpendAntBlack,
                    contentColor = SpendAntWhite
                ),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Login", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── REGISTER ──────────────────────────────────────
            Button(
                onClick = onRegisterClick,
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpendAntBlack,
                    contentColor = SpendAntWhite
                ),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Register", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // ── HORMIGA ───────────────────────────────────────────
        Image(
            painter = painterResource(id = R.drawable.ant_goal_side),
            contentDescription = "SpendAnt",
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-55).dp)
        )
    }
}
