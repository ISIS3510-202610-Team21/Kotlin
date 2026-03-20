package com.example.spendantt.data.notifications

data class ParsedPaymentNotification(
    val merchantName: String,
    val amount: Double
)

object PaymentNotificationParser {
    private val amountRegex = Regex("""\$?\s*([\d.]+(?:,\d{2})?)""")
    private val merchantRegex = Regex("""compra en\s+(.+?)\s+por\s+\$""", RegexOption.IGNORE_CASE)

    fun parse(title: String, body: String): ParsedPaymentNotification? {
        val merchantName = extractMerchant(body) ?: extractMerchant(title) ?: return null
        val amount = extractAmount(body) ?: extractAmount(title) ?: return null
        return ParsedPaymentNotification(
            merchantName = merchantName.trim(),
            amount = amount
        )
    }

    private fun extractMerchant(text: String): String? {
        return merchantRegex.find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun extractAmount(text: String): Double? {
        val rawAmount = amountRegex.find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?: return null

        val normalized = rawAmount
            .replace(".", "")
            .replace(",", ".")

        return normalized.toDoubleOrNull()
    }
}
