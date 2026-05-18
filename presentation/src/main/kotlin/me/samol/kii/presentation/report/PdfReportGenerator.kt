package me.samol.kii.presentation.report

import me.samol.kii.core.formatDecimal
import me.samol.kii.domain.model.AssetRelationType
import me.samol.kii.domain.model.InitialStabilityInput
import me.samol.kii.domain.model.StabilityInput
import me.samol.kii.domain.model.StabilityModelType
import me.samol.kii.domain.model.StabilityResult
import me.samol.kii.domain.model.StabilityStatus
import me.samol.kii.domain.model.StabilityStepResult
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType0Font
import java.awt.Color
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

/**
 * Generates a PDF report for initial and dynamic stability calculations.
 *
 * @param outputDirectory directory where generated reports are saved.
 */
class PdfReportGenerator(
    private val outputDirectory: Path = Paths.get(
        System.getProperty("user.home"),
        "Documents",
        "KiiStabilityReports"
    )
) {
    private val fileTimestampFormat = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    private val reportDateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    /**
     * Creates and saves a PDF report.
     *
     * @param data complete report data snapshot.
     * @return path to the generated PDF file.
     */
    fun generate(data: StabilityReportData): Path {
        Files.createDirectories(outputDirectory)
        val now = LocalDateTime.now()
        val output = outputDirectory.resolve("kii_stability_report_${fileTimestampFormat.format(now)}.pdf")

        PDDocument().use { document ->
            val font = loadFont(document)
            val writer = PdfWriter(document, font, "Дата создания отчёта: ${reportDateFormat.format(now)}")

            writer.title("Отчет по расчету устойчивости КИИ")
            writer.paragraph("В отчете приведены исходные данные по активам, выбранный тип связи, параметры динамической модели и результаты расчета.")

            writer.section("Активы и расчет начальной устойчивости")
            writer.keyValues(
                listOf(
                    "Тип связи" to relationLabel(data.initialInput),
                    "X0" to formatDecimal(data.initialResult.x0)
                )
            )
            writer.subsection("Таблица активов")
            writer.table(
                headers = listOf("id", "name", "C", "I", "A", "tC", "vC", "tI", "vI", "tA", "vA", "weight"),
                rows = data.initialInput.assets.map { asset ->
                    listOf(
                        asset.id,
                        asset.name,
                        formatDecimal(asset.ciaWeights.confidentiality),
                        formatDecimal(asset.ciaWeights.integrity),
                        formatDecimal(asset.ciaWeights.availability),
                        formatDecimal(asset.threatProfile.threatC),
                        formatDecimal(asset.threatProfile.vulnerabilityC),
                        formatDecimal(asset.threatProfile.threatI),
                        formatDecimal(asset.threatProfile.vulnerabilityI),
                        formatDecimal(asset.threatProfile.threatA),
                        formatDecimal(asset.threatProfile.vulnerabilityA),
                        formatDecimal(asset.assetWeight)
                    )
                },
                widths = listOf(44f, 132f, 42f, 42f, 42f, 42f, 42f, 42f, 42f, 42f, 42f, 54f),
                fontSize = 7.4f
            )

            writer.subsection("Расчет по активам")
            writer.table(
                headers = listOf("id", "name", "w", "risk C", "risk I", "risk A", "risk CIA", "Si"),
                rows = data.initialResult.assetResults.map { asset ->
                    listOf(
                        asset.assetId,
                        asset.assetName,
                        formatDecimal(asset.normalizedAssetWeight),
                        formatDecimal(asset.riskC),
                        formatDecimal(asset.riskI),
                        formatDecimal(asset.riskA),
                        formatDecimal(asset.riskCIA),
                        formatDecimal(asset.assetResilience)
                    )
                },
                widths = listOf(54f, 160f, 58f, 70f, 70f, 70f, 86f, 68f),
                fontSize = 8f
            )

            writer.section("Параметры динамической модели")
            writer.keyValues(dynamicParameterRows(data.dynamicInput))

            writer.section("Результаты расчета")
            writer.chart(data.dynamicResult)
            writer.keyValues(
                listOf(
                    "Затраты U" to money(data.dynamicResult.spentControl),
                    "Затраты R" to money(data.dynamicResult.spentRegulation),
                    "Затраты защиты" to money(data.dynamicResult.spentDefense),
                    "Затраты атакующего" to money(data.dynamicResult.spentAttacker),
                    "Статус" to statusLabel(data.dynamicResult.status)
                )
            )
            writer.subsection("Таблица результатов")
            writer.table(
                headers = listOf("n", "Xn", "baselineX", "deviation", "U", "R", "random", "attack", "S"),
                rows = data.dynamicResult.steps.map { step ->
                    listOf(
                        step.step.toString(),
                        formatDecimal(step.x),
                        formatDecimal(step.baselineX),
                        formatDecimal(step.deviation),
                        step.controlImpulse.toString(),
                        step.regulationImpulse.toString(),
                        formatDecimal(step.randomValue),
                        if (step.attackEvent) "да" else "нет",
                        formatDecimal(step.attackValue)
                    )
                },
                widths = listOf(42f, 74f, 88f, 86f, 44f, 44f, 86f, 70f, 72f),
                fontSize = 8f
            )

            writer.close()
            document.save(output.toFile())
        }

        return output
    }

    private fun loadFont(document: PDDocument): PDFont {
        val candidates = listOf(
            "C:\\Windows\\Fonts\\arial.ttf",
            "C:\\Windows\\Fonts\\segoeui.ttf",
            "C:\\Windows\\Fonts\\times.ttf"
        )
        val fontFile = candidates.map(::File).firstOrNull { it.exists() }
            ?: throw IllegalStateException("Не найден системный шрифт для генерации PDF")
        return PDType0Font.load(document, fontFile)
    }

    private fun dynamicParameterRows(input: StabilityInput): List<Pair<String, String>> {
        return listOf(
            "Выбранная модель" to modelLabel(input.modelType),
            "Количество шагов n" to input.steps.toString(),
            "Dt" to formatDecimal(input.dt),
            "T" to formatDecimal(input.t),
            "X0" to formatDecimal(input.x0),
            "Пороговое значение Xкр" to formatDecimal(input.xCritical),
            "a" to (input.a?.let(::formatDecimal) ?: "не используется"),
            "Управление U" to enabledLabel(input.controlEnabled),
            "Стоимость U0" to money(input.controlCost),
            "Расписание U" to input.controlSchedule.joinToString(", "),
            "Восстановление R" to enabledLabel(input.regulationEnabled),
            "Стоимость R0" to money(input.regulationCost),
            "Порог R" to formatDecimal(input.disorderThreshold),
            "Атака S" to enabledLabel(input.attackEnabled),
            "Порог S" to formatDecimal(input.attackThreshold),
            "Стоимость S0" to money(input.attackCost),
            "kU" to formatDecimal(input.kControl),
            "kR" to formatDecimal(input.kRegulation),
            "kS" to formatDecimal(input.kAttack)
        )
    }
}

