package me.samol.kii.domain.calculator

import me.samol.kii.domain.model.StabilityInput
import me.samol.kii.domain.model.StabilityResult
import me.samol.kii.domain.model.StabilityStatus
import me.samol.kii.domain.model.StabilityStepResult
import kotlin.math.abs
import kotlin.random.Random

/**
 * Base class for dynamic stability calculators.
 *
 * @param input dynamic model input shared by all formulas.
 */
abstract class StabilityCalculator(
    protected val input: StabilityInput
) {
    protected val denominator: Double = 1.0 + input.dt / input.t

    /**
     * Calculates the baseline value for one step.
     *
     * @param step current step number, starting at 0.
     * @param previousBaselineX previous baseline value.
     * @return calculated baseline value.
     */
    abstract fun calculateBaselineX(step: Int, previousBaselineX: Double): Double

    /**
     * Calculates current stability for one step.
     *
     * @param step current step number, starting at 0.
     * @param previousX previous calculated stability.
     * @param baselineX current baseline value.
     * @param controlImpulse U impulse.
     * @param regulationImpulse R impulse.
     * @param attackValue S attack value.
     * @return current stability Xn.
     */
    abstract fun calculateCurrentX(
        step: Int,
        previousX: Double,
        baselineX: Double,
        controlImpulse: Int,
        regulationImpulse: Int,
        attackValue: Double
    ): Double

    /**
     * Runs dynamic stability calculation for all configured steps.
     *
     * @return table rows, costs, threshold, and final status.
     */
    fun calculate(): StabilityResult {
        validateInput()
        val random = Random(input.randomSeed)
        val rows = mutableListOf<StabilityStepResult>()
        var previousX = input.x0
        var previousBaseline = input.x0

        rows += StabilityStepResult(
            step = 0,
            x = input.x0,
            baselineX = input.x0,
            deviation = 0.0,
            controlImpulse = 0,
            regulationImpulse = 0,
            randomValue = 0.0,
            attackEvent = false,
            attackValue = 0.0
        )

        for (step in 1..input.steps) {
            val baselineX = calculateBaselineX(step, previousBaseline).coerceIn(0.0, 1.0)
            val controlImpulse = controlImpulseAt(step)
            val randomValue = random.nextDouble()
            val attackEvent = input.attackEnabled && randomValue >= input.attackThreshold
            val attackValue = if (attackEvent) input.attackCost else 0.0
            val previousRow = rows.last()
            val regulationImpulse = if (
                input.regulationEnabled &&
                previousRow.x < previousRow.baselineX &&
                previousRow.deviation >= input.disorderThreshold
            ) {
                1
            } else {
                0
            }
            val x = calculateCurrentX(
                step = step,
                previousX = previousX,
                baselineX = baselineX,
                controlImpulse = controlImpulse,
                regulationImpulse = regulationImpulse,
                attackValue = attackValue
            ).coerceIn(0.0, 1.0)

            rows += StabilityStepResult(
                step = step,
                x = x,
                baselineX = baselineX,
                deviation = abs(baselineX - x),
                controlImpulse = controlImpulse,
                regulationImpulse = regulationImpulse,
                randomValue = randomValue,
                attackEvent = attackEvent,
                attackValue = attackValue
            )
            previousX = x
            previousBaseline = baselineX
        }

        val spentControl = rows.sumOf { it.controlImpulse } * input.controlCost
        val spentRegulation = rows.sumOf { it.regulationImpulse } * input.regulationCost
        val spentAttacker = rows.count { it.attackEvent } * input.attackCost
        val status = if (rows.any { it.x < input.xCritical }) StabilityStatus.CRITICAL else StabilityStatus.STABLE

        return StabilityResult(
            modelType = input.modelType,
            steps = rows,
            spentControl = spentControl,
            spentRegulation = spentRegulation,
            spentDefense = spentControl + spentRegulation,
            spentAttacker = spentAttacker,
            xCritical = input.xCritical,
            status = status
        )
    }

    /**
     * Calculates the U contribution for a formula.
     *
     * @param controlImpulse U impulse.
     * @return additive control effect.
     */
    protected fun controlEffect(controlImpulse: Int): Double {
        return controlImpulse * calculatedCoefficient(input.controlCost) * input.dt * input.controlCost / denominator
    }

    /**
     * Calculates the R contribution for a formula.
     *
     * @param regulationImpulse R impulse.
     * @return additive regulation effect.
     */
    protected fun regulationEffect(regulationImpulse: Int): Double {
        return regulationImpulse * calculatedCoefficient(input.regulationCost) * input.dt * input.regulationCost / denominator
    }

    /**
     * Calculates the S contribution for a formula.
     *
     * @param attackValue attack value for the current step.
     * @return subtractive attack effect.
     */
    protected fun attackEffect(attackValue: Double): Double {
        return calculatedCoefficient(input.attackCost) * input.dt * attackValue / denominator
    }

    /**
     * Calculates an impulse coefficient from the model time constant and impulse cost.
     *
     * @param impulseCost base impulse cost.
     * @return coefficient for U, R, or S.
     */
    protected fun calculatedCoefficient(impulseCost: Double): Double {
        return if (impulseCost <= 0.0) 0.0 else 1.0 / (input.t * impulseCost + 0.00001)
    }

    /**
     * Reads control schedule for a step.
     *
     * @param step current step number, starting at 0.
     * @return 1 when U is enabled for this step; otherwise 0.
     */
    private fun controlImpulseAt(step: Int): Int {
        if (!input.controlEnabled) return 0
        return input.controlSchedule.getOrElse(step) { 0 }.coerceIn(0, 1)
    }

    /**
     * Validates input values that are common for every dynamic model.
     *
     * @return Unit when input is valid.
     */
    private fun validateInput() {
        require(input.steps > 0) { "Поле \"Количество шагов моделирования\" должно быть больше 0" }
        requirePositiveField("Dt", input.dt)
        requirePositiveField("T", input.t)
        requireUnitRangeField("X0", input.x0)
        requireUnitRangeField("Xкр", input.xCritical)
        if (input.controlEnabled) {
            requirePositiveField("U0, руб", input.controlCost)
        }
        if (input.regulationEnabled) {
            requireUnitRangeField("Порог R", input.disorderThreshold)
            requirePositiveField("R0, руб", input.regulationCost)
        }
        if (input.attackEnabled) {
            requireUnitRangeField("Порог S", input.attackThreshold)
            requirePositiveField("S0, руб", input.attackCost)
        }
        input.a?.let { requireUnitRangeField("a", it) }
    }

    /**
     * Validates a positive dynamic model field.
     *
     * @param fieldName visible field name.
     * @param value field value.
     */
    private fun requirePositiveField(fieldName: String, value: Double) {
        require(value.isFinite()) { "Поле \"$fieldName\" должно быть конечным числом" }
        require(value > 0.0) { "Поле \"$fieldName\" должно быть больше 0" }
    }

    /**
     * Validates a dynamic model field in the [0; 1] range.
     *
     * @param fieldName visible field name.
     * @param value field value.
     */
    private fun requireUnitRangeField(fieldName: String, value: Double) {
        require(value.isFinite()) { "Поле \"$fieldName\" должно быть конечным числом" }
        require(value in 0.0..1.0) { "Поле \"$fieldName\" должно быть в диапазоне [0; 1]" }
    }
}