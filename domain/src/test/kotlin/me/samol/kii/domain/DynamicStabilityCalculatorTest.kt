package me.samol.kii.domain

import me.samol.kii.domain.calculator.createCalculator
import me.samol.kii.domain.model.StabilityInput
import me.samol.kii.domain.model.StabilityModelType
import kotlin.math.exp
import kotlin.test.Test
import kotlin.test.assertEquals

class DynamicStabilityCalculatorTest {
    @Test
    fun `calculates exponential model`() {
        val input = baseInput(StabilityModelType.EXPONENTIAL, t = 8.0, a = null)
        val result = createCalculator(input).calculate()

        assertEquals(0, result.steps.first().step)
        assertEquals(1.0, result.steps.first().x, 1e-9)
        assertEquals(1, result.steps.last().step)
        assertEquals(1.0 / (1.0 + 1.0 / 8.0), result.steps.last().x, 1e-9)
    }

    @Test
    fun `calculates near exponential model`() {
        val input = baseInput(StabilityModelType.NEAR_EXPONENTIAL, t = 6.0, a = 0.6)
        val result = createCalculator(input).calculate()

        val expected = 1.0 - 1.0 * 1.0 * (1.0 - 0.6) * exp(0.0 / 6.0) / (1.0 * 6.0)
        assertEquals(1.0, result.steps.first().x, 1e-9)
        assertEquals(expected, result.steps.last().x, 1e-9)
    }

    @Test
    fun `calculates aging model`() {
        val input = baseInput(StabilityModelType.AGING, t = 6.0, a = 0.6)
        val result = createCalculator(input).calculate()

        val expected = 1.0 / (0.6 + (1.0 - 0.6) * exp(0.0 / 6.0))
        assertEquals(1.0, result.steps.first().x, 1e-9)
        assertEquals(expected, result.steps.last().x, 1e-9)
    }

    @Test
    fun `applies regulation from previous step deviation`() {
        val input = baseInput(StabilityModelType.EXPONENTIAL, t = 8.0, a = null).copy(
            steps = 2,
            attackEnabled = true,
            attackThreshold = 0.0,
            regulationEnabled = true,
            disorderThreshold = 0.05
        )
        val result = createCalculator(input).calculate()

        assertEquals(0, result.steps[1].regulationImpulse)
        assertEquals(1, result.steps[2].regulationImpulse)
    }

    private fun baseInput(modelType: StabilityModelType, t: Double, a: Double?): StabilityInput {
        return StabilityInput(
            modelType = modelType,
            steps = 1,
            dt = 1.0,
            t = t,
            x0 = 1.0,
            xCritical = 0.4,
            a = a,
            controlEnabled = false,
            controlCost = 50_000.0,
            controlSchedule = listOf(0),
            regulationEnabled = false,
            regulationCost = 50_000.0,
            disorderThreshold = 0.05,
            attackEnabled = false,
            attackThreshold = 0.8,
            attackCost = 50_000.0
        )
    }
}