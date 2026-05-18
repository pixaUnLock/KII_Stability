package me.samol.kii.domain.calculator

import me.samol.kii.domain.model.StabilityInput
import kotlin.math.exp

/**
 * Implements the near-exponential nonlinear stability model.
 *
 * @param input dynamic stability input.
 */
class NearExponentialStabilityCalculator(input: StabilityInput) : StabilityCalculator(input) {
    /**
     * Calculates the nonlinear baseline value.
     *
     * @param step current step number.
     * @param previousBaselineX previous baseline value.
     * @return baseline X for the step.
     */
    override fun calculateBaselineX(step: Int, previousBaselineX: Double): Double {
        val a = input.a ?: error("Параметр a обязателен для почти экспоненциальной модели")
        return previousBaselineX -
            previousBaselineX * previousBaselineX *
            (1.0 - a) *
            exp((step - 1).coerceAtLeast(0).toDouble() / input.t) /
            (input.x0 * input.t)
    }

    /**
     * Calculates Xn for the near-exponential model.
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
        val a = input.a ?: error("Параметр a обязателен для почти экспоненциальной модели")
        return previousX -
            previousX * previousX *
            (1.0 - a) *
            exp((step - 1).coerceAtLeast(0).toDouble() / input.t) /
            (input.x0 * input.t) +
            controlEffect(controlImpulse) +
            regulationEffect(regulationImpulse) -
            attackEffect(attackValue)
    }
}