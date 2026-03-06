package com.example.spendantt.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spendantt.ui.theme.SpendAntGreen
import com.example.spendantt.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpendAntGreen)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ── TÍTULO ────────────────────────────────────────
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

        // ── CAMPO USERNAME ─────────────────────────────────
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
            enabled = !viewModel.isLoading.value,
            singleLine = true
        )

        // ── CAMPO PASSWORD ─────────────────────────────────
        OutlinedTextField(
            value = viewModel.password.value,
            onValueChange = { viewModel.onPasswordChange(it) },
            placeholder = { Text("Password") },
            visualTransformation = if (viewModel.showPassword.value)
                VisualTransformation.None
            else
                PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { viewModel.toggleShowPassword() }) {
                    Icon(
                        imageVector = if (viewModel.showPassword.value)
                            Icons.Filled.Visibility
                        else
                            Icons.Filled.VisibilityOff,
                        contentDescription = if (viewModel.showPassword.value)
                            "Ocultar contraseña"
                        else
                            "Mostrar contraseña"
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
            enabled = !viewModel.isLoading.value,
            singleLine = true
        )

        // ── MENSAJE DE ERROR ───────────────────────────────
        if (viewModel.errorMessage.value.isNotEmpty()) {
            Text(
                text = viewModel.errorMessage.value,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // ── BOTÓN LOGIN ────────────────────────────────────
        Button(
            onClick = { viewModel.login(onLoginSuccess) },
            modifier = Modifier
                .width(140.dp)
                .height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black
            ),
            enabled = !viewModel.isLoading.value
        ) {
            if (viewModel.isLoading.value) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = SpendAntGreen,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    "Login",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // ── MASCOTA (placeholder) ────────────────────────────
        Text(
            text = "🐜",
            fontSize = 120.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )
    }
}