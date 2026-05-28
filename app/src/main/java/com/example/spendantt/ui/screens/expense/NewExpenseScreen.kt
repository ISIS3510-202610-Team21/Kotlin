package com.example.spendantt.ui.screens.expense

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.spendantt.BuildConfig
import com.example.spendantt.R
import com.example.spendantt.data.currency.CurrencyProvider
import com.example.spendantt.data.voice.VoiceParseResult
import com.example.spendantt.ui.components.NoInternetBanner
import com.example.spendantt.viewmodel.VoiceInputViewModel
import com.example.spendantt.ui.screens.labels.LabelsScreen
import com.example.spendantt.ui.theme.SpendAntBlack
import com.example.spendantt.ui.theme.SpendAntFontFamily
import com.example.spendantt.ui.theme.SpendAntGreen
import com.example.spendantt.ui.theme.SpendAntWhite
import com.example.spendantt.util.ConnectivityObserver
import com.example.spendantt.viewmodel.NewExpenseViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.maps.model.LatLng
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.launch
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private enum class ExpenseScreenState {
    FORM,
    LABELS,
    LABEL_PROMPT,
    MAP_PICKER,
    VOICE_INPUT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewExpenseScreen(
    viewModel: NewExpenseViewModel,
    onClose: () -> Unit,
    onSaved: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isConnected by ConnectivityObserver.isConnected.collectAsState()
    var currentScreen by remember { mutableStateOf(ExpenseScreenState.FORM) }
    var showOfflineMapPopup by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val voiceViewModel = remember { VoiceInputViewModel(context.applicationContext) }

    DisposableEffect(voiceViewModel) {
        onDispose { voiceViewModel.cancel() }
    }

    LaunchedEffect(uiState.showLabelPrompt) {
        if (uiState.showLabelPrompt) {
            currentScreen = ExpenseScreenState.LABEL_PROMPT
        }
    }

    if (showOfflineMapPopup) {
        AlertDialog(
            onDismissRequest = { showOfflineMapPopup = false },
            title = { Text("No internet connection") },
            text = { Text("Cannot open the map without internet connection. Please connect to the internet and try again.") },
            confirmButton = {
                TextButton(onClick = { showOfflineMapPopup = false }) { Text("OK") }
            }
        )
    }

    if (uiState.showOfflineAutocat) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissOfflineAutocat() },
            title = { Text("No internet connection") },
            text = { Text("Auto-categorization is not available without internet. Your expense has been saved locally and will sync when connection is restored.") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissOfflineAutocat() }) { Text("OK") }
            }
        )
    }

    when (currentScreen) {
        ExpenseScreenState.FORM -> {
            NewExpenseFormContent(
                viewModel = viewModel,
                onClose = onClose,
                onSaved = onSaved,
                onLabelClick = { currentScreen = ExpenseScreenState.LABELS },
                onLocationClick = {
                    if (isConnected) {
                        currentScreen = ExpenseScreenState.MAP_PICKER
                    } else {
                        showOfflineMapPopup = true
                    }
                },
                onVoiceInputClick = { currentScreen = ExpenseScreenState.VOICE_INPUT }
            )
        }

        ExpenseScreenState.LABELS -> {
            LabelsScreen(
                labelsGroupedByCategory = uiState.labelsGroupedByCategory,
                selectedLabelIds = uiState.selectedLabelIds,
                onLabelToggle = { label -> viewModel.toggleLabel(label) },
                onDone = { currentScreen = ExpenseScreenState.FORM },
                onClose = { currentScreen = ExpenseScreenState.FORM }
            )
        }

        ExpenseScreenState.LABEL_PROMPT -> {
            LabelPromptScreen(
                viewModel = viewModel,
                onSaved = onSaved
            )
        }

        ExpenseScreenState.MAP_PICKER -> {
            MapPickerScreen(
                initialLatitude = uiState.latitude,
                initialLongitude = uiState.longitude,
                onClose = { currentScreen = ExpenseScreenState.FORM },
                onConfirm = { latLng ->
                    viewModel.onLocationSelected(latLng.latitude, latLng.longitude)
                    currentScreen = ExpenseScreenState.FORM
                }
            )
        }

        ExpenseScreenState.VOICE_INPUT -> {
            VoiceInputScreen(
                voiceViewModel = voiceViewModel,
                onClose = {
                    voiceViewModel.stopListening()
                    currentScreen = ExpenseScreenState.FORM
                },
                onConfirm = { result ->
                    viewModel.autoFillFromVoice(result)
                    voiceViewModel.clearDraft()
                    currentScreen = ExpenseScreenState.FORM
                }
            )
        }
    }
}