private class PdfWriter(
    private val document: PDDocument,
    private val font: PDFont,
    private val footerText: String
) {
    private val pageSize = PDRectangle(PDRectangle.A4.height, PDRectangle.A4.width)
    private val margin = 36f
    private val bottomReserved = 32f
    private val normalText = Color(0x1F2937)
    private val borderColor = Color(0xCBD5E1)
    private var page = PDPage(pageSize)
    private var content: PDPageContentStream
    private var y: Float

    init {
        document.addPage(page)
        content = PDPageContentStream(document, page)
        y = page.mediaBox.height - margin
        footer()
    }

    fun close() {
        content.close()
    }

    fun title(text: String) {
        ensure(34f)
        drawText(text, margin, y, 18f, Color(0x111827))
        y -= 28f
    }

    fun section(text: String) {
        ensure(30f)
        y -= 6f
        drawText(text, margin, y, 14f, Color(0x111827))
        y -= 22f
    }

    fun subsection(text: String) {
        ensure(24f)
        y -= 4f
        drawText(text, margin, y, 11f, Color(0x334155))
        y -= 18f
    }

    fun paragraph(text: String) {
        val lines = wrap(text, page.mediaBox.width - margin * 2, 10f)
        ensure(lines.size * 14f + 8f)
        lines.forEach { line ->
            drawText(line, margin, y, 10f, normalText)
            y -= 14f
        }
        y -= 6f
    }

    fun keyValues(rows: List<Pair<String, String>>) {
        table(
            headers = emptyList(),
            rows = rows.map { listOf(it.first, it.second) },
            widths = listOf(210f, 430f),
            fontSize = 9f,
            header = false
        )
    }

    fun table(
        headers: List<String>,
        rows: List<List<String>>,
        widths: List<Float>,
        fontSize: Float,
        header: Boolean = true
    ) {
        if (header && headers.isNotEmpty()) {
            drawTableRow(headers, widths, fontSize, Color(0xE2E8F0), true)
        }
        rows.forEach { row ->
            drawTableRow(row, widths, fontSize, Color.WHITE, false)
        }
        y -= 10f
    }

    fun chart(result: StabilityResult) {
        ensure(246f)
        val left = margin
        val top = y
        val width = 650f
        val height = 220f
        val axisX = left + 46f
        val axisY = top - height + 34f
        val plotWidth = width - 76f
        val plotHeight = height - 58f
        val maxStep = max(1, result.steps.maxOfOrNull { it.step } ?: 1)
        val maxValue = max(1.0, max(result.xCritical, result.steps.maxOfOrNull { it.x } ?: result.xCritical)) * 1.12

        content.setLineWidth(1f)
        content.setStrokingColor(Color(0x111827))
        line(axisX, axisY, axisX + plotWidth + 16f, axisY)
        line(axisX, axisY, axisX, axisY + plotHeight + 16f)
        arrow(axisX + plotWidth + 16f, axisY, true)
        arrow(axisX, axisY + plotHeight + 16f, false)
        drawText("n", axisX + plotWidth + 24f, axisY - 4f, 8f, Color(0x111827))
        drawText("Xn", axisX - 12f, axisY + plotHeight + 24f, 8f, Color(0x111827))

        val yTickStep = 0.2
        var tick = 0.0
        while (tick <= maxValue + 0.0001) {
            val ty = yFor(tick, axisY, plotHeight, maxValue)
            content.setStrokingColor(Color(0xE5E7EB))
            line(axisX, ty, axisX + plotWidth, ty)
            content.setStrokingColor(Color(0x111827))
            line(axisX - 3f, ty, axisX, ty)
            drawText(formatDecimal(tick), axisX - 30f, ty - 3f, 7f, Color(0x475569))
            tick += yTickStep
        }

        val xLabelStep = max(1, ceil(maxStep / 24.0).toInt())
        for (step in 0..maxStep) {
            val tx = xFor(step, axisX, plotWidth, maxStep)
            content.setStrokingColor(Color(0x111827))
            line(tx, axisY, tx, axisY - 3f)
            if (step % xLabelStep == 0) {
                drawText(step.toString(), tx - 4f, axisY - 14f, 7f, Color(0x475569))
            }
        }

        val thresholdY = yFor(result.xCritical, axisY, plotHeight, maxValue)
        content.setStrokingColor(Color(0xE11D48))
        content.setLineWidth(1.2f)
        line(axisX, thresholdY, axisX + plotWidth, thresholdY)
        drawText("Порог Xкр = ${formatDecimal(result.xCritical)}", axisX + plotWidth - 100f, thresholdY + 6f, 8f, Color(0xE11D48))

        val points = result.steps.sortedBy { it.step }
        content.setStrokingColor(Color(0x2563EB))
        content.setLineWidth(1.5f)
        points.zipWithNext().forEach { (a, b) ->
            line(
                xFor(a.step, axisX, plotWidth, maxStep),
                yFor(a.x, axisY, plotHeight, maxValue),
                xFor(b.step, axisX, plotWidth, maxStep),
                yFor(b.x, axisY, plotHeight, maxValue)
            )
        }
        points.forEach { step ->
            val cx = xFor(step.step, axisX, plotWidth, maxStep)
            val cy = yFor(step.x, axisY, plotHeight, maxValue)
            content.setNonStrokingColor(pointColor(step, result.xCritical))
            filledCircle(cx, cy, 2.8f)
        }

        y -= height + 18f
    }

    private fun drawTableRow(values: List<String>, widths: List<Float>, fontSize: Float, background: Color, bold: Boolean) {
        val cellLines = values.mapIndexed { index, value ->
            wrap(value, widths[index] - 8f, fontSize)
        }
        val rowHeight = max(19f, cellLines.maxOfOrNull { it.size }?.times(fontSize + 3f)?.plus(8f) ?: 19f)
        ensure(rowHeight)

        var x = margin
        values.indices.forEach { index ->
            content.setNonStrokingColor(background)
            content.addRect(x, y - rowHeight, widths[index], rowHeight)
            content.fill()
            content.setStrokingColor(borderColor)
            content.addRect(x, y - rowHeight, widths[index], rowHeight)
            content.stroke()

            val textColor = if (bold) Color(0x0F172A) else normalText
            cellLines[index].forEachIndexed { lineIndex, line ->
                drawText(line, x + 4f, y - fontSize - 6f - lineIndex * (fontSize + 3f), fontSize, textColor)
            }
            x += widths[index]
        }
        y -= rowHeight
    }

    private fun wrap(text: String, maxWidth: Float, fontSize: Float): List<String> {
        if (text.isBlank()) return listOf("")
        val words = text.replace("\n", " ").split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var line = ""
        words.forEach { word ->
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (textWidth(candidate, fontSize) <= maxWidth) {
                line = candidate
            } else {
                if (line.isNotEmpty()) lines += line
                line = word
            }
        }
        if (line.isNotEmpty()) lines += line
        return lines
    }

    private fun textWidth(text: String, fontSize: Float): Float {
        return font.getStringWidth(text) / 1000f * fontSize
    }

    private fun ensure(height: Float) {
        if (y - height < margin + bottomReserved) {
            content.close()
            page = PDPage(pageSize)
            document.addPage(page)
            content = PDPageContentStream(document, page)
            y = page.mediaBox.height - margin
            footer()
        }
    }

    private fun footer() {
        drawText(footerText, margin, 18f, 8f, Color(0x64748B))
    }

    private fun drawText(text: String, x: Float, baseline: Float, size: Float, color: Color) {
        content.beginText()
        content.setFont(font, size)
        content.setNonStrokingColor(color)
        content.newLineAtOffset(x, baseline)
        content.showText(text)
        content.endText()
    }

    private fun xFor(step: Int, axisX: Float, plotWidth: Float, maxStep: Int): Float {
        return axisX + step.toFloat() / maxStep.toFloat() * plotWidth
    }

    private fun yFor(value: Double, axisY: Float, plotHeight: Float, maxValue: Double): Float {
        return axisY + (value / maxValue * plotHeight).toFloat()
    }

    private fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
        content.moveTo(x1, y1)
        content.lineTo(x2, y2)
        content.stroke()
    }

    private fun arrow(x: Float, y: Float, horizontal: Boolean) {
        if (horizontal) {
            line(x, y, x - 7f, y + 4f)
            line(x, y, x - 7f, y - 4f)
        } else {
            line(x, y, x - 4f, y - 7f)
            line(x, y, x + 4f, y - 7f)
        }
    }

    private fun filledCircle(cx: Float, cy: Float, radius: Float) {
        val control = radius * 0.5522848f
        content.moveTo(cx + radius, cy)
        content.curveTo(cx + radius, cy + control, cx + control, cy + radius, cx, cy + radius)
        content.curveTo(cx - control, cy + radius, cx - radius, cy + control, cx - radius, cy)
        content.curveTo(cx - radius, cy - control, cx - control, cy - radius, cx, cy - radius)
        content.curveTo(cx + control, cy - radius, cx + radius, cy - control, cx + radius, cy)
        content.fill()
    }
}

