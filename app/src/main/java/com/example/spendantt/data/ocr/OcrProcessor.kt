package com.example.spendantt.data.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * OcrProcessor
 *
 * Procesa una imagen de recibo con ML Kit y extrae nombre, monto, fecha, hora y ubicación.
 *
 * Mejoras sobre la versión anterior (portadas desde la implementación Flutter del equipo):
 * - Sistema de scoring para seleccionar el monto correcto entre múltiples candidatos
 * - Corrección de errores típicos de OCR en números (O→0, l→1, S→5, I→1)
 * - Parsing inteligente de separadores decimales/miles (COP usa puntos como miles)
 * - Keywords priorizados: total > subtotal > tax > payment para descartar falsos positivos
 * - Detección de identificadores (NIT, facturas, códigos) para no confundirlos con montos
 * - Detección de marcadores de moneda (COP, COL$, USD)
 * - Extracción de direcciones colombianas (Carrera, Calle, Av, etc.) y etiquetadas
 * - Normalización de tildes y caracteres especiales en keywords
 */
class OcrProcessor(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    data class OcrResult(
        val name: String? = null,
        val amount: String? = null,
        val date: String? = null,
        val time: String? = null,
        val locationName: String? = null
    )

    suspend fun processReceipt(uri: Uri): OcrResult {
        val image = InputImage.fromFilePath(context, uri)
        val text = recognizeText(image)
        android.util.Log.d("OCR_TEXT", text)
        return parseReceiptText(text)
    }

    private suspend fun recognizeText(image: InputImage): String {
        return suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { result -> continuation.resume(result.text) }
                .addOnFailureListener { e -> continuation.resumeWithException(e) }
        }
    }

    private fun parseReceiptText(text: String): OcrResult {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        return OcrResult(
            name = extractName(lines),
            amount = extractAmount(lines),
            date = extractDate(lines),
            time = extractTime(lines),
            locationName = extractLocation(lines)
        )
    }

    // ── KEYWORDS ──────────────────────────────────────────────────────────────────
    // Ordenados por prioridad: el índice más bajo = mayor score al hacer match
    companion object {
        private val TOTAL_KEYWORDS = listOf(
            "total exacto", "total cop", "total a pagar", "total pagar",
            "valor total", "grand total", "amount due", "tctal", "tctal:",
            "amount", "total:", "total"
        )
        private val PAYMENT_KEYWORDS = listOf(
            "pago", "pagado", "paga con", "efectivo", "cash", "recibido",
            "recibe", "cambio", "vuelto", "change", "credito", "debito",
            "tarjeta", "visa", "mastercard"
        )
        private val DUE_KEYWORDS = listOf(
            "por cobrar", "saldo", "monto a pagar", "a pagar", "pendiente"
        )
        private val SUBTOTAL_KEYWORDS = listOf("subtotal", "sub total", "base")
        private val TAX_KEYWORDS = listOf("iva", "impuesto", "tax", "rete", "retencion", "tasa")
        private val COUNT_KEYWORDS = listOf(
            "item", "items", "caja", "cajas", "cantidad", "cant",
            "articulo", "articulos", "unidad", "unidades", "producto", "productos"
        )
        private val UNIT_KEYWORDS = listOf("/pc", "/und", "/un", "/u", "/kg", "/g", "/lt", "/ml", "@")
        private val ADDRESS_LABELS = listOf(
            "direccion", "dirección", "dir", "direc", "ubicacion", "ubicación"
        )
        private val IDENTIFIER_KEYWORDS = listOf(
            "nit", "factura", "invoice", "ticket", "tiquete", "ref", "referencia",
            "codigo", "autorizacion", "aprobacion", "transaccion", "trans",
            "pedido", "orden", "documento", "cantidad", "cuota", "cuotas",
            "correlativo", "serie", "doc", "id", "terminal", "lote"
        )
        private val NAME_BLOCKED = setOf(
            "factura", "invoice", "recibo", "receipt", "nit", "cash", "sale",
            "venta", "fecha", "date", "hora", "time", "total", "subtotal", "tax", "iva"
        )
        private val CURRENCY_REGEX = Regex("(?:cop|col\\\$|usd|eur|\\\$)", RegexOption.IGNORE_CASE)
        // Detecta números con posibles errores OCR y marcadores de moneda
        private val AMOUNT_REGEX = Regex(
            "(?:(?:cop|col\\\$|usd|eur)\\s*)?-?\\s*[\\\$]?\\s*\\d[\\dOoIlSs.,|]*(?:\\s*(?:cop|col\\\$|usd|eur))?",
            RegexOption.IGNORE_CASE
        )
        private val COLOMBIAN_ADDRESS_REGEX = Regex(
            "\\b(?:carrera|cra|cr|calle|cl|avenida|av|diagonal|diag|transversal|tv)\\b",
            RegexOption.IGNORE_CASE
        )
    }

    // ── NORMALIZACIÓN DE TEXTO ─────────────────────────────────────────────────
    // Elimina tildes y convierte a minúsculas para comparar keywords
    private fun normalizeKeywordLine(line: String): String = line.lowercase()
        .replace('á', 'a').replace('é', 'e').replace('í', 'i')
        .replace('ó', 'o').replace('ú', 'u').replace('ñ', 'n')
        .replace('Á', 'a').replace('É', 'e').replace('Í', 'i')
        .replace('Ó', 'o').replace('Ú', 'u')

    // ── NOMBRE ────────────────────────────────────────────────────────────────
    // Las primeras 6 líneas suelen tener el nombre. Se filtran líneas con
    // números largos o keywords genéricos de recibo.
    private fun extractName(lines: List<String>): String? {
        for (line in lines.take(6)) {
            val normalized = line.lowercase()
            if (line.length < 3 || !line.any { it.isLetter() }) continue
            if (Regex("\\d{4,}").containsMatchIn(line)) continue
            if (NAME_BLOCKED.any { normalized.contains(it) }) continue
            return line.trim()
        }
        return null
    }

    // ── MONTO ─────────────────────────────────────────────────────────────────
    // Extrae todos los candidatos numéricos del recibo, los puntúa según
    // contexto (keywords cercanos, posición, señales monetarias) y elige el mejor.
    private fun extractAmount(lines: List<String>): String? {
        val candidates = mutableListOf<AmountCandidate>()

        for (index in lines.indices) {
            val prevLabelScore = if (index > 0)
                standaloneTotalLabelScore(normalizeKeywordLine(lines[index - 1]))
            else 0
            candidates.addAll(extractAmountCandidates(lines[index], index, prevLabelScore))
        }

        if (candidates.isEmpty()) return null

        // Preferir candidatos con señal monetaria (separador, COP, $, en línea de total)
        val withMoneySignal = candidates.filter { it.hasMoneySignal }
        val pool = if (withMoneySignal.isNotEmpty()) withMoneySignal else candidates
        val selected = pool.maxWithOrNull(compareBy({ it.score }, { it.value })) ?: return null
        if (selected.score < 0) return null

        return formatAmount(selected.value)
    }

    private fun extractAmountCandidates(
        line: String,
        lineIndex: Int,
        previousTotalLabelScore: Int
    ): List<AmountCandidate> {
        val normalizedLine = normalizeKeywordLine(line)
        val candidates = mutableListOf<AmountCandidate>()

        AMOUNT_REGEX.findAll(line).forEach { match ->
            val rawCandidate = match.value
            val value = parseAmount(rawCandidate) ?: return@forEach

            val normalizedRaw = rawCandidate.lowercase()
            val prefix = normalizedLine.substring(0, match.range.first.coerceAtMost(normalizedLine.length))
            val suffix = normalizedLine.substring(match.range.last.plus(1).coerceAtMost(normalizedLine.length))

            val totalScore = keywordScoreBefore(prefix, TOTAL_KEYWORDS)
            val dueScore = keywordScoreBefore(prefix, DUE_KEYWORDS)
            val effectiveTotalScore = maxOf(totalScore, dueScore)
            val nearPayment = hasKeywordBefore(prefix, PAYMENT_KEYWORDS)
            val nearSubtotal = hasKeywordBefore(prefix, SUBTOTAL_KEYWORDS)
            val nearTax = hasKeywordBefore(prefix, TAX_KEYWORDS)
            val nearCount = hasKeywordBefore(prefix, COUNT_KEYWORDS)
            val nearUnit = hasNearbyUnitKeyword(prefix, suffix)
            val onTotalLine = effectiveTotalScore > 0 && !nearTax && !nearCount
            val afterTotalLine = !onTotalLine && previousTotalLabelScore > 0 && !nearTax && !nearCount
            val distanceToTotal = distanceToKeywordBefore(
                prefix,
                if (totalScore > 0) TOTAL_KEYWORDS else DUE_KEYWORDS
            )
            val isNegative = rawCandidate.contains('-')
            val digitCount = amountDigitCount(rawCandidate)
            val hasSeparator = rawCandidate.contains(Regex("[.,]"))
            val hasDollarSign = normalizedRaw.contains('$')
            val hasCurrencyMarker = CURRENCY_REGEX.containsMatchIn(normalizedRaw)
            val looksLikeId = looksLikeIdentifier(rawCandidate, normalizedLine)

            val score = scoreCandidate(
                value = value,
                digitCount = digitCount,
                hasSeparator = hasSeparator,
                normalizedLine = normalizedLine,
                hasDollarSign = hasDollarSign,
                hasCurrencyMarker = hasCurrencyMarker,
                nearPayment = nearPayment,
                nearSubtotal = nearSubtotal,
                nearTax = nearTax,
                nearCount = nearCount,
                nearUnit = nearUnit,
                looksLikeId = looksLikeId,
                onTotalLine = onTotalLine,
                afterTotalLine = afterTotalLine,
                totalKeywordScore = effectiveTotalScore.coerceAtLeast(previousTotalLabelScore),
                distanceToTotal = distanceToTotal,
                isNegative = isNegative
            )

            candidates.add(AmountCandidate(
                value = value,
                lineIndex = lineIndex,
                score = score,
                hasSeparator = hasSeparator,
                hasCurrencyMarker = hasCurrencyMarker,
                onTotalLine = onTotalLine,
                afterTotalLine = afterTotalLine
            ))
        }

        return candidates
    }

    // ── SCORING DE CANDIDATOS ─────────────────────────────────────────────────
    // Replica el sistema de puntuación de Flutter. Bonos por contexto de total,
    // señales monetarias y magnitud. Penalizaciones por subtotal, IVA, conteos,
    // pagos, precios unitarios e identificadores.
    private fun scoreCandidate(
        value: Int, digitCount: Int, hasSeparator: Boolean, normalizedLine: String,
        hasDollarSign: Boolean, hasCurrencyMarker: Boolean,
        nearPayment: Boolean, nearSubtotal: Boolean, nearTax: Boolean,
        nearCount: Boolean, nearUnit: Boolean, looksLikeId: Boolean,
        onTotalLine: Boolean, afterTotalLine: Boolean,
        totalKeywordScore: Int, distanceToTotal: Int?, isNegative: Boolean
    ): Int {
        var score = 0

        // Bonos por posición respecto al keyword "total"
        if (onTotalLine) score += 120 + (totalKeywordScore * 4)
        else if (afterTotalLine) score += 85 + (totalKeywordScore * 3)

        // Bono por proximidad al keyword (cuanto más cerca, más bono)
        if (distanceToTotal != null) {
            val proximityBonus = 40 - distanceToTotal.coerceAtMost(40)
            if (proximityBonus > 0) score += proximityBonus
        }

        // Bono extra por frases de total muy específicas
        if (normalizedLine.startsWith("total") || normalizedLine.contains("total \$") ||
            normalizedLine.contains("total cop") || normalizedLine.contains("por cobrar") ||
            normalizedLine.contains("total exacto")) score += 25

        // Señales monetarias
        if (hasDollarSign) score += 12 else if (hasCurrencyMarker) score += 8
        if (hasSeparator) score += 10

        // Magnitud: valores grandes son más probablemente totales (en COP)
        if (value >= 1000) score += minOf(18, value.toString().length * 2)
        else if (value < 100 && !hasCurrencyMarker) score -= 40

        // Penalizaciones duras
        if (value <= 0) score -= 220
        if (digitCount <= 2) score -= 220
        if (!hasSeparator && !hasCurrencyMarker && !onTotalLine && !afterTotalLine && digitCount >= 5)
            score -= 220  // número largo sin formato monetario = probablemente un ID

        // Penalizaciones por contexto incorrecto
        if (nearSubtotal) score -= 120
        if (nearTax) score -= 120
        if (nearCount) score -= 220
        if (nearPayment) score -= 130
        if (nearUnit) score -= 120
        if (looksLikeId) {
            if (!hasSeparator && !hasCurrencyMarker && !onTotalLine && !afterTotalLine)
                score -= 150
            else score -= 20
        }
        if (isNegative) score -= 70

        return score
    }

    // ── CORRECCIÓN DE OCR + PARSING DE MONTO ──────────────────────────────────
    // OCR confunde O con 0, l/I/| con 1, S/s con 5 en contextos numéricos.
    // Normaliza el token antes de parsear.
    private fun normalizeOcrToken(raw: String): String {
        val sb = StringBuilder()
        for (i in raw.indices) {
            val c = raw[i]
            val prev = if (i > 0) raw[i - 1] else ' '
            val next = if (i + 1 < raw.length) raw[i + 1] else ' '

            if (c.isDigit() || c == ',' || c == '.' || c == '-') {
                sb.append(c)
                continue
            }
            val hasNumericNeighbor = looksLikeAmountNeighbor(prev) || looksLikeAmountNeighbor(next)
            if (!hasNumericNeighbor) continue
            when (c) {
                'o', 'O' -> sb.append('0')
                'l', 'I', '|' -> sb.append('1')
                's', 'S' -> sb.append('5')
            }
        }
        return sb.toString()
    }

    private fun looksLikeAmountNeighbor(c: Char): Boolean =
        c.isDigit() || c in listOf(',', '.', '-', 'o', 'O', 'l', 'I', 's', 'S', '|')

    // Detecta si el separador de miles/decimales es punto o coma según posición
    // y cantidad de dígitos después del último separador.
    private fun parseAmount(raw: String): Int? {
        var cleaned = normalizeOcrToken(raw)
        if (cleaned.isEmpty()) return null

        val lastDot = cleaned.lastIndexOf('.')
        val lastComma = cleaned.lastIndexOf(',')

        if (lastDot != -1 && lastComma != -1) {
            // Ambos presentes: el más a la derecha es el decimal
            val decimalSep = if (lastDot > lastComma) '.' else ','
            val thousandsSep = if (decimalSep == '.') ',' else '.'
            val decimalIdx = if (decimalSep == '.') lastDot else lastComma
            val decimalDigits = cleaned.length - decimalIdx - 1
            cleaned = cleaned.replace(thousandsSep.toString(), "")
            cleaned = if (decimalDigits == 1 || decimalDigits == 2)
                cleaned.replaceFirst(decimalSep.toString(), ".")
            else
                cleaned.replace(decimalSep.toString(), "")
            return cleaned.toDoubleOrNull()?.let { Math.round(it).toInt() }
        }

        val lastSep = maxOf(lastDot, lastComma)
        if (lastSep != -1) {
            val decimalDigits = cleaned.length - lastSep - 1
            val sep = cleaned[lastSep]
            cleaned = when {
                // 1-2 dígitos después = decimal (ej: 25.000,37 → centavos)
                decimalDigits == 1 || decimalDigits == 2 ->
                    cleaned.replace(if (sep == '.') "," else ".", "").replaceFirst(sep.toString(), ".")
                // 3 dígitos después = separador de miles (ej: 25.000 en COP)
                else -> cleaned.replace(Regex("[,.]"), "")
            }
        }

        return cleaned.toDoubleOrNull()?.let { Math.round(it).toInt() }
    }

    private fun amountDigitCount(raw: String): Int = normalizeOcrToken(raw).count { it.isDigit() }

    // Formatea el monto como entero con separadores de miles (estilo COP)
    private fun formatAmount(value: Int): String = "%,d".format(value).replace(',', '.')

    // ── KEYWORD HELPERS ───────────────────────────────────────────────────────
    private fun lineContainsKeyword(line: String, keyword: String): Boolean {
        val escaped = Regex.escape(keyword)
        return Regex("(?<![a-z0-9])$escaped(?![a-z0-9])", RegexOption.IGNORE_CASE).containsMatchIn(line)
    }

    private fun keywordScoreBefore(prefix: String, keywords: List<String>, window: Int = 28): Int {
        val context = prefix.takeLast(window).trimEnd()
        for (i in keywords.indices) {
            if (lineContainsKeyword(context, keywords[i])) return keywords.size - i
        }
        return 0
    }

    private fun hasKeywordBefore(prefix: String, keywords: List<String>, window: Int = 28): Boolean =
        keywordScoreBefore(prefix, keywords, window) > 0

    private fun distanceToKeywordBefore(prefix: String, keywords: List<String>, window: Int = 28): Int? {
        val context = prefix.takeLast(window)
        var best: Int? = null
        for (keyword in keywords) {
            var start = 0
            while (start < context.length) {
                val idx = context.indexOf(keyword, start)
                if (idx == -1) break
                val distance = context.length - (idx + keyword.length)
                if (best == null || distance < best) best = distance
                start = idx + keyword.length
            }
        }
        return best
    }

    private fun hasNearbyUnitKeyword(before: String, after: String): Boolean {
        val context = before.takeLast(16) + after.take(16)
        if (UNIT_KEYWORDS.any { context.contains(it) }) return true
        return Regex(
            "(?:\\b\\d+\\s*(?:pc|pcs|und|un|u)\\b|\\bx\\s*\\d+\\b|\\b\\d+\\s*x\\b)",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(context)
    }

    // Si una línea es SOLO un label de total (sin número), el monto puede estar en la siguiente
    private fun standaloneTotalLabelScore(normalizedLine: String): Int {
        val trimmed = normalizedLine.trim()
        if (trimmed.isEmpty() || trimmed.any { it.isDigit() }) return 0
        if (COUNT_KEYWORDS.any { lineContainsKeyword(trimmed, it) } ||
            TAX_KEYWORDS.any { lineContainsKeyword(trimmed, it) }) return 0
        val totalScore = TOTAL_KEYWORDS.indexOfFirst { lineContainsKeyword(trimmed, it) }
            .let { if (it == -1) 0 else TOTAL_KEYWORDS.size - it }
        if (totalScore > 0) return totalScore
        return keywordScoreBefore(trimmed, DUE_KEYWORDS, 48)
    }

    // Detecta si un candidato numérico parece un NIT, código, referencia, etc.
    private fun looksLikeIdentifier(raw: String, normalizedLine: String): Boolean {
        if (IDENTIFIER_KEYWORDS.any { lineContainsKeyword(normalizedLine, it) }) return true
        if (CURRENCY_REGEX.containsMatchIn(raw)) return false  // moneda = no es identificador
        val digitsOnly = raw.replace(Regex("\\D"), "")
        return !raw.contains(Regex("[.,]")) && digitsOnly.length >= 8
    }

    // ── FECHA ─────────────────────────────────────────────────────────────────
    private fun extractDate(lines: List<String>): String? {
        val dateRegex = Regex(
            "(\\d{1,2}[/\\-.](\\d{1,2})[/\\-.](\\d{2,4}))|(\\d{4}[/\\-.](\\d{1,2})[/\\-.](\\d{1,2}))"
        )
        for (line in lines) {
            val raw = dateRegex.find(line)?.value ?: continue
            return normalizeDate(raw) ?: continue
        }
        return null
    }

    private fun normalizeDate(raw: String): String? {
        val formats = listOf(
            "dd/MM/yyyy", "dd-MM-yyyy", "dd.MM.yyyy",
            "MM/dd/yyyy", "MM-dd-yyyy",
            "yyyy-MM-dd", "yyyy/MM/dd",
            "dd/MM/yy", "dd-MM-yy", "MM/dd/yy", "MM-dd-yy"
        ).map { SimpleDateFormat(it, Locale.getDefault()).apply { isLenient = false } }
        val output = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        for (fmt in formats) {
            try { return output.format(fmt.parse(raw) ?: continue) } catch (_: Exception) {}
        }
        return null
    }

    // ── HORA ──────────────────────────────────────────────────────────────────
    private fun extractTime(lines: List<String>): String? {
        val timeRegex = Regex("(\\d{1,2}:\\d{2}(?::\\d{2})?\\s?(?:am|pm|AM|PM)?)")
        for (line in lines) {
            val raw = timeRegex.find(line)?.value?.trim() ?: continue
            return normalizeTime(raw)
        }
        return null
    }

    private fun normalizeTime(raw: String): String {
        return try {
            val hasAmPm = raw.lowercase().let { it.contains("am") || it.contains("pm") }
            val candidates = if (hasAmPm) listOf(
                SimpleDateFormat("hh:mm a", Locale.US),
                SimpleDateFormat("h:mm a", Locale.US),
                SimpleDateFormat("hh:mm:ss a", Locale.US)
            ) else listOf(
                SimpleDateFormat("HH:mm", Locale.US),
                SimpleDateFormat("HH:mm:ss", Locale.US)
            )
            val output = SimpleDateFormat("hh:mma", Locale.getDefault())
            for (fmt in candidates) {
                try { return output.format(fmt.parse(raw) ?: continue) } catch (_: Exception) {}
            }
            raw
        } catch (_: Exception) { raw }
    }

    // ── UBICACIÓN ─────────────────────────────────────────────────────────────
    // Primero busca líneas con etiqueta "Dirección:", luego detecta
    // formato de dirección colombiana (Carrera, Calle, Av, etc.)
    private fun extractLocation(lines: List<String>): String? {
        for (i in lines.indices) {
            val labeled = extractLabeledAddress(lines, i)
            if (labeled != null) return labeled
        }
        for (i in lines.indices) {
            if (looksLikeColombianAddress(lines[i])) {
                return combineAddressLines(lines, i)
            }
        }
        return null
    }

    private fun extractLabeledAddress(lines: List<String>, index: Int): String? {
        val line = lines[index]
        for (label in ADDRESS_LABELS) {
            val match = Regex(
                "^\\s*$label\\s*[:.-]?\\s*(.*)\$", RegexOption.IGNORE_CASE
            ).find(line.lowercase()) ?: continue
            val extracted = line.substring(match.range.first)
                .replace(Regex("^\\s*$label\\s*[:.-]?\\s*", RegexOption.IGNORE_CASE), "").trim()
            if (extracted.isNotEmpty() && looksLikeColombianAddress(extracted)) return extracted
            if (index + 1 < lines.size && looksLikeColombianAddress(lines[index + 1])) {
                return combineAddressLines(lines, index + 1)
            }
        }
        return null
    }

    private fun looksLikeColombianAddress(line: String): Boolean =
        COLOMBIAN_ADDRESS_REGEX.containsMatchIn(line.lowercase()) && line.any { it.isDigit() }

    private fun combineAddressLines(lines: List<String>, startIndex: Int): String {
        val segments = mutableListOf(lines[startIndex])
        if (startIndex + 1 < lines.size) {
            val next = lines[startIndex + 1]
            if (Regex("\\b(bogota|bogotá|medellin|medellín|cali|colombia)\\b")
                    .containsMatchIn(next.lowercase()) ||
                (!looksLikeColombianAddress(next) && next.any { it.isLetter() } && next.length <= 40)) {
                segments.add(next)
            }
        }
        return segments.joinToString(", ")
    }

    // ── DATA CLASS INTERNO ────────────────────────────────────────────────────
    private data class AmountCandidate(
        val value: Int,
        val lineIndex: Int,
        val score: Int,
        val hasSeparator: Boolean,
        val hasCurrencyMarker: Boolean,
        val onTotalLine: Boolean,
        val afterTotalLine: Boolean
    ) {
        val hasMoneySignal get() = hasSeparator || hasCurrencyMarker || onTotalLine || afterTotalLine
    }
}
