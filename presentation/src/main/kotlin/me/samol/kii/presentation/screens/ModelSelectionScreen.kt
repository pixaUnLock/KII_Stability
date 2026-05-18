package me.samol.kii.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.samol.kii.domain.model.StabilityModelType
import me.samol.kii.presentation.components.AppButton
import me.samol.kii.presentation.viewmodel.ModelSelectionViewModel

/**
 * Renders dynamic model selection cards.
 *
 * @param viewModel model list provider.
 * @param x0 current initial stability displayed to the user.
 * @param onBack back navigation callback.
 * @param onSelect selected model callback.
 * @return rendered UI.
 */
@Composable
fun ModelSelectionScreen(
    viewModel: ModelSelectionViewModel,
    x0: Double,
    onBack: () -> Unit,
    onSelect: (StabilityModelType) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Выбор модели динамики", style = MaterialTheme.typography.h5)
            AppButton("Назад", onBack)
        }
        Text("Текущий X0 = $x0", style = MaterialTheme.typography.body1)
        viewModel.models().forEach { (type, label) ->
            Card(
                border = BorderStroke(1.dp, Color(0xFFCAD3DF)),
                elevation = 0.dp,
                modifier = Modifier.fillMaxWidth().clickable { onSelect(type) }
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(label, style = MaterialTheme.typography.h6)
                    Text("Открыть параметры модели и расписание воздействий", style = MaterialTheme.typography.body2)
                }
            }
        }
    }
}