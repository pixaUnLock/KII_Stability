package me.samol.kii.presentation.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.samol.kii.core.formatMoney
import me.samol.kii.domain.model.StabilityResult
import me.samol.kii.presentation.components.AppButton
import me.samol.kii.presentation.components.Section
import me.samol.kii.presentation.components.StabilityLineChart
import me.samol.kii.presentation.components.TableCell
import me.samol.kii.presentation.report.PdfReportGenerator
import me.samol.kii.presentation.report.StabilityReportData
import me.samol.kii.presentation.viewmodel.ResultViewModel

/**
 * Renders calculation result table, chart, costs, and status.
 *
 * @param result dynamic calculation result.
 * @param reportData full calculation data used to generate a PDF report.
 * @param onBack back navigation callback.
 * @param onStart start navigation callback.
 * @return rendered UI.
 */
@Composable
fun CalculationResultScreen(
    result: StabilityResult,
    reportData: StabilityReportData?,
    onBack: () -> Unit,
    onStart: () -> Unit
) {
    val resultViewModel = ResultViewModel(result)
    val reportGenerator = remember { PdfReportGenerator() }
    var reportMessage by remember { mutableStateOf<String?>(null) }
    var reportError by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Результаты расчета", style = MaterialTheme.typography.h5)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppButton("Параметры модели", onBack)
                AppButton("В начало", onStart)
            }
        }
        Section("График Xn") {
            StabilityLineChart(result.steps, result.xCritical)
            ChartLegend()
        }
        Section("Затраты и статус") {
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Text("U: ${formatMoney(result.spentControl)}")
                Text("R: ${formatMoney(result.spentRegulation)}")
                Text("Защита: ${formatMoney(result.spentDefense)}")
                Text("Атакующий: ${formatMoney(result.spentAttacker)}")
            }
            Text(resultViewModel.statusLabel(), style = MaterialTheme.typography.h6, modifier = Modifier.padding(top = 8.dp))
        }
        Section("Таблица результатов") {
            Column(Modifier.horizontalScroll(rememberScrollState())) {
                Row {
                    listOf("n", "Xn", "baselineX", "deviation", "U", "R", "random", "attack", "S").forEach {
                        TableCell(it, width = 118, background = Color(0xFFE2E8F0))
                    }
                }
                result.steps.forEach { row ->
                    val xColor = if (row.x < result.xCritical) Color(0xFFDDD6FE) else Color.White
                    val controlColor = if (row.controlImpulse == 1) Color(0xFFA7F3D0) else Color.White
                    val regulationColor = if (row.regulationImpulse == 1) Color(0xFFFDE68A) else Color.White
                    val attackColor = if (row.attackEvent) Color(0xFFFECACA) else Color.White
                    Row {
                        TableCell(row.step.toString(), width = 118, background = xColor)
                        TableCell("%.4f".format(row.x), width = 118, background = xColor)
                        TableCell("%.4f".format(row.baselineX), width = 118)
                        TableCell("%.4f".format(row.deviation), width = 118)
                        TableCell(row.controlImpulse.toString(), width = 118, background = controlColor)
                        TableCell(row.regulationImpulse.toString(), width = 118, background = regulationColor)
                        TableCell("%.4f".format(row.randomValue), width = 118)
                        TableCell(if (row.attackEvent) "да" else "нет", width = 118, background = attackColor)
                        TableCell("%.2f".format(row.attackValue), width = 118, background = attackColor)
                    }
                }
            }
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                AppButton(
                    label = "Создать отчет",
                    enabled = reportData != null,
                    onClick = {
                        val data = reportData ?: return@AppButton
                        runCatching { reportGenerator.generate(data) }
                            .onSuccess { path ->
                                reportMessage = "Отчет сохранен: $path"
                                reportError = null
                            }
                            .onFailure { throwable ->
                                reportError = throwable.message ?: "Не удалось создать отчет"
                                reportMessage = null
                            }
                    }
                )
            }
            reportMessage?.let { message ->
                Text(message, color = Color(0xFF166534), modifier = Modifier.padding(top = 8.dp))
            }
            reportError?.let { message ->
                Text(message, color = Color(0xFFB91C1C), modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun ChartLegend() {
    Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Условные обозначения", style = MaterialTheme.typography.subtitle1)
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            LegendLine(Color(0xFF2563EB), "Xn")
            LegendLine(Color(0xFFE11D48), "пороговое значение")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            LegendDot(Color(0xFF2563EB), "расчетное значение устойчивости")
            LegendDot(Color(0xFF059669), "применено управляющее воздействие")
            LegendDot(Color(0xFFF59E0B), "сработало восстановление устойчивости")
            LegendDot(Color(0xFFDC2626), "произошло атакующее воздействие")
            LegendDot(Color(0xFF7C3AED), "устойчивость ниже порога")
        }
    }
}

@Composable
private fun LegendLine(color: Color, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Box(Modifier.size(width = 28.dp, height = 3.dp).background(color))
        Text(text, style = MaterialTheme.typography.body2)
    }
}

@Composable
private fun LegendDot(color: Color, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Box(Modifier.size(12.dp).background(color, CircleShape))
        Text(text, style = MaterialTheme.typography.body2)
    }
}