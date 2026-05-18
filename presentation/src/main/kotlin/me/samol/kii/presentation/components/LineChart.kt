package me.samol.kii.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import me.samol.kii.domain.model.StabilityStepResult
import org.jetbrains.skia.Font
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.Typeface
import kotlin.math.ceil
import kotlin.math.max

/**
 * Draws a line chart for Xn values.
 *
 * @param steps dynamic calculation rows.
 * @param xCritical critical threshold to draw as a horizontal line.
 * @return rendered chart.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun StabilityLineChart(steps: List<StabilityStepResult>, xCritical: Double) {
    var hoveredIndex by remember(steps) { mutableStateOf<Int?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(Color.White)
            .padding(12.dp)
            .onSizeChanged { canvasSize = it }
            .pointerMoveFilter(
                onMove = { offset ->
                    if (steps.isEmpty()) {
                        hoveredIndex = null
                    } else {
                        val left = 64f
                        val right = canvasSize.width.toFloat() - 28f
                        val plotRight = right - 24f
                        val top = 24f
                        val plotTop = top + 20f
                        val bottom = canvasSize.height.toFloat() - 48f
                        val chartWidth = max(1f, plotRight - left)
                        val maxY = ceil(max(1.0, steps.maxOf { it.x }.coerceAtLeast(xCritical)) / 0.2) * 0.2

                        fun xFor(index: Int): Float = left + chartWidth * index / max(1, steps.size - 1)
                        fun yFor(value: Double): Float {
                            val chartHeight = max(1f, bottom - plotTop)
                            return bottom - (value / maxY).toFloat() * chartHeight
                        }

                        hoveredIndex = steps.indices.minByOrNull { index ->
                            val dx = offset.x - xFor(index)
                            val dy = offset.y - yFor(steps[index].x)
                            dx * dx + dy * dy
                        }?.takeIf { index ->
                            val dx = offset.x - xFor(index)
                            val dy = offset.y - yFor(steps[index].x)
                            dx * dx + dy * dy <= 18f * 18f
                        }
                    }
                    false
                },
                onExit = {
                    hoveredIndex = null
                    false
                }
            )
    ) {
        if (steps.isEmpty()) return@Canvas
        val left = 64f
        val top = 24f
        val plotTop = top + 20f
        val right = size.width - 28f
        val plotRight = right - 24f
        val bottom = size.height - 48f
        val chartWidth = max(1f, plotRight - left)
        val chartHeight = max(1f, bottom - plotTop)
        val maxY = ceil(max(1.0, steps.maxOf { it.x }.coerceAtLeast(xCritical)) / 0.2) * 0.2

        fun xFor(index: Int): Float = left + chartWidth * index / max(1, steps.size - 1)
        fun yFor(value: Double): Float = bottom - (value / maxY).toFloat() * chartHeight

        val axisColor = Color(0xFF64748B)
        val tickColor = Color(0xFF94A3B8)
        drawLine(axisColor, Offset(left, top), Offset(left, bottom), strokeWidth = 2f)
        drawLine(axisColor, Offset(left, bottom), Offset(right, bottom), strokeWidth = 2f)
        drawLine(axisColor, Offset(left, top), Offset(left - 6f, top + 12f), strokeWidth = 2f)
        drawLine(axisColor, Offset(left, top), Offset(left + 6f, top + 12f), strokeWidth = 2f)
        drawLine(axisColor, Offset(right, bottom), Offset(right - 12f, bottom - 6f), strokeWidth = 2f)
        drawLine(axisColor, Offset(right, bottom), Offset(right - 12f, bottom + 6f), strokeWidth = 2f)

        var yTick = 0.0
        while (yTick <= maxY + 0.0001) {
            val y = yFor(yTick)
            drawLine(tickColor, Offset(left - 5f, y), Offset(left + 5f, y), strokeWidth = 1.5f)
            drawLine(Color(0xFFE2E8F0), Offset(left, y), Offset(plotRight, y), strokeWidth = 1f)
            yTick += 0.2
        }

        steps.forEachIndexed { index, _ ->
            val x = xFor(index)
            drawLine(tickColor, Offset(x, bottom - 5f), Offset(x, bottom + 5f), strokeWidth = 1.5f)
        }

        val criticalY = yFor(xCritical)
        drawLine(Color(0xFFE11D48), Offset(left, criticalY), Offset(plotRight, criticalY), strokeWidth = 2f)

        val path = Path()
        steps.forEachIndexed { index, row ->
            val point = Offset(xFor(index), yFor(row.x))
            if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        drawPath(path, Color(0xFF2563EB), style = Stroke(width = 3f))

        steps.forEachIndexed { index, row ->
            val point = Offset(xFor(index), yFor(row.x))
            val color = when {
                row.attackEvent -> Color(0xFFDC2626)
                row.controlImpulse == 1 -> Color(0xFF059669)
                row.regulationImpulse == 1 -> Color(0xFFF59E0B)
                row.x < xCritical -> Color(0xFF7C3AED)
                else -> Color(0xFF2563EB)
            }
            drawCircle(color, radius = 5f, center = point)
            drawCircle(Color.White, radius = 2f, center = point)
        }

        hoveredIndex?.let { index ->
            val row = steps[index]
            val point = Offset(xFor(index), yFor(row.x))
            drawCircle(Color(0xFF0F172A), radius = 7f, center = point)
            drawCircle(Color.White, radius = 3f, center = point)
        }

        drawIntoCanvas { canvas ->
            val font = Font(Typeface.makeDefault(), 14f)
            val smallFont = Font(Typeface.makeDefault(), 12f)
            val textPaint = Paint().apply {
                color = org.jetbrains.skia.Color.makeRGB(51, 65, 85)
                mode = PaintMode.FILL
            }
            val criticalPaint = Paint().apply {
                color = org.jetbrains.skia.Color.makeRGB(190, 18, 60)
                mode = PaintMode.FILL
            }
            val tooltipBackgroundPaint = Paint().apply {
                color = org.jetbrains.skia.Color.makeRGB(241, 245, 249)
                mode = PaintMode.FILL
            }
            val tooltipBorderPaint = Paint().apply {
                color = org.jetbrains.skia.Color.makeRGB(100, 116, 139)
                mode = PaintMode.STROKE
                strokeWidth = 1.5f
            }
            val nativeCanvas = canvas.nativeCanvas

            nativeCanvas.drawString("Xn", left - 12f, top - 6f, font, textPaint)
            nativeCanvas.drawString("n", right - 4f, bottom + 28f, font, textPaint)

            var yLabel = 0.0
            while (yLabel <= maxY + 0.0001) {
                val y = yFor(yLabel)
                val label = if (yLabel == 0.0) "0" else "%.1f".format(yLabel)
                nativeCanvas.drawString(label, left - 38f, y + 4f, smallFont, textPaint)
                yLabel += 0.2
            }

            steps.forEachIndexed { index, row ->
                val x = xFor(index)
                nativeCanvas.drawString(row.step.toString(), x - 4f, bottom + 20f, smallFont, textPaint)
            }

            nativeCanvas.drawString(
                "порог = %.2f".format(xCritical),
                left + 8f,
                criticalY - 6f,
                smallFont,
                criticalPaint
            )

            hoveredIndex?.let { index ->
                val row = steps[index]
                val point = Offset(xFor(index), yFor(row.x))
                val label = "n=${row.step}, Xn=%.4f".format(row.x)
                val tooltipWidth = 118f
                val tooltipHeight = 28f
                val tooltipX = (point.x + 12f).coerceAtMost(right - tooltipWidth)
                val tooltipY = (point.y - 38f).coerceAtLeast(plotTop)

                nativeCanvas.drawRect(
                    org.jetbrains.skia.Rect.makeXYWH(tooltipX, tooltipY, tooltipWidth, tooltipHeight),
                    tooltipBackgroundPaint
                )
                nativeCanvas.drawRect(
                    org.jetbrains.skia.Rect.makeXYWH(tooltipX, tooltipY, tooltipWidth, tooltipHeight),
                    tooltipBorderPaint
                )
                nativeCanvas.drawString(label, tooltipX + 8f, tooltipY + 18f, smallFont, textPaint)
            }
        }
    }
}