@Composable
private fun LabelPromptScreen(
    viewModel: NewExpenseViewModel,
    onSaved: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaved()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpendAntWhite)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(title = "Labels", onClose = {}, onConfirm = {})

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Hey! We couldn't categorize \"${uiState.name}\".",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SpendAntFontFamily,
                    color = SpendAntBlack,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Add a label so we can learn for next time!",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    fontFamily = SpendAntFontFamily,
                    textAlign = TextAlign.Center
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                LabelsScreen(
                    labelsGroupedByCategory = uiState.labelsGroupedByCategory,
                    selectedLabelIds = uiState.selectedLabelIds,
                    onLabelToggle = { label -> viewModel.toggleLabel(label) },
                    onDone = { viewModel.savePendingLabels() },
                    onClose = { viewModel.dismissLabelPrompt() },
                    showHeader = false
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun NewExpenseFormContent(
    viewModel: NewExpenseViewModel,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    onLabelClick: () -> Unit,
    onLocationClick: () -> Unit,
    onVoiceInputClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencyState by CurrencyProvider.uiState.collectAsState()
    val context = LocalContext.current

    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var showReceiptOptions by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            viewModel.consumeSaved()
            onSaved()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { viewModel.onReceiptSelected(it) }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onReceiptSelected(it) }
    }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onReceiptSelected(it) }
    }

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

    val isConnected by ConnectivityObserver.isConnected.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpendAntWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 92.dp)
        ) {
            TopBar(
                title = "New Expense",
                onClose = onClose,
                onConfirm = { viewModel.saveExpense() }
            )

            NoInternetBanner(visible = !isConnected)

            Spacer(modifier = Modifier.height(96.dp))

            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 3.dp,
                            shape = RoundedCornerShape(0.dp),
                            clip = false
                        )
                        .background(Color(0xFFE2F8E4))
                ) {
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = { viewModel.onNameChange(it) },
                        placeholder = { Text("Expense name", color = Color(0xFF6D6D6D)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(0.dp),
                        colors = expenseEntryFieldColors()
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 3.dp,
                            shape = RoundedCornerShape(0.dp),
                            clip = false
                        )
                        .background(Color(0xFFE2F8E4))
                ) {
                    OutlinedTextField(
                        value = uiState.amount,
                        onValueChange = { viewModel.onAmountChange(it) },
                        placeholder = { Text("${currencyState.activeCurrency} 0", color = Color(0xFF6D6D6D)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(0.dp),
                        colors = expenseEntryFieldColors()
                    )
                }

                RegretExpenseRow(
                    checked = uiState.regretExpense,
                    onCheckedChange = viewModel::onRegretExpenseChange
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(SpendAntGreen)
                            .clickable { onLabelClick() }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "+ Label",
                            color = SpendAntBlack,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = SpendAntFontFamily,
                            fontSize = 14.sp
                        )
                    }

                    uiState.selectedLabels.forEach { label ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(SpendAntGreen)
                                .clickable { viewModel.toggleLabel(label) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = SpendAntBlack,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = label.name,
                                    color = SpendAntBlack,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = SpendAntFontFamily,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { showReceiptOptions = true },
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SpendAntBlack,
                            contentColor = SpendAntWhite
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_scan_receipt),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan Receipt", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onVoiceInputClick,
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SpendAntBlack,
                            contentColor = SpendAntWhite
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice input",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (uiState.receiptImageUri != null) {
                    // CACHING | Dilan | 5pts | Coil AsyncImage: carga el URI del recibo de forma asíncrona
                    // con caché en memoria y disco automáticos — evita redecodificar el bitmap en cada
                    // recomposición; Coil gestiona el ciclo de vida y cancela la carga si el composable sale
                    AsyncImage(
                        model = uiState.receiptImageUri,
                        contentDescription = "Receipt preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                    Text(
                        text = "Receipt selected. OCR can update amount, date, time and merchant.",
                        color = SpendAntGreen,
                        fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        textAlign = TextAlign.Center
                    )
                }

                if (uiState.isUploadingImage) {
                    InlineLoading(text = "Uploading receipt...")
                }

                if (uiState.isProcessingOcr) {
                    InlineLoading(text = "Reading receipt...")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }
        }

        if (uiState.isSaving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SpendAntGreen)
            }
        }

        ExpenseBottomMetaBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            date = uiState.date.ifEmpty { "Select date" },
            time = uiState.time.ifEmpty { "Select time" },
            location = uiState.locationName.ifEmpty { "Pick location" },
            onDateClick = {
                showDatePicker(
                    context = context,
                    initialDate = uiState.date,
                    onDateSelected = viewModel::onDateSelected
                )
            },
            onTimeClick = {
                showTimePicker(
                    context = context,
                    initialTime = uiState.time,
                    onTimeSelected = viewModel::onTimeSelected
                )
            },
            onLocationClick = onLocationClick
        )
    }

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
                    ReceiptOptionButton(
                        iconRes = R.drawable.ic_camera,
                        label = "Camera",
                        onClick = {
                            showReceiptOptions = false
                            val hasCameraPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
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

                    ReceiptOptionButton(
                        iconRes = R.drawable.ic_gallery,
                        label = "Gallery",
                        onClick = {
                            showReceiptOptions = false
                            galleryLauncher.launch("image/*")
                        }
                    )

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
internal fun MapPickerScreen(
    initialLatitude: Double?,
    initialLongitude: Double?,
    onClose: () -> Unit,
    onConfirm: (LatLng) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isConnected by ConnectivityObserver.isConnected.collectAsState()
    val defaultLatLng = remember(initialLatitude, initialLongitude) {
        if (initialLatitude != null && initialLongitude != null) {
            LatLng(initialLatitude, initialLongitude)
        } else {
            LatLng(4.7110, -74.0721)
        }
    }
    var searchQuery by remember { mutableStateOf("") }
    var selectedLatLng by remember {
        mutableStateOf<LatLng?>(if (initialLatitude != null && initialLongitude != null) defaultLatLng else null)
    }
    var mapInstance by remember { mutableStateOf<GoogleMap?>(null) }
    val mapView = rememberMapViewWithLifecycle()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            requestCurrentLocation(
                context = context,
                fusedLocationClient = fusedLocationClient,
                onMapClick = { latLng ->
                    selectedLatLng = latLng
                    mapInstance?.let { updateMapSelection(it, defaultLatLng, latLng) }
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpendAntWhite)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                title = "Pick Location",
                onClose = onClose,
                onConfirm = { selectedLatLng?.let(onConfirm) },
                confirmEnabled = isConnected
            )

            if (BuildConfig.MAPS_API_KEY.isBlank()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Add MAPS_API_KEY to local.properties to enable map selection.",
                        color = SpendAntBlack,
                        fontSize = 16.sp,
                        fontFamily = SpendAntFontFamily,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                Box(modifier = Modifier.weight(1f)) {
                    AndroidView(
                        factory = {
                            mapView.apply {
                                getMapAsync { googleMap ->
                                    mapInstance = googleMap
                                    configureMap(googleMap, defaultLatLng) { latLng ->
                                        selectedLatLng = latLng
                                    }
                                    updateMapSelection(googleMap, defaultLatLng, selectedLatLng)
                                }
                            }
                        },
                        update = {
                            mapInstance?.let { googleMap ->
                                updateMapSelection(googleMap, defaultLatLng, selectedLatLng)
                            }
                        }
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    "Search place",
                                    color = Color(0xFF777777),
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                            colors = searchLocationFieldColors()
                        )
                        Button(
                            onClick = {
                                if (searchQuery.isNotBlank()) {
                                    coroutineScope.launch {
                                        searchLocationByName(context, searchQuery)?.let { result ->
                                            val latLng = LatLng(result.first, result.second)
                                            selectedLatLng = latLng
                                            mapInstance?.let { updateMapSelection(it, defaultLatLng, latLng) }
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SpendAntBlack,
                                contentColor = SpendAntWhite
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Go", fontSize = 12.sp)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = SpendAntWhite,
                        tonalElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val hasFinePermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                    val hasCoarsePermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (hasFinePermission || hasCoarsePermission) {
                                        requestCurrentLocation(
                                            context = context,
                                            fusedLocationClient = fusedLocationClient,
                                            onMapClick = { latLng ->
                                                selectedLatLng = latLng
                                                mapInstance?.let { updateMapSelection(it, defaultLatLng, latLng) }
                                            }
                                        )
                                    } else {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SpendAntGreen,
                                    contentColor = SpendAntBlack
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(50.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Text("Get current location", fontSize = 12.sp)
                            }

                            Text(
                                text = if (selectedLatLng == null) "Tap on the map to select a location."
                                else "Selected: ${"%.5f".format(selectedLatLng!!.latitude)}, ${"%.5f".format(selectedLatLng!!.longitude)}",
                                color = SpendAntBlack,
                                fontFamily = SpendAntFontFamily,
                                fontSize = 11.sp
                            )

                            Button(
                                onClick = { selectedLatLng?.let(onConfirm) },
                                enabled = selectedLatLng != null,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SpendAntBlack,
                                    contentColor = SpendAntWhite
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(50.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Text("Save location", fontSize = 12.sp)
                            }
                        }
                    }

                    if (!isConnected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x99000000))
                                .clickable(enabled = true, onClick = {}),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = SpendAntWhite,
                                tonalElevation = 8.dp,
                                modifier = Modifier
                                    .padding(horizontal = 32.dp)
                                    .fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "No internet connection",
                                        color = SpendAntBlack,
                                        fontSize = 16.sp,
                                        fontFamily = SpendAntFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Map functionality has been disabled because there is no internet connection available.",
                                        color = Color(0xFF777777),
                                        fontSize = 13.sp,
                                        fontFamily = SpendAntFontFamily,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun configureMap(
    googleMap: GoogleMap,
    defaultLatLng: LatLng,
    onMapClick: (LatLng) -> Unit
) {
    googleMap.uiSettings.isZoomControlsEnabled = false
    googleMap.uiSettings.isCompassEnabled = true
    googleMap.uiSettings.isMyLocationButtonEnabled = false
    googleMap.moveCamera(
        CameraUpdateFactory.newLatLngZoom(defaultLatLng, 10f)
    )
    googleMap.setOnMapClickListener { latLng ->
        googleMap.clear()
        googleMap.addMarker(
            com.google.android.gms.maps.model.MarkerOptions()
                .position(latLng)
                .title("Expense location")
        )
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        onMapClick(latLng)
    }
}

private fun updateMapSelection(
    googleMap: GoogleMap,
    defaultLatLng: LatLng,
    selectedLatLng: LatLng?
) {
    googleMap.clear()
    val markerLatLng = selectedLatLng ?: defaultLatLng
    if (selectedLatLng != null) {
        googleMap.addMarker(
            com.google.android.gms.maps.model.MarkerOptions()
                .position(selectedLatLng)
                .title("Expense location")
        )
    }
    googleMap.moveCamera(
        CameraUpdateFactory.newLatLngZoom(
            markerLatLng,
            if (selectedLatLng != null) 15f else 10f
        )
    )
}

private fun requestCurrentLocation(
    context: android.content.Context,
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
    onMapClick: (LatLng) -> Unit
) {
    val hasFinePermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val hasCoarsePermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    if (!hasFinePermission && !hasCoarsePermission) return

    fusedLocationClient
        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
        .addOnSuccessListener { location ->
            location?.let {
                onMapClick(LatLng(it.latitude, it.longitude))
            }
        }
}

@Composable
private fun ExpenseMetaPickerRow(
    iconRes: Int,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE2F8E4))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                tint = SpendAntBlack,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    text = label,
                    color = Color(0xFF6D6D6D),
                    fontSize = 13.sp,
                    fontFamily = SpendAntFontFamily
                )
                Text(
                    text = value,
                    color = SpendAntBlack,
                    fontSize = 16.sp,
                    fontFamily = SpendAntFontFamily,
                    fontWeight = FontWeight.Normal
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(SpendAntBlack)
        )
    }
}

@Composable
private fun RegretExpenseRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = SpendAntGreen,
                uncheckedColor = SpendAntBlack,
                checkmarkColor = SpendAntBlack
            )
        )
        Text(
            text = "Do you regret this expense?",
            color = SpendAntBlack,
            fontFamily = SpendAntFontFamily,
            fontSize = 14.sp
        )
    }
}

@Composable
internal fun ExpenseBottomMetaBar(
    modifier: Modifier = Modifier,
    date: String,
    time: String,
    location: String,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
    onLocationClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(0.dp),
                clip = false
            )
            .background(SpendAntWhite)
            .border(width = 1.dp, color = Color(0xFFE3E3E3))
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExpenseBottomMetaItem(
            iconRes = R.drawable.ic_calendar,
            value = date,
            modifier = Modifier.weight(1f),
            onClick = onDateClick
        )
        ExpenseBottomMetaItem(
            iconRes = R.drawable.ic_clock,
            value = time,
            modifier = Modifier.weight(1f),
            onClick = onTimeClick
        )
        ExpenseBottomMetaItem(
            iconRes = R.drawable.ic_location,
            value = location,
            modifier = Modifier.weight(1f),
            onClick = onLocationClick
        )
    }
}

