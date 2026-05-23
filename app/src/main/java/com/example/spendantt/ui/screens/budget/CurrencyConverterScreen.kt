package com.example.spendantt.ui.screens.budget

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spendantt.data.currency.CurrencyProvider
import com.example.spendantt.ui.theme.SpendAntFontFamily
import kotlin.math.roundToInt

private val selectedAccent = Color(0xFF297DE7)
private val currencyColors = listOf(
    Color(0xFF78E4B0),
    Color(0xFFB87DE9),
    Color(0xFF4AD3F5),
    Color(0xFFBDDD34),
    Color(0xFF9A1737),
    Color(0xFFFF886E),
    Color(0xFFFCC34D),
    Color(0xFFA1BF9D),
    Color(0xFF5B204E),
    Color(0xFFD1A039)
)

private val currencyCategory = mapOf(
    "COP" to "Local Currency",
    "USD" to "Global Reserve",
    "EUR" to "Global Reserve",
    "GBP" to "Global Reserve",
    "JPY" to "Global Reserve",
    "CHF" to "Global Reserve",
    "CAD" to "Strong Economy",
    "AUD" to "Strong Economy",
    "MXN" to "Latin American",
    "BRL" to "Latin American",
    "CLP" to "Latin American",
    "PEN" to "Latin American",
    "ARS" to "Latin American",
    "CNY" to "Global Economy"
)

private val supportedCurrencies = listOf(
    "COP",
    "USD",
    "EUR",
    "GBP",
    "JPY",
    "CAD",
    "AUD",
    "MXN",
    "BRL",
    "CLP",
    "PEN",
    "ARS",
    "CHF",
    "CNY"
)

private val currencyDisplayName = mapOf(
    "COP" to "Colombian Peso",
    "USD" to "US Dollar",
    "EUR" to "Euro",
    "GBP" to "British Pound",
    "JPY" to "Japanese Yen",
    "CAD" to "Canadian Dollar",
    "AUD" to "Australian Dollar",
    "MXN" to "Mexican Peso",
    "BRL" to "Brazilian Real",
    "CLP" to "Chilean Peso",
    "PEN" to "Peruvian Sol",
    "ARS" to "Argentine Peso",
    "CHF" to "Swiss Franc",
    "CNY" to "Chinese Yuan"
)

private val largeNumberCurrencies = setOf("COP", "ARS", "CLP", "JPY")

@Composable
fun CurrencyConverterScreen(
    onClose: () -> Unit,
    onConfirm: (iso: String, rate: Double) -> Unit
) {
    val currencyState by CurrencyProvider.uiState.collectAsState()
    var selectedIso by remember { mutableStateOf(currencyState.activeCurrency) }

    LaunchedEffect(currencyState.activeCurrency) {
        selectedIso = currencyState.activeCurrency
    }

    val filteredRates = remember(currencyState.ratesCache) {
        buildMap {
            supportedCurrencies.forEach { iso ->
                put(iso, currencyState.ratesCache[iso] ?: if (iso == "COP") 1.0 else 1.0)
            }
        }
    }
    val selectedRate = filteredRates[selectedIso] ?: 1.0
    val listItems = remember(filteredRates, selectedIso) {
        supportedCurrencies.filter { it != selectedIso && filteredRates.containsKey(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF44C669))
                .padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Black)
            }
            Text(
                text = "Currency Converter",
                color = Color.Black,
                fontFamily = SpendAntFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
            IconButton(
                onClick = {
                    if (currencyState.ratesCache.size > 1) {
                        onConfirm(selectedIso, selectedRate)
                        onClose()
                    }
                },
                enabled = currencyState.ratesCache.size > 1,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Save",
                    tint = if (currencyState.ratesCache.size > 1) Color.Black else Color(0xFF777777)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                AnimatedContent(selectedIso, modifier = Modifier.animateContentSize(), label = "currency_top_card") { iso ->
                    CurrencyCard(
                        iso = iso,
                        accent = selectedAccent,
                        isSelected = true,
                        onClick = {},
                        equivalence = "1 $iso = 1 $iso"
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color(0xFFD0D0D0))
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            itemsIndexed(listItems) { index, iso ->
                val accent = currencyColors[index % currencyColors.size]
                CurrencyCard(
                    iso = iso,
                    accent = accent,
                    isSelected = false,
                    onClick = { selectedIso = iso },
                    equivalence = buildEquivalenceLabel(selectedIso, iso, filteredRates)
                )
            }
        }
    }
}

@Composable
private fun CurrencyCard(
    iso: String,
    accent: Color,
    isSelected: Boolean,
    equivalence: String,
    onClick: () -> Unit
) {
    val background = if (isSelected) {
        lerp(selectedAccent, Color.White, 0.82f)
    } else {
        lerp(accent, Color.White, 0.76f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isSelected) 6.dp else 5.dp,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
                clip = false
            )
            .background(background)
            .then(
                if (isSelected) Modifier.border(1.dp, selectedAccent.copy(alpha = 0.35f))
                else Modifier.border(0.dp, Color.Transparent)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(if (isSelected) selectedAccent else accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = iso, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = currencyDisplayName[iso] ?: iso,
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
                Text(
                    text = if (isSelected) "Current Currency" else (currencyCategory[iso] ?: "Global Economy"),
                    color = if (isSelected) selectedAccent else Color(0xFF5E5E5E),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = equivalence,
                color = Color.Gray,
                fontSize = if (isSelected) 14.sp else 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun buildEquivalenceLabel(
    selectedIso: String,
    listIso: String,
    rates: Map<String, Double>
): String {
    val rateSel = rates[selectedIso] ?: 1.0
    val rateList = rates[listIso] ?: 1.0
    if (rateSel == 0.0 || rateList == 0.0) return "1 $selectedIso = 1 $listIso"

    val selectedFor1List = rateSel / rateList
    val listFor1Selected = rateList / rateSel

    return when {
        selectedFor1List >= 1000 -> {
            "${formatEquivalenceValue(selectedFor1List)} $selectedIso = 1 $listIso"
        }
        listFor1Selected >= 1 -> {
            var leftValue = 1.0
            var rightValue = listFor1Selected
            if (listIso in largeNumberCurrencies && rightValue < 1000) {
                while (rightValue < 1000) {
                    leftValue *= 10
                    rightValue *= 10
                }
            }
            "${formatEquivalenceValue(leftValue)} $selectedIso = ${formatEquivalenceValue(rightValue)} $listIso"
        }
        else -> {
            var leftValue = 1000.0
            var rightValue = listFor1Selected * 1000.0
            while (rightValue < 1.0) {
                leftValue *= 10
                rightValue *= 10
            }
            "${formatEquivalenceValue(leftValue)} $selectedIso = ${formatEquivalenceValue(rightValue)} $listIso"
        }
    }
}

private fun formatEquivalenceValue(value: Double): String {
    return if (value >= 10) {
        CurrencyProvider.formatValue(value.roundToInt().toDouble())
    } else {
        CurrencyProvider.formatValue(value)
    }
}
