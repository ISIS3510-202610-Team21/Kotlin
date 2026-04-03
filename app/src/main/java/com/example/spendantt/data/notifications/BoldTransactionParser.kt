package com.example.spendantt.data.notifications

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Datos extraídos de una transacción Bold
 */
data class ParsedBoldTransaction(
    val merchantName: String,
    val amount: Double,
    val date: Long,
    val time: String,
    val location: String?,
    val transactionId: String?,
    val paymentMethod: String?,
    val cardLastDigits: String?
)

/**
 * Parser para extraer información de emails de transacciones Bold
 * 
 * Diseñado para ser robusto y manejar múltiples formatos de Gmail
 */
object BoldTransactionParser {
    
    // Regex más flexible para extraer monto (busca números con punto o coma)
    private val amountRegex = Regex("""(?:COP\s*)?\$?\s*([\d.,]+)""", RegexOption.IGNORE_CASE)
    
    // Regex para fecha y hora (formato flexible)
    private val dateTimeRegex = Regex("""(\d{4}[/-]\d{2}[/-]\d{2})\s+(\d{2}:\d{2}:\d{2})""")
    
    // Regex para ID de transacción Bold (más flexible)
    private val transactionIdRegex = Regex("""(?:ID\s+Transacci[oó]n\s+Bold|CPRY)[:\s]*([A-Z0-9]+)""", RegexOption.IGNORE_CASE)
    
    // Formato de fecha de Bold
    private val boldDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
    private val boldDateFormatDash = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mma", Locale.getDefault())
    
