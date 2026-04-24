package com.example.spendantt.ui.screens.budget

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.spendantt.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    currentDisplayName: String,
    currentAvatarPath: String?,
    onBackClick: () -> Unit,
    onSaveClick: (String, Uri?) -> Unit,
    modifier: Modifier = Modifier
) {
    var displayName by remember { mutableStateOf(currentDisplayName) }
    var selectedAvatarUri by remember { mutableStateOf<Uri?>(null) }
    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedAvatarUri = uri
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpendAntBackground)
    ) {
        // ── HEADER CON BACK BUTTON ──────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpendAntGreen)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = SpendAntTextOnGreen
                )
            }

            Text(
                text = "Edit Profile",
                color = SpendAntTextOnGreen,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            )

            TextButton(
                onClick = { onSaveClick(displayName, selectedAvatarUri) }
            ) {
                Text(
                    text = "Save",
                    color = SpendAntTextOnGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            // ── AVATAR CON BOTÓN DE CÁMARA ──────────────────────────
            Box(
                contentAlignment = Alignment.Center
            ) {
                // Avatar de fondo
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFCDD2)),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        selectedAvatarUri != null -> {
                            AsyncImage(
                                model = selectedAvatarUri,
                                contentDescription = "Avatar preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        !currentAvatarPath.isNullOrBlank() -> {
                            AsyncImage(
                                model = currentAvatarPath,
                                contentDescription = "Avatar preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        else -> {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Avatar",
                                tint = Color(0xFFE57373),
                                modifier = Modifier.size(60.dp)
                            )
                        }
                    }
                }

                // Botón de cámara en la esquina inferior derecha
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .offset(x = 30.dp, y = 30.dp)
                        .clip(CircleShape)
                        .background(SpendAntBlack)
                        .border(2.dp, SpendAntBackground, CircleShape)
                        .clickable {
                            avatarPickerLauncher.launch("image/*")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Cambiar foto",
                        tint = SpendAntWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── CAMPO DE USERNAME ───────────────────────────────────
            Text(
                text = "Display Name",
                color = SpendAntTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpendAntGreen,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                    focusedContainerColor = SpendAntBackground,
                    unfocusedContainerColor = SpendAntBackground
                ),
                placeholder = {
                    Text(
                        text = "Enter your display name",
                        color = SpendAntTextSecondary
                    )
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── INFORMACIÓN ADICIONAL ───────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SpendAntWhite
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Profile Information",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SpendAntTextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "You can update your display name and profile picture. Changes will be reflected across the app.",
                        fontSize = 14.sp,
                        color = SpendAntTextSecondary,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}
