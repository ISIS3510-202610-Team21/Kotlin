package com.example.spendantt.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spendantt.R
import com.example.spendantt.ui.theme.*
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
            Spacer(modifier = Modifier.height(80.dp))

            // ── TÍTULO ────────────────────────────────────────
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

            Spacer(modifier = Modifier.height(40.dp))

            // ── USERNAME ──────────────────────────────────────
            OutlinedTextField(
                value = username,
                onValueChange = viewModel::onUsernameChange,
                placeholder = { Text("Username", color = SpendAntTextSecondary) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpendAntGreenLight,
                    unfocusedBorderColor = SpendAntGreenLight,
                    focusedContainerColor = SpendAntGreenLight,
                    unfocusedContainerColor = SpendAntGreenLight,
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── EMAIL ─────────────────────────────────────────
            OutlinedTextField(
                value = email,
                onValueChange = viewModel::onEmailChange,
                placeholder = { Text("Email", color = SpendAntTextSecondary) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpendAntGreenLight,
                    unfocusedBorderColor = SpendAntGreenLight,
                    focusedContainerColor = SpendAntGreenLight,
                    unfocusedContainerColor = SpendAntGreenLight,
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── PASSWORD ──────────────────────────────────────
            OutlinedTextField(
                value = password,
                onValueChange = viewModel::onPasswordChange,
                placeholder = { Text("Password", color = SpendAntTextSecondary) },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = viewModel::toggleShowPassword) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = SpendAntTextSecondary
                        )
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpendAntGreenLight,
                    unfocusedBorderColor = SpendAntGreenLight,
                    focusedContainerColor = SpendAntGreenLight,
                    unfocusedContainerColor = SpendAntGreenLight,
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── ERROR ─────────────────────────────────────────
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = SpendAntError,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── BOTÓN REGISTER ────────────────────────────────
            Button(
                onClick = { viewModel.register(onRegisterSuccess) },
                enabled = !isLoading,
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpendAntBlack,
                    contentColor = SpendAntWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = SpendAntWhite, modifier = Modifier.size(20.dp))
                } else {
                    Text(text = "Register", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── VOLVER AL LOGIN ───────────────────────────────
            TextButton(onClick = onBackToLogin) {
                Text(
                    text = "Already have an account? Login",
                    color = SpendAntBlack,
                    fontSize = 14.sp
                )
            }
        }

        // ── HORMIGA ABAJO ─────────────────────────────────────
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