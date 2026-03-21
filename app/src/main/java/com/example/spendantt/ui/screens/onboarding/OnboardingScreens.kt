package com.example.spendantt.ui.screens.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.spendantt.R
import com.example.spendantt.data.service.IcsImportService
import com.example.spendantt.ui.components.BlackButton
import com.example.spendantt.ui.theme.SpendAntFontFamily
import com.example.spendantt.ui.theme.SpendAntGreenv2
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen1(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SpendAntGreenv2)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(120.dp))

            Text(
                text = "Welcome to SpendAnt!",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = SpendAntFontFamily,
                color = Color.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "We'll help you handle the small stuff so your savings can grow big",
                fontSize = 18.sp,
                fontFamily = SpendAntFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            Image(
                painter = painterResource(id = R.drawable.ant_presenting),
                contentDescription = "SpendAnt mascot",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .height(280.dp)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            BlackButton(
                text = "Continue",
                onClick = onContinue,
                width = 160.dp,
                height = 50.dp,
                cornerRadius = 25.dp,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun OnboardingScreen2(
    userId: Int,
    onImportSuccess: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val icsImportService = remember { IcsImportService(context) }
    var isImporting by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }

    val icsPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) {
            isImporting = false
            importError = "File selection was cancelled. You can retry or skip."
            return@rememberLauncherForActivityResult
        }

        coroutineScope.launch {
            val result = icsImportService.importFromUri(userId, uri)
            result.fold(
                onSuccess = { onImportSuccess() },
                onFailure = { error ->
                    isImporting = false
                    importError = error.message ?: "Unable to import the selected .ics file."
                }
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SpendAntGreenv2)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(120.dp))

            Text(
                text = "To help us spot patterns in your spending, import a calendar .ics file so SpendAnt can understand your upcoming plans and routines.",
                fontSize = 18.sp,
                fontFamily = SpendAntFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            Image(
                painter = painterResource(id = R.drawable.ant_waving),
                contentDescription = "SpendAnt mascot waving",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .height(280.dp)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            BlackButton(
                text = if (isImporting) "Importing..." else "Import Calendar (.ics)",
                onClick = {
                    if (!isImporting) {
                        importError = null
                        isImporting = true
                        icsPickerLauncher.launch(arrayOf("*/*"))
                    }
                },
                width = 240.dp,
                height = 50.dp,
                cornerRadius = 25.dp,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isImporting) {
                CircularProgressIndicator(color = Color.Black)
                Spacer(modifier = Modifier.height(16.dp))
            }

            importError?.let { error ->
                Text(
                    text = error,
                    fontSize = 14.sp,
                    fontFamily = SpendAntFontFamily,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = "Skip",
                fontSize = 16.sp,
                fontFamily = SpendAntFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                modifier = Modifier.clickable { onSkip() }
            )

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun OnboardingScreen3(
    onAllowNotifications: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onAllowNotifications()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SpendAntGreenv2)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(120.dp))

            Text(
                text = "Want instant insights? Allow notification access for Google Pay, and SpendAnt will categorize your spending the second it happens.",
                fontSize = 18.sp,
                fontFamily = SpendAntFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            Image(
                painter = painterResource(id = R.drawable.ant_idle),
                contentDescription = "SpendAnt mascot idle",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .height(280.dp)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            BlackButton(
                text = "Allow Notification Access",
                onClick = {
                    if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onAllowNotifications()
                    }
                },
                width = 260.dp,
                height = 50.dp,
                cornerRadius = 25.dp,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Skip",
                fontSize = 16.sp,
                fontFamily = SpendAntFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                modifier = Modifier.clickable { onSkip() }
            )

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun OnboardingScreen4(
    onAllowLocation: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onAllowLocation()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SpendAntGreenv2)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(120.dp))

            Text(
                text = "Want smarter suggestions? Allow location access so SpendAnt can detect nearby places, organize your spending better, and make each record faster and more accurate.",
                fontSize = 18.sp,
                fontFamily = SpendAntFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            Image(
                painter = painterResource(id = R.drawable.ant_standing),
                contentDescription = "SpendAnt mascot standing",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .height(280.dp)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            BlackButton(
                text = "Allow Location Access",
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
                        onAllowLocation()
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                width = 260.dp,
                height = 50.dp,
                cornerRadius = 25.dp,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Skip",
                fontSize = 16.sp,
                fontFamily = SpendAntFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                modifier = Modifier.clickable { onSkip() }
            )

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}
