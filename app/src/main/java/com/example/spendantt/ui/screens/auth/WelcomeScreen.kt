package com.example.spendantt.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.spendantt.R
import com.example.spendantt.ui.components.BlackButton
import com.example.spendantt.ui.theme.SpendAntFontFamily
import com.example.spendantt.ui.theme.SpendAntGreenv2

@Composable
fun WelcomeScreen(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onLoginWithOtherUserClick: (() -> Unit)? = null,
    hasLoggedInOnce: Boolean = false,
    lastUserDisplayName: String = "",
    modifier: Modifier = Modifier
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val uiScale = (screenWidth / 424f).coerceIn(0.93f, 1.12f)
    val antWidthFraction = when {
        screenWidth < 360 -> 0.62f
        screenWidth < 400 -> 0.65f
        else -> 0.68f
    }
    val antBottomInset = when {
        screenHeight < 700 -> 250f
        screenHeight < 800 -> 230f
        else -> 210f
    }
    val antYOffset = 0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SpendAntGreenv2)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = (28 * uiScale).dp)
                .padding(bottom = (antBottomInset * uiScale).dp)
                .zIndex(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = "SpendAnt",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .requiredWidth((screenWidth * 1.5f).dp)
                    .padding(top = (120 * uiScale).dp, bottom = (15 * uiScale).dp)
            )

            Spacer(modifier = Modifier.height((32 * uiScale).dp))

            val greetingName = if (lastUserDisplayName.isNotEmpty()) lastUserDisplayName else "there"

            Text(
                text = "Hi $greetingName.",
                fontSize = (18 * uiScale).sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = SpendAntFontFamily,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height((12 * uiScale).dp))

            BlackButton(
                text = "Login with FingerPrint",
                onClick = onLoginClick,
                width = (260 * uiScale).dp,
                height = (48 * uiScale).dp,
                cornerRadius = (12 * uiScale).dp,
                fontSize = (16 * uiScale).sp
            )

            Spacer(modifier = Modifier.height((12 * uiScale).dp))

            TextButton(onClick = { (onLoginWithOtherUserClick ?: onRegisterClick).invoke() }) {
                Text(
                    text = "Login with another user",
                    color = Color.Black,
                    fontSize = 15.sp,
                    fontFamily = SpendAntFontFamily,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Image(
            painter = painterResource(id = R.drawable.ant_login),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .zIndex(0f)
                .align(Alignment.BottomStart)
                .offset(x = (-4).dp, y = (antYOffset * uiScale).dp)
                .fillMaxWidth(antWidthFraction)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    WelcomeScreen(
        onLoginClick = {},
        onRegisterClick = {},
        hasLoggedInOnce = true,
        lastUserDisplayName = "Bob"
    )
}
