package me.samol.kii.presentation.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.samol.kii.domain.model.StabilityModelType
import me.samol.kii.presentation.components.ActionRow
import me.samol.kii.presentation.components.AppButton
import me.samol.kii.presentation.components.LabeledCheckbox
import me.samol.kii.presentation.components.NumericField
import me.samol.kii.presentation.components.Section
import me.samol.kii.presentation.components.TableCell
import me.samol.kii.presentation.viewmodel.DynamicModelViewModel

private const val ControlScheduleCellWidth = 88

/**
 * Renders dynamic model parameter input.
 *
 * @param viewModel dynamic model view model.
 * @param onBack back navigation callback.
 * @return rendered UI.
 */
@Composable
fun DynamicModelInputScreen(viewModel: DynamicModelViewModel, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Параметры динамической модели: ${viewModel.modelType.label()}", style = MaterialTheme.typography.h5)
            AppButton("Назад", onBack)
        }

        Section("Основные параметры") {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                NumericField("Количество шагов моделирования", viewModel.steps, { viewModel.steps = it }, width = 240)
                NumericField("Dt", viewModel.dt, { viewModel.dt = it })
                NumericField("T", viewModel.t, { viewModel.t = it })
                NumericField("X0", viewModel.x0, { viewModel.x0 = it })
                NumericField("Xкр", viewModel.xCritical, { viewModel.xCritical = it })
                if (viewModel.modelType.requiresA()) {
                    NumericField("a", viewModel.a, { viewModel.a = it })
                }
            }
        }

        Section("Управление U") {
            Row {
                LabeledCheckbox("Включить U", viewModel.controlEnabled) { viewModel.controlEnabled = it }
                NumericField("U0, руб", viewModel.controlCost, { viewModel.controlCost = it })
            }
            Column(Modifier.horizontalScroll(rememberScrollState())) {
                Row {
                    viewModel.controlSchedule.indices.forEach {
                        TableCell("n=$it", width = ControlScheduleCellWidth, background = Color(0xFFE2E8F0))
                    }
                }
                Row {
                    viewModel.controlSchedule.forEachIndexed { index, value ->
                        val buttonColor = if (value == 1) Color(0xFFCFE8D6) else Color(0xFFF2CACA)
                        val contentColor = if (value == 1) Color(0xFF1F5D35) else Color(0xFF7A2E2E)
                        Button(
                            onClick = { viewModel.toggleControlSchedule(index) },
                            enabled = index != 0,
                            modifier = Modifier.width(ControlScheduleCellWidth.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = buttonColor,
                                contentColor = contentColor
                            )
                        ) {
                            Text(if (value == 1) "вкл" else "выкл")
                        }
                    }
                }
            }
            Row(Modifier.padding(top = 8.dp)) {
                AppButton(
                    label = "Подобрать U",
                    onClick = viewModel::optimizeControlSchedule,
                    enabled = viewModel.controlEnabled && !viewModel.isCalculating && !viewModel.isOptimizingControl
                )
            }
        }

        Section("Регулирование R и атаки S") {
            Row {
                LabeledCheckbox("Включить R", viewModel.regulationEnabled) { viewModel.regulationEnabled = it }
                NumericField("R0, руб", viewModel.regulationCost, { viewModel.regulationCost = it })
                NumericField("Порог R", viewModel.disorderThreshold, { viewModel.disorderThreshold = it })
            }
            Row {
                LabeledCheckbox("Включить S", viewModel.attackEnabled) { viewModel.attackEnabled = it }
                NumericField("S0, руб", viewModel.attackCost, { viewModel.attackCost = it })
                NumericField("Порог S", viewModel.attackThreshold, { viewModel.attackThreshold = it })
            }
        }

        ActionRow {
            AppButton("Рассчитать", viewModel::calculate, enabled = !viewModel.isCalculating && !viewModel.isOptimizingControl)
        }
        viewModel.error?.let { Text(it, color = Color(0xFFDC2626), modifier = Modifier.padding(top = 8.dp)) }
    }
}

private fun StabilityModelType.label(): String {
    return when (this) {
        StabilityModelType.EXPONENTIAL -> "Экспоненциальная модель"
        StabilityModelType.NEAR_EXPONENTIAL -> "Почти экспоненциальная модель"
        StabilityModelType.AGING -> "Модель старения"
    }
}

private fun StabilityModelType.requiresA(): Boolean {
    return this == StabilityModelType.NEAR_EXPONENTIAL || this == StabilityModelType.AGING
}