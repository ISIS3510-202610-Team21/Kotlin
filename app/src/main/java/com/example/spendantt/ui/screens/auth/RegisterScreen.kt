package com.example.spendantt.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.spendantt.R
import com.example.spendantt.ui.components.BlackButton
import com.example.spendantt.ui.theme.SpendAntFontFamily
import com.example.spendantt.ui.theme.SpendAntGreenv2
import com.example.spendantt.viewmodel.RegisterViewModel

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onRegisterSuccess: (Int) -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val username by viewModel.username
    val email by viewModel.email
    val password by viewModel.password
    val showPassword by viewModel.showPassword
    val errorMessage by viewModel.errorMessage
    val isLoading by viewModel.isLoading
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
                    .padding(top = (70 * uiScale).dp, bottom = (15 * uiScale).dp)
            )

            OutlinedTextField(
                value = username,
                onValueChange = viewModel::onUsernameChange,
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
                enabled = !isLoading,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = SpendAntFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (14 * uiScale).sp,
                    lineHeight = (16 * uiScale).sp
                )
            )

            Spacer(modifier = Modifier.height((14 * uiScale).dp))

            OutlinedTextField(
                value = email,
                onValueChange = viewModel::onEmailChange,
                placeholder = {
                    Text(
                        "Email",
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
                enabled = !isLoading,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = SpendAntFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (14 * uiScale).sp,
                    lineHeight = (16 * uiScale).sp
                )
            )

            Spacer(modifier = Modifier.height((14 * uiScale).dp))

            OutlinedTextField(
                value = password,
                onValueChange = viewModel::onPasswordChange,
                placeholder = {
                    Text(
                        "Password",
                        color = Color(0xFF4F545A),
                        fontSize = (14 * uiScale).sp,
                        fontFamily = SpendAntFontFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                visualTransformation = if (showPassword) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = viewModel::toggleShowPassword, enabled = !isLoading) {
                        Icon(
                            imageVector = if (showPassword) {
                                Icons.Filled.Visibility
                            } else {
                                Icons.Filled.VisibilityOff
                            },
                            contentDescription = if (showPassword) {
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
                enabled = !isLoading,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = SpendAntFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (14 * uiScale).sp,
                    lineHeight = (18 * uiScale).sp
                )
            )

            Spacer(modifier = Modifier.height((32 * uiScale).dp))

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = SpendAntFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            BlackButton(
                text = "Register",
                onClick = { viewModel.register(onRegisterSuccess) },
                enabled = !isLoading,
                isLoading = isLoading,
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
                TextButton(onClick = onBackToLogin) {
                    Text(
                        text = "Login",
                        color = Color.Black,
                        fontSize = 15.sp,
                        fontFamily = SpendAntFontFamily,
                        fontWeight = FontWeight.SemiBold
                    )
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



