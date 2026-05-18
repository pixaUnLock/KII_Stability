package me.samol.kii.domain.calculator

import me.samol.kii.domain.model.StabilityInput

/**
 * Implements the exponential stability model.
 *
 * @param input dynamic stability input.
 */
class ExponentialStabilityCalculator(input: StabilityInput) : StabilityCalculator(input) {
    /**
     * Calculates the exponential baseline value.
     *
     * @param step current step number.
     * @param previousBaselineX previous baseline value.
     * @return baseline X for the step.
     */
    override fun calculateBaselineX(step: Int, previousBaselineX: Double): Double {
        return previousBaselineX / denominator
    }

    /**
     * Calculates Xn for the exponential model.
     *
     * @param step current step number.
     * @param previousX previous calculated X.
     * @param baselineX current baseline X.
     * @param controlImpulse U impulse.
     * @param regulationImpulse R impulse.
     * @param attackValue S attack value.
     * @return current Xn.
     */
    override fun calculateCurrentX(
        step: Int,
        previousX: Double,
        baselineX: Double,
        controlImpulse: Int,
        regulationImpulse: Int,
        attackValue: Double
    ): Double {
        return previousX / denominator +
            controlEffect(controlImpulse) +
            regulationEffect(regulationImpulse) -
            attackEffect(attackValue)
    }
}