@Composable
private fun ExpenseBottomMetaItem(
    iconRes: Int,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = value,
            tint = SpendAntBlack,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = value,
            color = SpendAntBlack,
            fontSize = 12.sp,
            fontFamily = SpendAntFontFamily,
            maxLines = 1
        )
    }
}

@Composable
private fun InlineLoading(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            strokeWidth = 2.dp,
            color = SpendAntGreen
        )
        Text(text, fontSize = 13.sp, color = SpendAntBlack)
    }
}

@Composable
private fun TopBar(
    title: String,
    onClose: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true
) {
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
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = SpendAntFontFamily,
            color = SpendAntBlack,
            modifier = Modifier.align(Alignment.Center)
        )
        IconButton(
            onClick = onConfirm,
            enabled = confirmEnabled,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Save",
                tint = if (confirmEnabled) SpendAntBlack else Color(0xFF999999)
            )
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

@Composable
internal fun expenseEntryFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color.Transparent,
    unfocusedBorderColor = Color.Transparent,
    disabledBorderColor = Color.Transparent,
    errorBorderColor = Color.Transparent,
    focusedContainerColor = Color(0xFFE2F8E4),
    unfocusedContainerColor = Color(0xFFE2F8E4),
    disabledContainerColor = Color(0xFFE2F8E4),
    focusedTextColor = SpendAntBlack,
    unfocusedTextColor = SpendAntBlack,
    focusedPlaceholderColor = Color(0xFF6D6D6D),
    unfocusedPlaceholderColor = Color(0xFF6D6D6D),
    cursorColor = SpendAntBlack
)

