package me.samol.kii.presentation.report

import me.samol.kii.domain.model.InitialStabilityInput
import me.samol.kii.domain.model.InitialStabilityResult
import me.samol.kii.domain.model.StabilityInput
import me.samol.kii.domain.model.StabilityResult

/**
 * Full report payload.
 *
 * @property initialInput asset table and relation settings.
 * @property initialResult calculated initial stability.
 * @property dynamicInput selected model and model parameters.
 * @property dynamicResult dynamic calculation result.
 */
data class StabilityReportData(
    val initialInput: InitialStabilityInput,
    val initialResult: InitialStabilityResult,
    val dynamicInput: StabilityInput,
    val dynamicResult: StabilityResult
)