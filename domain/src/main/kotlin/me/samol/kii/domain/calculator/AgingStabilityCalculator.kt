package me.samol.kii.domain.calculator

import me.samol.kii.domain.model.StabilityInput
import kotlin.math.exp

/**
 * Implements the aging stability model.
 *
 * @param input dynamic stability input.
 */
class AgingStabilityCalculator(input: StabilityInput) : StabilityCalculator(input) {
    /**
     * Calculates the aging baseline value.
     *
     * @param step current step number.
     * @param previousBaselineX previous baseline value.
     * @return baseline X for the step.
     */
    override fun calculateBaselineX(step: Int, previousBaselineX: Double): Double {
        val a = input.a ?: error("Параметр a обязателен для модели старения")
        return input.x0 / (a + (1.0 - a) * exp((step - 1).coerceAtLeast(0).toDouble() / input.t))
    }

    /**
     * Calculates Xn for the aging model.
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
        return baselineX +
            controlEffect(controlImpulse) +
            regulationEffect(regulationImpulse) -
            attackEffect(attackValue)
    }
}