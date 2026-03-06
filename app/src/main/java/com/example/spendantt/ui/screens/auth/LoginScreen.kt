package com.example.spendantt.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spendantt.ui.theme.SpendAntGreen
import com.example.spendantt.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: (Int) -> Unit,
    loginButtonText: String = "Login",
    useBiometricMode: Boolean = false,
    onBiometricLoginClick: (() -> Unit)? = null,
    showManualFallbackAction: Boolean = false,
    onUseManualLogin: (() -> Unit)? = null,
    manualFieldsEnabled: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpendAntGreen)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "SpendAnt",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Your Finance Pal",
            fontSize = 16.sp,
            fontStyle = FontStyle.Italic,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        OutlinedTextField(
            value = viewModel.username.value,
            onValueChange = { viewModel.onUsernameChange(it) },
            placeholder = { Text("Username") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent
            ),
            enabled = manualFieldsEnabled && !viewModel.isLoading.value,
            singleLine = true
        )

        OutlinedTextField(
            value = viewModel.password.value,
            onValueChange = { viewModel.onPasswordChange(it) },
            placeholder = { Text("Password") },
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
                        }
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent
            ),
            enabled = manualFieldsEnabled && !viewModel.isLoading.value,
            singleLine = true
        )

        if (viewModel.errorMessage.value.isNotEmpty()) {
            Text(
                text = viewModel.errorMessage.value,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
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
                .width(200.dp)
                .height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            enabled = if (useBiometricMode) true else !viewModel.isLoading.value
        ) {
            if (!useBiometricMode && viewModel.isLoading.value) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = SpendAntGreen,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = loginButtonText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        if (showManualFallbackAction && onUseManualLogin != null) {
            TextButton(onClick = onUseManualLogin) {
                Text(text = "Usar login manual", color = Color.Black)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "\uD83D\uDC1C",
            fontSize = 120.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )
    }
}
