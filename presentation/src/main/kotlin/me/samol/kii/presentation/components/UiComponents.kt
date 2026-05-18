package me.samol.kii.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Draws a compact bordered section with a title.
 *
 * @param title section title.
 * @param modifier Compose modifier.
 * @param content section content.
 * @return rendered UI.
 */
@Composable
fun Section(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(title, style = MaterialTheme.typography.h6, modifier = Modifier.padding(bottom = 6.dp))
        Card(
            border = BorderStroke(1.dp, Color(0xFFCAD3DF)),
            elevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.background(Color(0xFFF8FAFC)).padding(12.dp)) {
                content()
            }
        }
    }
}

/**
 * Draws a labeled numeric text field.
 *
 * @param label field label.
 * @param value field value.
 * @param onValueChange value change handler.
 * @param width fixed field width.
 * @return rendered UI.
 */
@Composable
fun NumericField(label: String, value: String, onValueChange: (String) -> Unit, width: Int = 120) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.width(width.dp).padding(end = 8.dp, bottom = 8.dp)
    )
}

/**
 * Draws a labeled checkbox row.
 *
 * @param label row label.
 * @param checked current checked state.
 * @param onCheckedChange change handler.
 * @return rendered UI.
 */
@Composable
fun LabeledCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label)
    }
}

/**
 * Draws a relation/model radio row.
 *
 * @param label radio label.
 * @param selected whether selected.
 * @param onClick click handler.
 * @return rendered UI.
 */
@Composable
fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label)
    }
}

/**
 * Draws a bordered table cell.
 *
 * @param text visible text.
 * @param width cell width.
 * @param background cell background color.
 * @param height optional fixed cell height.
 * @return rendered UI.
 */
@Composable
fun TableCell(text: String, width: Int = 120, background: Color = Color.White, height: Int? = null) {
    val baseModifier = Modifier
        .width(width.dp)
        .border(1.dp, Color(0xFFD8DEE9))
        .background(background)
    val sizedModifier = if (height == null) baseModifier else baseModifier.height(height.dp)

    Text(
        text = text,
        modifier = sizedModifier.padding(6.dp),
        style = MaterialTheme.typography.body2
    )
}

/**
 * Draws a primary action row with consistent spacing.
 *
 * @param content row content.
 * @return rendered UI.
 */
@Composable
fun ActionRow(content: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        content()
    }
}

/**
 * Draws a simple button used across screens.
 *
 * @param label button label.
 * @param onClick click handler.
 * @param enabled whether the button can be clicked.
 * @return rendered UI.
 */
@Composable
fun AppButton(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled) {
        Text(label)
    }
}