@Composable
private fun searchLocationFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SpendAntGreen,
    unfocusedBorderColor = SpendAntGreen,
    focusedContainerColor = SpendAntWhite,
    unfocusedContainerColor = SpendAntWhite,
    disabledContainerColor = SpendAntWhite,
    focusedTextColor = SpendAntBlack,
    unfocusedTextColor = SpendAntBlack,
    focusedPlaceholderColor = Color(0xFF777777),
    unfocusedPlaceholderColor = Color(0xFF777777),
    cursorColor = SpendAntBlack
)

@Composable
private fun spendAntTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SpendAntGreen,
    unfocusedBorderColor = Color(0xFFE0E0E0),
    focusedTextColor = SpendAntBlack,
    unfocusedTextColor = SpendAntBlack,
    focusedPlaceholderColor = SpendAntBlack,
    unfocusedPlaceholderColor = SpendAntBlack,
    cursorColor = SpendAntBlack
)

internal fun showDatePicker(
    context: android.content.Context,
    initialDate: String,
    onDateSelected: (Long) -> Unit
) {
    val calendar = Calendar.getInstance()
    parseDateMillis(initialDate)?.let { calendar.timeInMillis = it }
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis.let(onDateSelected)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

internal fun showTimePicker(
    context: android.content.Context,
    initialTime: String,
    onTimeSelected: (Int, Int) -> Unit
) {
    val calendar = Calendar.getInstance()
    parseTime(initialTime)?.let {
        calendar.set(Calendar.HOUR_OF_DAY, it.first)
        calendar.set(Calendar.MINUTE, it.second)
    }
    TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            onTimeSelected(hourOfDay, minute)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        false
    ).show()
}

