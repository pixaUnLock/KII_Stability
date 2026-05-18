package me.samol.kii.presentation.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.samol.kii.domain.model.AssetRelationType
import me.samol.kii.presentation.components.ActionRow
import me.samol.kii.presentation.components.AppButton
import me.samol.kii.presentation.components.Section
import me.samol.kii.presentation.components.TableCell
import me.samol.kii.presentation.viewmodel.EditableAssetState
import me.samol.kii.presentation.viewmodel.InitialStabilityViewModel

private const val AssetTableCellWidth = 120
private const val AssetNameCellWidth = 340
private const val AssetHeaderCellHeight = 54
private const val RelationRowHeight = 64
private val FeedbackBaseRelationTypes = listOf(
    AssetRelationType.INDEPENDENT,
    AssetRelationType.WEAK_DEPENDENCY,
    AssetRelationType.STRONG_DEPENDENCY
)

/**
 * Renders the initial X0 input screen with an editable asset table.
 *
 * @param viewModel initial stability view model.
 * @param onBack back navigation callback.
 * @return rendered UI.
 */
@Composable
fun InitialStabilityInputScreen(viewModel: InitialStabilityViewModel, onBack: () -> Unit) {
    val vertical = rememberScrollState()
    Column(Modifier.fillMaxSize().verticalScroll(vertical).padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Исходные параметры активов", style = MaterialTheme.typography.h5)
            AppButton("Назад", onBack)
        }

        Section("Таблица активов") {
            Column(Modifier.horizontalScroll(rememberScrollState())) {
                AssetHeader()
                viewModel.assets.forEach { row -> AssetRow(row) }
            }
            ActionRow {
                AppButton("Добавить актив", viewModel::addAsset)
                AppButton("Удалить актив", viewModel::removeLastAsset, enabled = viewModel.assets.size > 1)
                AppButton("Демо-данные", viewModel::loadDemo)
            }
        }

        Section("Тип связи активов") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AssetRelationType.values().forEach { type ->
                    if (type == AssetRelationType.FEEDBACK) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.height(RelationRowHeight.dp)
                            ) {
                                RadioButton(
                                    selected = viewModel.relationType == type,
                                    onClick = { viewModel.relationType = type }
                                )
                                Text(type.russianLabel(), maxLines = 1)
                            }
                            if (viewModel.relationType == AssetRelationType.FEEDBACK) {
                                FeedbackSettingsPanel(viewModel)
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(RelationRowHeight.dp)
                        ) {
                            RadioButton(
                                selected = viewModel.relationType == type,
                                onClick = { viewModel.relationType = type }
                            )
                            Text(type.russianLabel(), maxLines = 1)
                        }
                    }
                }
            }
        }

        ActionRow {
            AppButton("Рассчитать устойчивость системы", viewModel::calculate, enabled = !viewModel.isCalculating)
        }
        viewModel.error?.let { Text(it, color = Color(0xFFDC2626), modifier = Modifier.padding(top = 8.dp)) }
        viewModel.result?.let { result ->
            Section("Результат X0") {
                Text("X0 = ${result.x0}", style = MaterialTheme.typography.h6)
                Column(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp)) {
                    Row {
                        TableCell("Актив", width = AssetNameCellWidth, background = Color(0xFFE2E8F0))
                        listOf("C_norm", "I_norm", "A_norm", "riskCIA", "Si", "wi").forEach { label ->
                            TableCell(label, width = AssetTableCellWidth, background = Color(0xFFE2E8F0))
                        }
                    }
                    result.assetResults.forEach {
                        Row {
                            TableCell(it.assetName, width = AssetNameCellWidth)
                            TableCell("%.4f".format(it.normalizedC), width = AssetTableCellWidth)
                            TableCell("%.4f".format(it.normalizedI), width = AssetTableCellWidth)
                            TableCell("%.4f".format(it.normalizedA), width = AssetTableCellWidth)
                            TableCell("%.4f".format(it.riskCIA), width = AssetTableCellWidth)
                            TableCell("%.4f".format(it.assetResilience), width = AssetTableCellWidth)
                            TableCell("%.4f".format(it.normalizedAssetWeight), width = AssetTableCellWidth)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedbackSettingsPanel(viewModel: InitialStabilityViewModel) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .padding(start = 44.dp, bottom = 8.dp)
            .border(1.dp, Color(0xFF94A3B8))
            .padding(12.dp)
    ) {
        Text("Базовый тип связи", style = MaterialTheme.typography.subtitle1)
        FeedbackBaseRelationTypes.forEach { type ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = viewModel.feedbackBaseRelationType == type,
                    onClick = { viewModel.feedbackBaseRelationType = type }
                )
                Text(type.russianLabel(), maxLines = 1)
            }
        }
        FeedbackField(viewModel.feedbackCoefficient, { viewModel.feedbackCoefficient = it })
    }
}

@Composable
private fun FeedbackField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text("feedback") },
        singleLine = true,
        modifier = Modifier.width(AssetTableCellWidth.dp)
    )
}

private fun AssetRelationType.russianLabel(): String {
    return when (this) {
        AssetRelationType.INDEPENDENT -> "Независимые активы"
        AssetRelationType.WEAK_DEPENDENCY -> "Слабая зависимость"
        AssetRelationType.STRONG_DEPENDENCY -> "Сильная зависимость"
        AssetRelationType.FEEDBACK -> "Обратная связь"
    }
}

@Composable
private fun AssetHeader() {
    Row {
        TableCell("id", width = AssetTableCellWidth, background = Color(0xFFE2E8F0), height = AssetHeaderCellHeight)
        TableCell("name", width = AssetNameCellWidth, background = Color(0xFFE2E8F0), height = AssetHeaderCellHeight)
        listOf(
            "C",
            "I",
            "A",
            "Tc (угроза конф.)",
            "Vc (уязвимость конф.)",
            "Ti (угроза цел.)",
            "Vi (уязвимость цел.)",
            "Ta (угроза дост.)",
            "Va (уязвимость дост.)",
            "weight (вес)"
        ).forEach { label ->
            TableCell(label, width = AssetTableCellWidth, background = Color(0xFFE2E8F0), height = AssetHeaderCellHeight)
        }
    }
}

@Composable
private fun AssetRow(row: EditableAssetState) {
    Row {
        TextInputCell(row.id, { row.id = it }, width = AssetTableCellWidth)
        TextInputCell(row.name, { row.name = it }, width = AssetNameCellWidth)
        TextInputCell(row.confidentiality, { row.confidentiality = it })
        TextInputCell(row.integrity, { row.integrity = it })
        TextInputCell(row.availability, { row.availability = it })
        TextInputCell(row.threatC, { row.threatC = it })
        TextInputCell(row.vulnerabilityC, { row.vulnerabilityC = it })
        TextInputCell(row.threatI, { row.threatI = it })
        TextInputCell(row.vulnerabilityI, { row.vulnerabilityI = it })
        TextInputCell(row.threatA, { row.threatA = it })
        TextInputCell(row.vulnerabilityA, { row.vulnerabilityA = it })
        TextInputCell(row.assetWeight, { row.assetWeight = it })
    }
}

@Composable
private fun TextInputCell(value: String, onChange: (String) -> Unit, width: Int = AssetTableCellWidth) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        modifier = Modifier.width(width.dp).padding(1.dp)
    )
}