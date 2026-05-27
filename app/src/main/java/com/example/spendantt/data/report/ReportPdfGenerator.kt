package com.example.spendantt.data.report

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.res.ResourcesCompat
import com.example.spendantt.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.max

/**
 * Generates a multi-page A4 PDF for a [FinancialReport].
 *
 * Layout philosophy:
 *  - Reserved widths so amounts never collide with names or category bars.
 *  - Wrap expense names to at most 2 lines.
 *  - Per-page header (logo + "SpendAnt" + period) and footer (separator + generated stamp).
 *  - ensureSpace() opens a new page when a block doesn't fit.
 */
class ReportPdfGenerator(private val context: Context) {

    // A4 portrait in points
    private val pageWidth = 595
    private val pageHeight = 842

    private val marginH = 36f
    private val headerHeight = 76f
    private val footerHeight = 40f

    private val contentLeft = marginH
    private val contentRight get() = pageWidth - marginH
    private val contentTop get() = headerHeight + 18f
    private val contentBottom get() = pageHeight - footerHeight

    // Palette
    private val brandGreen = Color.parseColor("#44C669")
    private val brandPastel = Color.parseColor("#E0FFE9")
    private val grayText = Color.parseColor("#5E5E5E")
    private val redAmount = Color.parseColor("#F04C4C")
    private val faintLine = Color.parseColor("#E0E0E0")
    private val categoryColors = listOf(
        Color.parseColor("#44C669"),
        Color.parseColor("#4FC3F7"),
        Color.parseColor("#BA68C8"),
        Color.parseColor("#FF8A65"),
        Color.parseColor("#AED581"),
    )

