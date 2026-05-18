package me.samol.kii.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.samol.kii.presentation.components.AppButton
import me.samol.kii.presentation.viewmodel.StartViewModel

/**
 * Renders the start screen with mode selection.
 *
 * @param viewModel screen metadata provider.
 * @param onManual selected manual input flow callback.
 * @param onDemo selected demo flow callback.
 * @return rendered UI.
 */
@Composable
fun StartScreen(viewModel: StartViewModel, onManual: () -> Unit, onDemo: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(viewModel.title, style = MaterialTheme.typography.h4)
        Text(viewModel.description, style = MaterialTheme.typography.body1, modifier = Modifier.padding(top = 12.dp, bottom = 24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppButton("Ввести исходные параметры", onManual)
            AppButton("Демо-режим", onDemo)
        }
    }
}