private fun parseDateMillis(dateText: String): Long? {
    return try {
        SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).parse(dateText)?.time
    } catch (_: Exception) {
        null
    }
}

private fun parseTime(timeText: String): Pair<Int, Int>? {
    return try {
        val parsed = SimpleDateFormat("hh:mma", Locale.getDefault()).parse(timeText) ?: return null
        val calendar = Calendar.getInstance().apply { time = parsed }
        calendar.get(Calendar.HOUR_OF_DAY) to calendar.get(Calendar.MINUTE)
    } catch (_: Exception) {
        null
    }
}

private fun buildVoiceAmountPreview(
    result: VoiceParseResult,
    activeCurrency: String
): String {
    val amount = result.amount?.toDoubleOrNull() ?: return "-"
    val originalCurrency = result.detectedCurrencyCode ?: activeCurrency
    val originalText = "$originalCurrency ${CurrencyProvider.formatValue(amount)}"
    if (originalCurrency == activeCurrency) return originalText

    val copAmount = if (originalCurrency == "COP") {
        amount
    } else {
        val rate = CurrencyProvider.rateFor(originalCurrency)
        if (rate == 0.0) 0.0 else kotlin.math.ceil(amount / rate)
    }
    val activeAmount = CurrencyProvider.convertToLocal(copAmount)
    return "$originalText  (≈ $activeCurrency ${CurrencyProvider.formatValue(activeAmount)})"
}

