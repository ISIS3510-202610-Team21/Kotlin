package com.example.spendantt.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.zIndex
import com.example.spendantt.R
import com.example.spendantt.ui.components.BlackButton
import com.example.spendantt.ui.components.SpendAntButton
import com.example.spendantt.ui.theme.SpendAntFontFamily
import com.example.spendantt.ui.theme.SpendAntGreenv2
import com.example.spendantt.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: (Int) -> Unit,
    onRegisterClick: (()-> Unit)? = null,
    loginButtonText: String = "Login",
    useBiometricMode: Boolean = false,
    onBiometricLoginClick: (() -> Unit)? = null,
    showManualFallbackAction: Boolean = false,
    onUseManualLogin: (() -> Unit)? = null,
    manualFieldsEnabled: Boolean = true
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
        modifier = Modifier
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
                    .padding(top = (100 * uiScale).dp, bottom = (15 * uiScale).dp)
            )

            OutlinedTextField(
                value = viewModel.username.value,
                onValueChange = { viewModel.onUsernameChange(it) },
                placeholder = {
                    Text(
                        "Username",
                        color = Color(0xFF4F545A),
                        fontSize = (14 * uiScale).sp,
                        fontFamily = SpendAntFontFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height((56 * uiScale).dp)
                    .clip(RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(3.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFFD1E7DA),
                    focusedContainerColor = Color(0xFFD1E7DA),
                    disabledContainerColor = Color(0xFFD1E7DA),
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    disabledBorderColor = Color.Transparent,
                    cursorColor = Color.Black,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                enabled = manualFieldsEnabled && !viewModel.isLoading.value,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = SpendAntFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (14 * uiScale).sp,
                    lineHeight = (16 * uiScale).sp // Reducir el interlineado
                )
            )

            Spacer(modifier = Modifier.height((14 * uiScale).dp))

            OutlinedTextField(
                value = viewModel.password.value,
                onValueChange = { viewModel.onPasswordChange(it) },
                placeholder = {
                    Text(
                        "Password",
                        color = Color(0xFF4F545A),
                        fontSize = (14 * uiScale).sp,
                        fontFamily = SpendAntFontFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                visualTransformation = if (viewModel.showPassword.value) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(
                        onClick = { viewModel.toggleShowPassword() },
                        enabled = manualFieldsEnabled && !viewModel.isLoading.value
                    ) {
                        Icon(
                            imageVector = if (viewModel.showPassword.value) {
                                Icons.Filled.Visibility
                            } else {
                                Icons.Filled.VisibilityOff
                            },
                            contentDescription = if (viewModel.showPassword.value) {
                                "Hide password"
                            } else {
                                "Show password"
                            },
                            tint = Color(0xFF111111),
                            modifier = Modifier.size((20 * uiScale).dp)
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height((56 * uiScale).dp)
                    .clip(RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(3.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFFD1E7DA),
                    focusedContainerColor = Color(0xFFD1E7DA),
                    disabledContainerColor = Color(0xFFD1E7DA),
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    disabledBorderColor = Color.Transparent,
                    cursorColor = Color.Black,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                enabled = manualFieldsEnabled && !viewModel.isLoading.value,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = SpendAntFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (14 * uiScale).sp,
                    lineHeight = (18 * uiScale).sp
                )
            )

            Spacer(modifier = Modifier.height((32 * uiScale).dp))

            if (viewModel.errorMessage.value.isNotEmpty()) {
                Text(
                    text = "${viewModel.errorMessage.value}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = SpendAntFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            BlackButton(
                text = loginButtonText,
                onClick = {
                    if (useBiometricMode) {
                        onBiometricLoginClick?.invoke()
                    } else {
                        viewModel.login(onLoginSuccess)
                    }
                },
                enabled = if (useBiometricMode) true else !viewModel.isLoading.value,
                isLoading = !useBiometricMode && viewModel.isLoading.value,
                width = (120 * uiScale).dp,
                height = (48 * uiScale).dp,
                cornerRadius = (12 * uiScale).dp,
                fontSize = (16 * uiScale).sp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = (1 * uiScale).dp),
                horizontalArrangement = Arrangement.Center
            ) {
                if (onRegisterClick != null) {
                    TextButton(onClick = onRegisterClick) {
                        Text(
                            text = "Register",
                            color = Color.Black,
                            fontSize = 15.sp,
                            fontFamily = SpendAntFontFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (showManualFallbackAction && onUseManualLogin != null) {
                    TextButton(onClick = onUseManualLogin) {
                        Text(
                            text = "Use manual login",
                            color = Color.Black,
                            fontSize = 16.sp,
                            fontFamily = SpendAntFontFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .size(16.dp)
                        )
                    }
                }
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
fun LoginScreenPreview() {

    val context = LocalContext.current
    val viewModel = LoginViewModel(context)

    LoginScreen(
        viewModel = viewModel,
        onLoginSuccess = {},
        onRegisterClick = {},
        onBiometricLoginClick = {},
        onUseManualLogin = {}
    )
}