package me.samol.kii.app

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import me.samol.kii.data.DemoDynamicModelProvider
import me.samol.kii.data.DemoInitialStabilityProvider
import me.samol.kii.domain.model.InitialStabilityInput
import me.samol.kii.domain.model.InitialStabilityResult
import me.samol.kii.domain.model.StabilityInput
import me.samol.kii.domain.model.StabilityModelType
import me.samol.kii.domain.model.StabilityResult
import me.samol.kii.domain.usecase.CalculateDynamicStabilityUseCase
import me.samol.kii.domain.usecase.CalculateInitialStabilityUseCase
import me.samol.kii.domain.usecase.OptimizeControlScheduleUseCase
import me.samol.kii.presentation.navigation.AppScreen
import me.samol.kii.presentation.report.StabilityReportData
import me.samol.kii.presentation.screens.CalculationResultScreen
import me.samol.kii.presentation.screens.DynamicModelInputScreen
import me.samol.kii.presentation.screens.InitialStabilityInputScreen
import me.samol.kii.presentation.screens.ModelSelectionScreen
import me.samol.kii.presentation.screens.StartScreen
import me.samol.kii.presentation.viewmodel.DynamicModelViewModel
import me.samol.kii.presentation.viewmodel.InitialStabilityViewModel
import me.samol.kii.presentation.viewmodel.ModelSelectionViewModel
import me.samol.kii.presentation.viewmodel.StartViewModel

private const val DemoX0 = 1.0

/**
 * Desktop application entry point.
 *
 * @return starts the Compose Desktop event loop.
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "KII Stability",
        state = WindowState(placement = WindowPlacement.Maximized)
    ) {
        MaterialTheme {
            App()
        }
    }
}

/**
 * Root application composable with lightweight dependency wiring and navigation state.
 *
 * @return rendered application flow.
 */
@Composable
fun App() {
    val initialViewModel = remember {
        InitialStabilityViewModel(CalculateInitialStabilityUseCase(), DemoInitialStabilityProvider())
    }
    val dynamicViewModel = remember {
        DynamicModelViewModel(CalculateDynamicStabilityUseCase(), DemoDynamicModelProvider(), OptimizeControlScheduleUseCase())
    }
    val startViewModel = remember { StartViewModel() }
    val modelSelectionViewModel = remember { ModelSelectionViewModel() }

    var screen: AppScreen by remember { mutableStateOf(AppScreen.Start) }
    var isDemoFlow by remember { mutableStateOf(false) }
    var initialResult: InitialStabilityResult? by remember { mutableStateOf(null) }
    var dynamicResult: StabilityResult? by remember { mutableStateOf(null) }
    var initialInputForReport: InitialStabilityInput? by remember { mutableStateOf(null) }
    var dynamicInputForReport: StabilityInput? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        onDispose {
            initialViewModel.dispose()
            dynamicViewModel.dispose()
        }
    }

    LaunchedEffect(initialViewModel.result) {
        val value = initialViewModel.result
        if (value != null && screen == AppScreen.InitialInput) {
            initialResult = value
            initialInputForReport = initialViewModel.lastInput
            screen = AppScreen.ModelSelection
        }
    }

    LaunchedEffect(dynamicViewModel.result) {
        val value = dynamicViewModel.result
        if (value != null && screen == AppScreen.DynamicInput) {
            dynamicResult = value
            dynamicInputForReport = dynamicViewModel.lastInput
            screen = AppScreen.Result
        }
    }

    when (screen) {
        AppScreen.Start -> StartScreen(
            viewModel = startViewModel,
            onManual = {
                isDemoFlow = false
                initialResult = null
                dynamicResult = null
                initialInputForReport = null
                dynamicInputForReport = null
                initialViewModel.loadDemo()
                screen = AppScreen.InitialInput
            },
            onDemo = {
                isDemoFlow = true
                val input = DemoInitialStabilityProvider().create()
                val result = CalculateInitialStabilityUseCase()(input)
                initialResult = result
                dynamicResult = null
                initialInputForReport = input
                dynamicInputForReport = null
                screen = AppScreen.ModelSelection
            }
        )

        AppScreen.InitialInput -> InitialStabilityInputScreen(
            viewModel = initialViewModel,
            onBack = { screen = AppScreen.Start }
        )

        AppScreen.ModelSelection -> ModelSelectionScreen(
            viewModel = modelSelectionViewModel,
            x0 = if (isDemoFlow) DemoX0 else initialResult?.x0 ?: 1.0,
            onBack = {
                if (isDemoFlow) {
                    screen = AppScreen.Start
                } else {
                    initialViewModel.clearResult()
                    initialResult = null
                    initialInputForReport = null
                    screen = AppScreen.InitialInput
                }
            },
            onSelect = { model: StabilityModelType ->
                dynamicResult = null
                dynamicInputForReport = null
                if (isDemoFlow) {
                    dynamicViewModel.loadDemo(model, DemoX0)
                } else {
                    dynamicViewModel.loadBlank(model, initialResult?.x0 ?: 1.0)
                }
                screen = AppScreen.DynamicInput
            }
        )

        AppScreen.DynamicInput -> DynamicModelInputScreen(
            viewModel = dynamicViewModel,
            onBack = { screen = AppScreen.ModelSelection }
        )

        AppScreen.Result -> {
            val result = dynamicResult
            if (result == null) {
                screen = AppScreen.DynamicInput
            } else {
                val reportData = initialInputForReport?.let { initialInput ->
                    initialResult?.let { initial ->
                        dynamicInputForReport?.let { dynamicInput ->
                            StabilityReportData(initialInput, initial, dynamicInput, result)
                        }
                    }
                }
                CalculationResultScreen(
                    result = result,
                    reportData = reportData,
                    onBack = {
                        dynamicViewModel.clearResult()
                        dynamicResult = null
                        screen = AppScreen.DynamicInput
                    },
                    onStart = { screen = AppScreen.Start }
                )
            }
        }
    }
}