private val moneyFormat = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.US))

private fun money(value: Double): String = "${moneyFormat.format(value)} руб."

private fun enabledLabel(enabled: Boolean): String = if (enabled) "вкл" else "выкл"

private fun relationLabel(input: InitialStabilityInput): String {
    return when (input.relationType) {
        AssetRelationType.INDEPENDENT -> "Независимые активы"
        AssetRelationType.WEAK_DEPENDENCY -> "Слабая зависимость"
        AssetRelationType.STRONG_DEPENDENCY -> "Сильная зависимость"
        AssetRelationType.FEEDBACK -> "Обратная связь: ${baseRelationLabel(input.feedbackBaseRelationType)}; feedback = ${formatDecimal(input.feedbackCoefficient)}"
    }
}

private fun baseRelationLabel(relationType: AssetRelationType): String {
    return when (relationType) {
        AssetRelationType.INDEPENDENT -> "Независимые активы"
        AssetRelationType.WEAK_DEPENDENCY -> "Слабая зависимость"
        AssetRelationType.STRONG_DEPENDENCY -> "Сильная зависимость"
        AssetRelationType.FEEDBACK -> "Обратная связь"
    }
}

private fun modelLabel(modelType: StabilityModelType): String {
    return when (modelType) {
        StabilityModelType.EXPONENTIAL -> "Экспоненциальная модель"
        StabilityModelType.NEAR_EXPONENTIAL -> "Почти экспоненциальная модель"
        StabilityModelType.AGING -> "Модель старения"
    }
}

private fun statusLabel(status: StabilityStatus): String {
    return when (status) {
        StabilityStatus.STABLE -> "устойчивость не опускалась ниже порога"
        StabilityStatus.CRITICAL -> "устойчивость опускалась ниже порога"
    }
}

private fun pointColor(step: StabilityStepResult, xCritical: Double): Color {
    return when {
        step.attackEvent -> Color(0xDC2626)
        step.controlImpulse == 1 -> Color(0x059669)
        step.regulationImpulse == 1 -> Color(0xF59E0B)
        step.x < xCritical -> Color(0x7C3AED)
        else -> Color(0x2563EB)
    }
}