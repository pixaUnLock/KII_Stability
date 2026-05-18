package me.samol.kii.domain.usecase

import me.samol.kii.domain.calculator.InitialStabilityCalculator
import me.samol.kii.domain.model.InitialStabilityInput
import me.samol.kii.domain.model.InitialStabilityResult

/**
 * Use case that calculates initial stability X0.
 *
 * @return result with per-asset details and X0.
 */
class CalculateInitialStabilityUseCase {
    /**
     * Executes the initial stability calculation.
     *
     * @param input assets and aggregation options.
     * @return calculated initial stability result.
     */
    operator fun invoke(input: InitialStabilityInput): InitialStabilityResult {
        return InitialStabilityCalculator(input).calculate()
    }
}