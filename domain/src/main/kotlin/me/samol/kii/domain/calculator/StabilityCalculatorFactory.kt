package me.samol.kii.domain.calculator

import me.samol.kii.domain.model.StabilityInput
import me.samol.kii.domain.model.StabilityModelType

/**
 * Factory for dynamic stability calculators.
 *
 * @param input input that contains the selected model type.
 * @return calculator implementation matching the selected model.
 */
fun createCalculator(input: StabilityInput): StabilityCalculator {
    return when (input.modelType) {
        StabilityModelType.EXPONENTIAL -> ExponentialStabilityCalculator(input)
        StabilityModelType.NEAR_EXPONENTIAL -> NearExponentialStabilityCalculator(input)
        StabilityModelType.AGING -> AgingStabilityCalculator(input)
    }
}