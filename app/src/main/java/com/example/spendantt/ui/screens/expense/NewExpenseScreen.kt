package com.example.spendantt.ui.screens.expense

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.spendantt.R
import com.example.spendantt.ui.theme.*
import com.example.spendantt.viewmodel.NewExpenseViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewExpenseScreen(
    viewModel: NewExpenseViewModel,
    onClose: () -> Unit,
    onSaved: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // ── URI temporal para foto con cámara ─────────────────────
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // ── Mostrar bottom sheet de opciones ──────────────────────
    var showReceiptOptions by remember { mutableStateOf(false) }

    // ── Navegar a Home al guardar ──────────────────────────────
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaved()
    }

    // ── LAUNCHERS ─────────────────────────────────────────────

    // Cámara
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { viewModel.onReceiptSelected(it) }
        }
    }

    // Galería
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onReceiptSelected(it) }
    }

    // Archivo (PDF o imagen)
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onReceiptSelected(it) }
    }

    // Permiso cámara
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val photoFile = File.createTempFile("receipt_", ".jpg", context.cacheDir)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                photoFile
            )
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        }
    }

    // ── UI ────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize().background(SpendAntWhite)) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            // ── HEADER ────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SpendAntGreen)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = SpendAntBlack
                    )
                }
                Text(
                    text = "New Expense",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SpendAntBlack,
                    modifier = Modifier.align(Alignment.Center)
                )
                IconButton(
                    onClick = { viewModel.saveExpense() },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save",
                        tint = SpendAntBlack
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── CAMPOS ────────────────────────────────────────
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // Nombre
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = { viewModel.onNameChange(it) },
                    placeholder = { Text("Expense name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SpendAntGreen,
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    )
                )

                // Monto
                OutlinedTextField(
                    value = uiState.amount,
                    onValueChange = { viewModel.onAmountChange(it) },
                    placeholder = { Text("$ Amount") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SpendAntGreen,
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    )
                )

                // Label (sin funcionalidad por ahora)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(SpendAntGreen)
                        .clickable { /* TODO: labels */ }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "+ Label",
                        color = SpendAntBlack,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── SCAN RECEIPT ──────────────────────────────
                Button(
                    onClick = { showReceiptOptions = true },
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpendAntBlack,
                        contentColor = SpendAntWhite
                    ),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_scan_receipt),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan Receipt", fontWeight = FontWeight.SemiBold)
                }

                // Mostrar URI de recibo seleccionado
                if (uiState.receiptImageUri != null) {
                    Text(
                        text = "✓ Receipt selected",
                        color = SpendAntGreen,
                        fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── FOOTER: fecha, hora, ubicación ────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Fecha
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_calendar),
                            contentDescription = "Date",
                            modifier = Modifier.size(16.dp),
                            tint = SpendAntBlack
                        )
                        Text(
                            text = uiState.date.ifEmpty { "--/--/----" },
                            fontSize = 12.sp,
                            color = SpendAntBlack
                        )
                    }

                    // Hora
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_clock),
                            contentDescription = "Time",
                            modifier = Modifier.size(16.dp),
                            tint = SpendAntBlack
                        )
                        Text(
                            text = uiState.time.ifEmpty { "--:--" },
                            fontSize = 12.sp,
                            color = SpendAntBlack
                        )
                    }

                    // Ubicación
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_location),
                            contentDescription = "Location",
                            modifier = Modifier.size(16.dp),
                            tint = SpendAntBlack
                        )
                        Text(
                            text = if (uiState.locationName.isNotEmpty())
                                uiState.locationName.take(12) + if (uiState.locationName.length > 12) "..." else ""
                            else "Location...",
                            fontSize = 12.sp,
                            color = SpendAntBlack
                        )
                    }
                }
            }

            // Error
            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }
        }

        // Loading overlay
        if (uiState.isSaving) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SpendAntGreen)
            }
        }
    }

    // ── BOTTOM SHEET: opciones de recibo ──────────────────────
    if (showReceiptOptions) {
        ModalBottomSheet(
            onDismissRequest = { showReceiptOptions = false },
            containerColor = SpendAntWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "New Receipt",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SpendAntBlack
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {

                    // ── CÁMARA ────────────────────────────────
                    ReceiptOptionButton(
                        iconRes = R.drawable.ic_camera,
                        label = "Camera",
                        onClick = {
                            showReceiptOptions = false
                            val hasCameraPermission = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasCameraPermission) {
                                val photoFile = File.createTempFile("receipt_", ".jpg", context.cacheDir)
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    photoFile
                                )
                                cameraImageUri = uri
                                cameraLauncher.launch(uri)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    )

                    // ── GALERÍA ───────────────────────────────
                    ReceiptOptionButton(
                        iconRes = R.drawable.ic_gallery,
                        label = "Gallery",
                        onClick = {
                            showReceiptOptions = false
                            galleryLauncher.launch("image/*")
                        }
                    )

                    // ── ARCHIVO ───────────────────────────────
                    ReceiptOptionButton(
                        iconRes = R.drawable.ic_file,
                        label = "File",
                        onClick = {
                            showReceiptOptions = false
                            fileLauncher.launch("*/*")
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ReceiptOptionButton(
    iconRes: Int,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SpendAntGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                tint = SpendAntBlack,
                modifier = Modifier.size(32.dp)
            )
        }
        Text(
            text = label,
            fontSize = 12.sp,
            color = SpendAntBlack,
            fontWeight = FontWeight.SemiBold
        )
    }
}