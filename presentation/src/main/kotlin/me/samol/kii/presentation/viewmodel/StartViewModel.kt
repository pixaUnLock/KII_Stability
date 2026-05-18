package me.samol.kii.presentation.viewmodel

/**
 * ViewModel for the start screen.
 *
 * @return exposes static screen metadata for UI rendering.
 */
class StartViewModel {
    /** Screen title shown in the first viewport. */
    val title: String = "Расчет устойчивости КИИ"

    /** Short description of the workflow. */
    val description: String =
        "Расчет X0 по активам, выбор динамической модели и анализ устойчивости по шагам."
}