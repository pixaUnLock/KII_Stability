package me.samol.kii.domain.usecase

import me.samol.kii.domain.calculator.createCalculator
import me.samol.kii.domain.model.StabilityInput
import me.samol.kii.domain.model.StabilityResult

/**
 * Use case that calculates dynamic stability with the selected model.
 *
 * @return result table, costs, and final status.
 */
class CalculateDynamicStabilityUseCase {
    /**
     * Executes the dynamic stability calculation.
     *
     * @param input model parameters.
     * @return calculated dynamic stability result.
     */
    operator fun invoke(input: StabilityInput): StabilityResult {
        return createCalculator(input).calculate()
    }
}