    private val displayCurrencyFormatterLarge = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))
    private val displayCurrencyFormatterSmall = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

    // Holds mutable rendering state for the current PDF document.
    private class PageContext(
        val document: PdfDocument,
        val report: FinancialReport,
    ) {
        var pageNumber: Int = 0
        var page: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var cursorY: Float = 0f
    }

    suspend fun generate(report: FinancialReport): File = withContext(Dispatchers.IO) {
        val pdf = PdfDocument()
        val ctx = PageContext(pdf, report)
        openNewPage(ctx)

        drawTotalCard(ctx)
        drawDailyHistogram(ctx)
        drawTopExpenses(ctx)
        drawTopCategories(ctx)
        drawInsights(ctx)

        closeCurrentPage(ctx)

        val file = outputFile(report)
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        file
    }

    // ── Page lifecycle ───────────────────────────────────────────────────────

    private fun openNewPage(ctx: PageContext) {
        ctx.pageNumber += 1
        val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, ctx.pageNumber).create()
        val page = ctx.document.startPage(info)
        ctx.page = page
        ctx.canvas = page.canvas
        drawHeader(ctx)
        drawFooter(ctx)
        ctx.cursorY = contentTop
    }

    private fun closeCurrentPage(ctx: PageContext) {
        ctx.page?.let { ctx.document.finishPage(it) }
        ctx.page = null
        ctx.canvas = null
    }

    private fun ensureSpace(ctx: PageContext, requiredHeight: Float) {
        if (ctx.cursorY + requiredHeight > contentBottom) {
            closeCurrentPage(ctx)
            openNewPage(ctx)
        }
    }

    // ── Header / Footer ──────────────────────────────────────────────────────

    private fun drawHeader(ctx: PageContext) {
        val canvas = ctx.canvas ?: return
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = brandGreen }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), headerHeight, bgPaint)

        // Logo on the left, drawn directly on the green band (no separate circle bg).
        val logoSize = 44
        val logoLeft = marginH.toInt()
        val logoTop = ((headerHeight - logoSize) / 2f).toInt()
        runCatching {
            val drawable = ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.spendant_logo,
                context.theme,
            )
            drawable?.setBounds(logoLeft, logoTop, logoLeft + logoSize, logoTop + logoSize)
            drawable?.draw(canvas)
        }

        // Title block right of the logo
        val titleX = logoLeft + logoSize + 12f
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 20f
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.DEFAULT
            textSize = 10f
        }
        val titleY = headerHeight / 2f - 2f
        canvas.drawText("SpendAnt", titleX, titleY, titlePaint)
        canvas.drawText("Financial Report", titleX, titleY + 14f, subPaint)

        // Period right-aligned
        val periodPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 12f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(ctx.report.periodLabel, contentRight, headerHeight / 2f + 4f, periodPaint)
    }

    private fun drawFooter(ctx: PageContext) {
        val canvas = ctx.canvas ?: return
        val linePaint = Paint().apply { color = faintLine; strokeWidth = 1f }
        val lineY = pageHeight - footerHeight + 6f
        canvas.drawLine(contentLeft, lineY, contentRight, lineY, linePaint)

        val stamp = SimpleDateFormat("MMM d, yyyy 'at' HH:mm", Locale.ENGLISH)
            .format(Date(ctx.report.generatedAt))
        val text = "Generated by SpendAnt | $stamp"

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = grayText
            textSize = 9f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(text, pageWidth / 2f, lineY + 18f, paint)
    }

    // ── Sections ─────────────────────────────────────────────────────────────

    private fun drawTotalCard(ctx: PageContext) {
        val canvas = ctx.canvas ?: return
        val cardHeight = 64f
        ensureSpace(ctx, cardHeight + 18f)

        val rect = RectF(contentLeft, ctx.cursorY, contentRight, ctx.cursorY + cardHeight)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = brandPastel }
        canvas.drawRoundRect(rect, 8f, 8f, bgPaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = grayText
            textSize = 10f
        }
        canvas.drawText("Total spent", rect.left + 14f, rect.top + 22f, labelPaint)

        val amountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 22f
        }
        canvas.drawText(
            formatDisplayAmount(ctx.report.totalSpentCop, ctx.report),
            rect.left + 14f,
            rect.top + 48f,
            amountPaint,
        )
        ctx.cursorY += cardHeight + 18f
    }

    private fun drawDailyHistogram(ctx: PageContext) {
        val canvas = ctx.canvas ?: return
        val report = ctx.report
        if (report.dailySpends.isEmpty()) return

        val titleHeight = 22f
        val chartHeight = 110f
        val labelStripHeight = 16f
        val total = titleHeight + chartHeight + labelStripHeight + 14f
        ensureSpace(ctx, total)

        drawSectionTitle(ctx, "Daily Spending")
        val chartTop = ctx.cursorY
        val chartBottom = chartTop + chartHeight
        val axisY = chartBottom

        val maxAmount = report.dailySpends.maxOf { it.amountCop }.coerceAtLeast(1.0)
        val barAreaLeft = contentLeft
        val barAreaRight = contentRight
        val available = barAreaRight - barAreaLeft
        val n = report.dailySpends.size
        val spacing = 4f
        val barWidth = max(2f, (available - spacing * (n - 1)) / n)

        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = brandGreen }
        val axisPaint = Paint().apply { color = faintLine; strokeWidth = 1f }
        canvas.drawLine(barAreaLeft, axisY, barAreaRight, axisY, axisPaint)

        report.dailySpends.forEachIndexed { index, ds ->
            val ratio = (ds.amountCop / maxAmount).toFloat().coerceIn(0f, 1f)
            val h = ratio * (chartHeight - 6f)
            val x = barAreaLeft + index * (barWidth + spacing)
            val top = axisY - h
            canvas.drawRoundRect(RectF(x, top, x + barWidth, axisY), 2f, 2f, barPaint)
        }

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = grayText
            textSize = 8f
            textAlign = Paint.Align.CENTER
        }
        val maxLabels = 6
        val step = max(1, n / maxLabels)
        report.dailySpends.forEachIndexed { index, ds ->
            if (index % step == 0) {
                val x = barAreaLeft + index * (barWidth + spacing) + barWidth / 2f
                canvas.drawText(ds.date.dayOfMonth.toString(), x, axisY + 12f, labelPaint)
            }
        }

        ctx.cursorY = chartBottom + labelStripHeight + 14f
    }

    private fun drawTopExpenses(ctx: PageContext) {
        val canvas = ctx.canvas ?: return
        if (ctx.report.topExpenses.isEmpty()) return
        drawSectionTitle(ctx, "Top Expenses")

        // Reserve a fixed amount column on the right so names never collide with the price.
        val amountColumnWidth = 110f
        val rowGap = 6f

        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 12f
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = grayText
            textSize = 9f
        }
        val amountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = redAmount
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 12f
            textAlign = Paint.Align.RIGHT
        }
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = brandPastel }

        ctx.report.topExpenses.forEach { expense ->
            val nameMaxWidth = (contentRight - contentLeft) - amountColumnWidth - 28f
            val nameLines = wrapText(expense.name.ifBlank { "Unnamed expense" }, namePaint, nameMaxWidth, maxLines = 2)
            // Row height: paddingTop + nameLines * 16 + 4 + subline 12 + paddingBottom
            val rowHeight = 14f + nameLines.size * 16f + 4f + 12f + 12f

            ensureSpace(ctx, rowHeight + rowGap)
            val rect = RectF(contentLeft, ctx.cursorY, contentRight, ctx.cursorY + rowHeight)
            canvas.drawRoundRect(rect, 8f, 8f, bgPaint)

            // Draw each name line explicitly to avoid clipping
            val nameLeftX = rect.left + 14f
            var lineY = rect.top + 18f
            nameLines.forEach { line ->
                canvas.drawText(line, nameLeftX, lineY, namePaint)
                lineY += 16f
            }

            // Subline: category | date
            val dateStr = expense.date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH))
            canvas.drawText("${expense.category}  |  $dateStr", nameLeftX, lineY + 4f, subPaint)

            // Amount right-aligned, vertically centered-ish
            val amountY = rect.top + rowHeight / 2f + 4f
            canvas.drawText(
                formatDisplayAmount(expense.amountCop, ctx.report),
                rect.right - 14f,
                amountY,
                amountPaint,
            )

            ctx.cursorY += rowHeight + rowGap
        }
        ctx.cursorY += 8f
    }

    private fun drawTopCategories(ctx: PageContext) {
        val canvas = ctx.canvas ?: return
        if (ctx.report.topCategories.isEmpty()) return
        drawSectionTitle(ctx, "Top Categories")

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val amountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = grayText
            textSize = 10f
        }
        val total = ctx.report.topCategories.sumOf { it.amountCop }.coerceAtLeast(1.0)

        // Fixed reserved widths so the bar can never overlap label or amount.
        val labelColumnWidth = 110f
        val amountColumnWidth = 110f
        val barGapLeft = 8f
        val barGapRight = 4f
        val rowHeight = 26f

        ctx.report.topCategories.forEachIndexed { idx, category ->
            ensureSpace(ctx, rowHeight)

            val rowTop = ctx.cursorY
            val centerY = rowTop + rowHeight / 2f + 4f // rough baseline

            // Label (truncated to fit its reserved column)
            val labelText = ellipsize(category.label, labelPaint, labelColumnWidth - 6f)
            canvas.drawText(labelText, contentLeft, centerY, labelPaint)

            // Amount, placed right after the label column (before the bar)
            val amountText = formatDisplayAmount(category.amountCop, ctx.report)
            val amountText2 = ellipsize(amountText, amountPaint, amountColumnWidth - 6f)
            canvas.drawText(amountText2, contentLeft + labelColumnWidth, centerY, amountPaint)

            // Bar fills the remaining horizontal space
            val barLeft = contentLeft + labelColumnWidth + amountColumnWidth + barGapLeft
            val barRight = contentRight - barGapRight
            val barTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = faintLine }
            val barFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = categoryColors[idx % categoryColors.size]
            }
            val trackRect = RectF(barLeft, rowTop + 8f, barRight, rowTop + 18f)
            canvas.drawRoundRect(trackRect, 5f, 5f, barTrackPaint)
            val ratio = (category.amountCop / total).toFloat().coerceIn(0f, 1f)
            val fillRight = barLeft + (barRight - barLeft) * ratio
            if (fillRight > barLeft) {
                canvas.drawRoundRect(RectF(barLeft, rowTop + 8f, fillRight, rowTop + 18f), 5f, 5f, barFillPaint)
            }

            ctx.cursorY += rowHeight
        }
        ctx.cursorY += 12f
    }

    private fun drawInsights(ctx: PageContext) {
        if (ctx.report.bqInsights.isEmpty()) return
        val canvas = ctx.canvas ?: return
        drawSectionTitle(ctx, "Insights")

        val bullet = "•"
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 11f
        }
        val bulletPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = grayText
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val lineHeight = 16f
        val bulletInset = 14f

        ctx.report.bqInsights.forEach { insight ->
            val maxWidth = contentRight - contentLeft - bulletInset
            val lines = wrapText(insight, paint, maxWidth, maxLines = 4)
            val blockHeight = lines.size * lineHeight + 4f
            ensureSpace(ctx, blockHeight)

            canvas.drawText(bullet, contentLeft, ctx.cursorY + 12f, bulletPaint)
            var lineY = ctx.cursorY + 12f
            lines.forEach {
                canvas.drawText(it, contentLeft + bulletInset, lineY, paint)
                lineY += lineHeight
            }
            ctx.cursorY += blockHeight
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun drawSectionTitle(ctx: PageContext, title: String) {
        val canvas = ctx.canvas ?: return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 14f
        }
        ensureSpace(ctx, 24f)
        canvas.drawText(title, contentLeft, ctx.cursorY + 14f, paint)
        ctx.cursorY += 22f
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float, maxLines: Int): List<String> {
        if (text.isEmpty()) return listOf("")
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        words.forEach { word ->
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) lines.add(current.toString())
                current = StringBuilder(word)
                if (lines.size == maxLines - 1) {
                    // Last allowed line — truncate the remainder
                    val remaining = (listOf(word) + words.drop(words.indexOf(word) + 1)).joinToString(" ")
                    lines.add(ellipsize(remaining, paint, maxWidth))
                    return lines
                }
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        if (lines.size > maxLines) {
            val truncated = lines.take(maxLines).toMutableList()
            truncated[maxLines - 1] = ellipsize(truncated[maxLines - 1] + " " + lines.drop(maxLines).joinToString(" "), paint, maxWidth)
            return truncated
        }
        return lines
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        val ellipsis = "…"
        var end = text.length
        while (end > 0 && paint.measureText(text.substring(0, end) + ellipsis) > maxWidth) {
            end -= 1
        }
        return if (end <= 0) ellipsis else text.substring(0, end) + ellipsis
    }

    private fun formatDisplayAmount(amountCop: Double, report: FinancialReport): String {
        val value = amountCop * report.displayRate
        val formatted = if (value >= 100) displayCurrencyFormatterLarge.format(value)
                        else displayCurrencyFormatterSmall.format(value)
        return "${report.displayCurrency} $formatted"
    }

    private fun outputFile(report: FinancialReport): File {
        // App-scoped external storage works across all Android versions without
        // additional permissions and is exposed to other apps via FileProvider.
        val base = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir
        if (!base.exists()) base.mkdirs()
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date(report.generatedAt))
        val name = "SpendAnt_Report_${stamp}.pdf"
        return File(base, name)
    }
}
