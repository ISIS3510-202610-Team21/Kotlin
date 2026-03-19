package com.example.spendantt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spendantt.ui.theme.*

/**
 * Header verde con título, botón X (cerrar) y ✓ (confirmar)
 * Usado en: New Income, Budget and Income, etc.
 */
@Composable
fun SpendAntHeader(
    title: String,
    onClose: () -> Unit,
    onConfirm: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(SpendAntGreen)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        // Botón X - izquierda
        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cerrar",
                tint = SpendAntTextOnGreen
            )
        }

        // Título - centro
        Text(
            text = title,
            color = SpendAntTextOnGreen,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center)
        )

        // Botón ✓ - derecha (opcional)
        if (onConfirm != null) {
            IconButton(
                onClick = onConfirm,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Confirmar",
                    tint = SpendAntTextOnGreen
                )
            }
        }
    }
}

/**
 * Campo de texto estilo SpendAnt: fondo verde claro, sin borde, esquinas redondeadas
 */
@Composable
fun SpendAntTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    leadingText: String? = null,         // Para mostrar "$" antes del monto
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = SpendAntTextSecondary,
                    fontSize = 15.sp
                )
            },
            leadingIcon = if (leadingText != null) {
                {
                    Text(
                        text = leadingText,
                        color = SpendAntTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else null,
            isError = errorMessage != null,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SpendAntGreenLight,
                unfocusedContainerColor = SpendAntGreenLight,
                focusedBorderColor = SpendAntGreen,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = SpendAntBlack,
                unfocusedTextColor = SpendAntBlack,
                focusedPlaceholderColor = SpendAntBlack,
                unfocusedPlaceholderColor = SpendAntBlack,
                cursorColor = SpendAntBlack,
                errorBorderColor = SpendAntError,
                errorContainerColor = Color(0xFFFFEBEE)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = SpendAntError,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
            )
        }
    }
}

/**
 * Botón negro principal de SpendAnt
 */
@Composable
fun SpendAntButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(50.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SpendAntBlack,
            contentColor = SpendAntTextOnGreen,
            disabledContainerColor = Color(0xFF9E9E9E)
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = SpendAntWhite,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Toggle de dos opciones estilo SpendAnt (Just Once / Frequently)
 */
@Composable
fun SpendAntToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = index == selectedIndex
            Surface(
                onClick = { onSelect(index) },
                shape = RoundedCornerShape(50.dp),
                color = if (isSelected) SpendAntGreen else Color.Transparent,
                border = if (!isSelected) ButtonDefaults.outlinedButtonBorder else null
            ) {
                Text(
                    text = option,
                    color = if (isSelected) SpendAntTextOnGreen else SpendAntTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        }
    }
}
