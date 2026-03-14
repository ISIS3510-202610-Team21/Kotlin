package com.example.spendantt.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.painterResource
import com.example.spendantt.R
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
    val uiScale = (screenWidth / 424f).coerceIn(0.88f, 1.12f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpendAntGreenv2)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = (40 * uiScale).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "SpendAnt",
                fontSize = (52 * uiScale).sp,
                fontWeight = FontWeight.ExtraBold,
                fontStyle = FontStyle.Italic,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = (132 * uiScale).dp)
            )

            Text(
                text = "Your Finance Pal",
                fontSize = (16 * uiScale).sp,
                fontStyle = FontStyle.Italic,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = (4 * uiScale).dp, bottom = (70 * uiScale).dp)
            )

            OutlinedTextField(
                value = viewModel.username.value,
                onValueChange = { viewModel.onUsernameChange(it) },
                placeholder = {
                    Text("Username", color = Color(0xFF4F545A), fontSize = (14 * uiScale).sp)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height((56 * uiScale).dp)
                    .padding(bottom = (8 * uiScale).dp)
                    .clip(RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp),
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
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = (14 * uiScale).sp)
            )
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = (20 * uiScale).dp),
                thickness = 1.dp,
                color = Color(0x33000000)
            )

            OutlinedTextField(
                value = viewModel.password.value,
                onValueChange = { viewModel.onPasswordChange(it) },
                placeholder = {
                    Text("Password", color = Color(0xFF4F545A), fontSize = (14 * uiScale).sp)
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
                                "Ocultar contrasena"
                            } else {
                                "Mostrar contrasena"
                            },
                            tint = Color(0xFF111111),
                            modifier = Modifier.size((24 * uiScale).dp)
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height((56 * uiScale).dp)
                    .clip(RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp),
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
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = (14 * uiScale).sp)
            )
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = (36 * uiScale).dp),
                thickness = 1.dp,
                color = Color(0x33000000)
            )

            if (viewModel.errorMessage.value.isNotEmpty()) {
                Text(
                    text = "Error: ${viewModel.errorMessage.value}",
                    color = Color(0xFF7D0000),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Button(
                onClick = {
                    if (useBiometricMode) {
                        onBiometricLoginClick?.invoke()
                    } else {
                        viewModel.login(onLoginSuccess)
                    }
                },
                modifier = Modifier
                    .width((132 * uiScale).dp)
                    .height((58 * uiScale).dp),
                shape = RoundedCornerShape((16 * uiScale).dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                enabled = if (useBiometricMode) true else !viewModel.isLoading.value
            ) {
                if (!useBiometricMode && viewModel.isLoading.value) {
                    CircularProgressIndicator(
                        modifier = Modifier.size((20 * uiScale).dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = loginButtonText,
                        fontSize = (30 * uiScale).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = (20 * uiScale).dp),
                horizontalArrangement = Arrangement.Center
            ) {
                if (onRegisterClick != null) {
                    TextButton(onClick = onRegisterClick) {
                        Text(text = "Register", color = Color.Black, fontSize = 16.sp)
                    }
                }

                if (showManualFallbackAction && onUseManualLogin != null) {
                    TextButton(onClick = onUseManualLogin) {
                        Text(text = "Usar login manual", color = Color.Black, fontSize = 16.sp)
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
            painter = painterResource(id = R.drawable.ant_goal_side),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.66f)
        )
    }
}
