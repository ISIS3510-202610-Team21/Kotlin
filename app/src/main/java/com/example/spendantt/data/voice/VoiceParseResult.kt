package com.example.spendantt.data.voice

import com.example.spendantt.data.ocr.OcrProcessor

/**
 * Result of natural-language voice parsing.
 *
 * [detectedCurrencyCode] is intentionally separate from [amount] so the future
 * exchange-rate feature can convert before populating the form — just check
 * detectedCurrencyCode != null and apply the rate in NewExpenseViewModel.autoFillFromVoice.
 */
data class VoiceParseResult(
    val name: String? = null,
    val amount: String? = null,
    val detectedCurrencyCode: String? = null,
    val date: String? = null,
    val time: String? = null,
    val locationName: String? = null
) {
    fun toOcrResult() = OcrProcessor.OcrResult(
        name = name,
        amount = amount,
        date = date,
        time = time,
        locationName = locationName
    )
}