    /**
     * Parsea el contenido de un email de transacción Bold
     * Ahora más robusto: solo necesita "compra" + monto + nombre de comercio
     */
    fun parse(emailContent: String): ParsedBoldTransaction? {
        // Limpiar el contenido de los >> que Gmail agrega
        val cleanedContent = emailContent.lines()
            .map { it.trim().removePrefix(">>").trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
        
        val lines = cleanedContent.lines()
        
        // Validación mínima: debe contener "compra" (más flexible)
        val hasCompra = cleanedContent.contains("compra", ignoreCase = true)
        if (!hasCompra) {
            return null
        }
        
        // Extraer monto (obligatorio)
        val amount = extractAmount(cleanedContent, lines) ?: return null
        
        // Extraer nombre del comercio (obligatorio)
        val merchantName = extractMerchantName(cleanedContent, lines, amount) ?: return null
        
        // Extraer fecha y hora (opcional, usa timestamp actual si no encuentra)
        val (dateMillis, timeText) = extractDateTime(cleanedContent) ?: Pair(
            System.currentTimeMillis(),
            timeFormat.format(System.currentTimeMillis())
        )
        
        // Extraer ubicación (opcional)
        val location = extractLocation(lines)
        
        // Extraer ID de transacción (opcional)
        val transactionId = transactionIdRegex.find(cleanedContent)?.groupValues?.getOrNull(1)
        
        return ParsedBoldTransaction(
            merchantName = merchantName,
            amount = amount,
            date = dateMillis,
            time = timeText,
            location = location,
            transactionId = transactionId,
            paymentMethod = "Bold",
            cardLastDigits = null
        )
    }
    
    /**
     * Extrae el monto de la transacción de manera más flexible
     */
    private fun extractAmount(text: String, lines: List<String>): Double? {
        // Buscar primero en líneas que contengan "COP" o "$"
        val amountLines = lines.filter { 
            (it.contains("COP", ignoreCase = true) || it.contains("$")) &&
            it.matches(Regex(""".*[\d.,]+.*"""))
        }
        
        for (line in amountLines) {
            val match = amountRegex.find(line)
            if (match != null) {
                val rawAmount = match.groupValues.getOrNull(1) ?: continue
                val normalized = normalizeAmount(rawAmount)
                val parsed = normalized.toDoubleOrNull()
                if (parsed != null && parsed > 0) {
                    return parsed
                }
            }
        }
        
        // Si no encuentra, buscar en todo el texto
        val matches = amountRegex.findAll(text)
        for (match in matches) {
            val rawAmount = match.groupValues.getOrNull(1) ?: continue
            val normalized = normalizeAmount(rawAmount)
            val parsed = normalized.toDoubleOrNull()
            // Validar que sea un monto razonable (entre 100 y 100,000,000)
            if (parsed != null && parsed >= 100 && parsed <= 100_000_000) {
                return parsed
            }
        }
        
        return null
    }
    
    /**
     * Normaliza un monto removiendo separadores de miles y convirtiendo decimales
     */
    private fun normalizeAmount(rawAmount: String): String {
        val cleaned = rawAmount.trim()
        
        // Si tiene punto y coma, asumimos formato colombiano: 1.000,50 -> 1000.50
        return if (cleaned.contains(",") && cleaned.contains(".")) {
            cleaned.replace(".", "").replace(",", ".")
        } else if (cleaned.contains(".") && cleaned.count { it == '.' } > 1) {
            // Múltiples puntos = separador de miles: 1.000.000 -> 1000000
            cleaned.replace(".", "")
        } else if (cleaned.contains(",") && cleaned.count { it == ',' } > 1) {
            // Múltiples comas = separador de miles: 1,000,000 -> 1000000
            cleaned.replace(",", "")
        } else {
            // Solo puntos o solo comas, remover separador de miles
            cleaned.replace(".", "").replace(",", ".")
        }
    }
    
    /**
     * Extrae el nombre del comercio de manera más inteligente
     */
    private fun extractMerchantName(text: String, lines: List<String>, amount: Double): String? {
        // Estrategia 1: Buscar "Compra en [NOMBRE]"
        val compraEnRegex = Regex("""[Cc]ompra\s+en\s+([^p][^\n\r>>]+?)(?:\s+por|\s+COP|\s+\$|$)""")
        val compraMatch = compraEnRegex.find(text)
        if (compraMatch != null) {
            val name = compraMatch.groupValues.getOrNull(1)?.trim()
            if (!name.isNullOrBlank() && name.length > 2) {
                return name
            }
        }
        
        // Estrategia 2: Buscar la línea después de una línea con el monto
        val amountStr = amount.toInt().toString()
        for (i in lines.indices) {
            val line = lines[i]
            if (line.contains(amountStr) || line.contains("COP") || line.contains("Transacci")) {
                // El nombre suele estar 1-2 líneas después
                for (offset in 1..2) {
                    if (i + offset < lines.size) {
                        val candidate = lines[i + offset]
                        if (isValidMerchantName(candidate)) {
                            return candidate.trim()
                        }
                    }
                }
            }
        }
        
        // Estrategia 3: Buscar líneas que parezcan nombres (longitud razonable, no tienen números)
        for (line in lines) {
            if (isValidMerchantName(line)) {
                return line.trim()
            }
        }
        
        return null
    }
    
    /**
     * Valida si una línea parece un nombre de comercio
     */
    private fun isValidMerchantName(line: String): Boolean {
        val trimmed = line.trim()
        
        // Debe tener longitud razonable
        if (trimmed.length < 3 || trimmed.length > 60) return false
        
        // No debe ser una línea con campos técnicos
        val invalidKeywords = listOf(
            "cop", "pagos", "transacci", "terminal", "mid", "aid",
            "metodo", "medio", "cuota", "cuenta", "subtotal", "propina",
            "total", "codigo", "autorizaci", "app label", "pago sin contacto",
            "@", ".com", "http"
        )
        if (invalidKeywords.any { trimmed.contains(it, ignoreCase = true) }) return false
        
        // No debe ser solo números o símbolos
        if (trimmed.all { it.isDigit() || !it.isLetterOrDigit() }) return false
        
        // Debe contener al menos algunas letras
        if (trimmed.count { it.isLetter() } < 3) return false
        
        return true
    }
    
    /**
     * Extrae fecha y hora de manera más flexible
     */
    private fun extractDateTime(text: String): Pair<Long, String>? {
        val match = dateTimeRegex.find(text) ?: return null
        val dateStr = match.groupValues.getOrNull(1) ?: return null
        val timeStr = match.groupValues.getOrNull(2) ?: return null
        
        return try {
            // Intentar con formato con slash
            val fullDateTime = "${dateStr.replace("-", "/")} $timeStr"
            val date = boldDateFormat.parse(fullDateTime)
            if (date != null) {
                val formattedTime = timeFormat.format(date)
                return Pair(date.time, formattedTime)
            }
            
            // Intentar con formato con dash
            val fullDateTimeDash = "$dateStr $timeStr"
            val dateDash = boldDateFormatDash.parse(fullDateTimeDash)
            if (dateDash != null) {
                val formattedTime = timeFormat.format(dateDash)
                return Pair(dateDash.time, formattedTime)
            }
            
            null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extrae la ubicación (dirección) de la transacción
     */
    private fun extractLocation(lines: List<String>): String? {
        for (line in lines) {
            val trimmed = line.trim()
            // Buscar líneas que parezcan direcciones
            if (trimmed.contains(Regex("""[Cc]alle|[Cc]arrera|[Aa]v\.|[Kk]r|#\s*\d|,\s*Bogot"""))) {
                return trimmed
            }
        }
        return null
    }
}
