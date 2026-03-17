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
 * Procesa una imagen de recibo con ML Kit y extrae:
 * - Nombre del lugar
 * - Total a pagar
 * - Fecha
 * - Hora
 * - Dirección / ubicación
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
        return parseReceiptText(text)
    }

    private suspend fun recognizeText(image: InputImage): String {
        return suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    continuation.resume(result.text)
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
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

    // ── NOMBRE ────────────────────────────────────────────────
    // El nombre del lugar suele estar en las primeras líneas, en mayúsculas
    private fun extractName(lines: List<String>): String? {
        return lines.take(5).firstOrNull { line ->
            line.length > 3 &&
                    line.any { it.isLetter() } &&
                    !line.contains(Regex("\\d{4,}")) // no es un número largo
        }?.trim()
    }

    // ── TOTAL ─────────────────────────────────────────────────
    // Busca líneas con "total", "total a pagar", "valor total", etc.
    private fun extractAmount(lines: List<String>): String? {
        val totalKeywords = listOf("total a pagar", "total pagar", "total:", "total", "valor total", "grand total", "amount due")

        for (i in lines.indices) {
            val line = lines[i].lowercase()
            if (totalKeywords.any { line.contains(it) }) {
                // Intentar extraer número de la misma línea
                val amount = extractNumber(lines[i])
                if (amount != null) return amount

                // Si no, buscar en la siguiente línea
                if (i + 1 < lines.size) {
                    val nextAmount = extractNumber(lines[i + 1])
                    if (nextAmount != null) return nextAmount
                }
            }
        }

        // Fallback: buscar el número más grande del recibo (suele ser el total)
        return lines.mapNotNull { extractNumber(it) }
            .maxByOrNull { it.replace(",", "").replace(".", "").toLongOrNull() ?: 0L }
    }

    private fun extractNumber(line: String): String? {
        // Busca patrones como: 25,020.37 | 25.020,37 | 25020 | $32,000
        val regex = Regex("\\$?\\s?([\\d]{1,3}(?:[.,][\\d]{3})*(?:[.,][\\d]{1,2})?)")
        val match = regex.findAll(line).lastOrNull()
        return match?.groupValues?.get(1)?.trim()
    }

    // ── FECHA ─────────────────────────────────────────────────
    private fun extractDate(lines: List<String>): String? {
        // Patrones: dd/mm/yyyy | dd-mm-yyyy | yyyy-mm-dd | dd.mm.yyyy
        val dateRegex = Regex("(\\d{1,2}[/\\-.](\\d{1,2})[/\\-.](\\d{2,4}))|(\\d{4}[/\\-.](\\d{1,2})[/\\-.](\\d{1,2}))")

        for (line in lines) {
            val match = dateRegex.find(line) ?: continue
            val raw = match.value

            // Intentar normalizar a MM/dd/yyyy
            val normalized = normalizeDate(raw)
            if (normalized != null) return normalized
        }
        return null
    }

    private fun normalizeDate(raw: String): String? {
        val formats = listOf(
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
            SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()),
            SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        )
        val output = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        for (fmt in formats) {
            try {
                val date = fmt.parse(raw) ?: continue
                return output.format(date)
            } catch (e: Exception) { continue }
        }
        return null
    }

    // ── HORA ──────────────────────────────────────────────────
    private fun extractTime(lines: List<String>): String? {
        // Patrones: 10:30 | 10:30pm | 22:30 | 10:30:00
        val timeRegex = Regex("(\\d{1,2}:\\d{2}(:\\d{2})?\\s?(am|pm|AM|PM)?)")

        for (line in lines) {
            val match = timeRegex.find(line) ?: continue
            val raw = match.value.trim()

            // Normalizar a hh:mma
            return normalizeTime(raw)
        }
        return null
    }

    private fun normalizeTime(raw: String): String {
        return try {
            val hasAmPm = raw.lowercase().contains("am") || raw.lowercase().contains("pm")
            val fmt = if (hasAmPm) SimpleDateFormat("hh:mm a", Locale.getDefault())
            else SimpleDateFormat("HH:mm", Locale.getDefault())
            val output = SimpleDateFormat("hh:mma", Locale.getDefault())
            val date = fmt.parse(raw.replace(":", ":").trim()) ?: return raw
            output.format(date)
        } catch (e: Exception) { raw }
    }

    // ── UBICACIÓN ─────────────────────────────────────────────
    // Busca líneas que parezcan direcciones: Calle, Carrera, Av, #, No.
    private fun extractLocation(lines: List<String>): String? {
        val locationKeywords = listOf("calle", "carrera", "cra", "cl", "av.", "avenida", "cll", "transversal", "diagonal", "dirección", "direccion", "#")

        return lines.firstOrNull { line ->
            val lower = line.lowercase()
            locationKeywords.any { lower.contains(it) }
        }?.trim()
    }
}