private suspend fun searchLocationByName(
    context: android.content.Context,
    query: String
): Pair<Double, Double>? {
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocationName(query, 1) { addresses ->
                    val address = addresses.firstOrNull()
                    continuation.resume(
                        if (address != null) address.latitude to address.longitude else null
                    )
                }
            }
        } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocationName(query, 1)
                ?.firstOrNull()
                ?.let { it.latitude to it.longitude }
        }
    } catch (_: Exception) {
        null
    }
}

@Composable
internal fun VoiceInputScreen(
    voiceViewModel: VoiceInputViewModel,
    onClose: () -> Unit,
    onConfirm: (VoiceParseResult) -> Unit
) {
    val context = LocalContext.current
    val uiState by voiceViewModel.uiState.collectAsState()
    val currencyState by CurrencyProvider.uiState.collectAsState()

    val micPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) voiceViewModel.startListening()
        else voiceViewModel.stopListening()
    }

    val micTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseAlpha by micTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 0.55f,
        animationSpec = infiniteRepeatable(
            animation  = tween(650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    val micAlpha = if (uiState.isListening) pulseAlpha else 1f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpendAntWhite)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                title          = "Voice Register",
                onClose        = onClose,
                onConfirm      = { onConfirm(uiState.parsedResult) },
                confirmEnabled = uiState.hasParsedResult && !uiState.isParsing
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ── Format hint card ──────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE2F8E4))
                        .padding(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text       = "Say your expense in this format:",
                            color      = SpendAntBlack,
                            fontFamily = SpendAntFontFamily,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign  = TextAlign.Center,
                            modifier   = Modifier.fillMaxWidth()
                        )
                        Text(
                            text       = "\"I paid [amount] for [product] (on [day]) (at [time]) (at [location])\"",
                            color      = SpendAntBlack,
                            fontFamily = SpendAntFontFamily,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontStyle  = FontStyle.Italic,
                            textAlign  = TextAlign.Center,
                            modifier   = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text       = "For example:",
                            color      = SpendAntBlack,
                            fontFamily = SpendAntFontFamily,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign  = TextAlign.Center,
                            modifier   = Modifier.fillMaxWidth()
                        )
                        listOf(
                            "\"I paid 5 dollars for coffee\"",
                            "\"I paid 15 euros for lunch yesterday at McDonald's\"",
                            "\"I paid 25,000 pesos for a taxi on Tuesday at 10 PM at the airport\""
                        ).forEach { example ->
                            Text(
                                text       = example,
                                color      = SpendAntBlack,
                                fontFamily = SpendAntFontFamily,
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontStyle  = FontStyle.Italic,
                                textAlign  = TextAlign.Center,
                                modifier   = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFE0E0E0))
                )

                // ── Transcript section ────────────────────────────────────
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text       = "What I heard:",
                        color      = SpendAntBlack,
                        fontFamily = SpendAntFontFamily,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign  = TextAlign.Center
                    )

                    if (uiState.isParsing) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color       = SpendAntGreen
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFEEEEEE))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = when {
                                    uiState.transcript.isEmpty() -> "\"[Tap the mic below to speak]\""
                                    else -> "\"${uiState.transcript}\""
                                },
                                color      = SpendAntBlack,
                                fontFamily = SpendAntFontFamily,
                                fontStyle  = FontStyle.Italic,
                                fontSize   = 15.sp,
                                textAlign  = TextAlign.Center,
                                modifier   = Modifier.fillMaxWidth()
                            )
                        }

                        if (uiState.hasParsedResult) {
                            val result = uiState.parsedResult
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFE2F8E4))
                                    .padding(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(
                                        "Product"  to (result.name?.replaceFirstChar { it.uppercase() } ?: "-"),
                                        "Amount"   to buildVoiceAmountPreview(result, currencyState.activeCurrency),
                                        "Location" to (result.locationName?.replaceFirstChar { it.uppercase() } ?: "-"),
                                        "Date"     to (result.date ?: "-")
                                    ).filter { (_, value) -> value != "-" }.forEach { (label, value) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text(
                                                text       = label,
                                                color      = SpendAntBlack,
                                                fontFamily = SpendAntFontFamily,
                                                fontSize   = 14.sp,
                                                fontWeight = FontWeight.Normal,
                                                modifier   = Modifier.width(72.dp)
                                            )
                                            Text(
                                                text       = value,
                                                color      = SpendAntBlack,
                                                fontFamily = SpendAntFontFamily,
                                                fontSize   = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                text       = "Looks correct? Save now",
                                color      = SpendAntGreen,
                                fontFamily = SpendAntFontFamily,
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign  = TextAlign.Center,
                                modifier   = Modifier.clickable { onConfirm(uiState.parsedResult) }
                            )
                        }
                    }

                    if (uiState.error != null) {
                        Text(
                            text       = uiState.error!!,
                            color      = androidx.compose.material3.MaterialTheme.colorScheme.error,
                            fontFamily = SpendAntFontFamily,
                            fontSize   = 13.sp,
                            textAlign  = TextAlign.Center
                        )
                    }
                }

                // ── Mic button ────────────────────────────────────────────
                // Espacio fijo: el waveform se dibuja aquí cuando está escuchando.
                // Altura constante para que el botón no se mueva.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isListening) {
                        VoiceWaveform(
                            rmsDb    = uiState.rmsDb,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(SpendAntGreen.copy(alpha = micAlpha))
                        .clickable {
                            if (uiState.isListening) {
                                voiceViewModel.stopListening()
                            } else {
                                val granted = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) voiceViewModel.startListening()
                                else micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = if (uiState.isListening) "Stop listening" else "Start listening",
                        tint     = SpendAntBlack,
                        modifier = Modifier.size(40.dp)
                    )
                }

                if (uiState.isListening) {
                    Text(
                        text       = "Listening...",
                        color      = SpendAntGreen,
                        fontFamily = SpendAntFontFamily,
                        fontSize   = 13.sp
                    )
                }
            }
        }

        if (uiState.needsConnectivity) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000))
                    .clickable(enabled = true, onClick = {}),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SpendAntWhite,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text       = "No internet connection",
                            color      = SpendAntBlack,
                            fontSize   = 16.sp,
                            fontFamily = SpendAntFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            textAlign  = TextAlign.Center
                        )
                        Text(
                            text       = "Voice recognition requires internet on this device. Connect to continue.",
                            color      = Color(0xFF777777),
                            fontSize   = 13.sp,
                            fontFamily = SpendAntFontFamily,
                            textAlign  = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceWaveform(rmsDb: Float, modifier: Modifier = Modifier) {
    val amplitude = (rmsDb / 10f).coerceIn(0f, 1f)
    val barCount = 20

    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            repeat(barCount) { i ->
                val phase      = i * (2.0 * PI / barCount).toFloat()
                val sinVal     = sin((animProgress + phase).toDouble()).toFloat()
                val normalized = (sinVal + 1f) / 2f
                val bellWeight = 1f - abs(i - barCount / 2f) / (barCount / 2f) * 0.35f
                val barHeight  = (4f + normalized * 36f * amplitude * bellWeight).dp
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(barHeight)
                        .clip(RoundedCornerShape(2.dp))
                        .background(SpendAntGreen)
                )
            }
        }
    }
}

@Composable
private fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember {
        MapView(context).apply {
            onCreate(Bundle())
        }
    }

    DisposableEffect(lifecycle, mapView) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                mapView.onStart()
            }

            override fun onResume(owner: LifecycleOwner) {
                mapView.onResume()
            }

            override fun onPause(owner: LifecycleOwner) {
                mapView.onPause()
            }

            override fun onStop(owner: LifecycleOwner) {
                mapView.onStop()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                mapView.onDestroy()
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    return mapView
}
