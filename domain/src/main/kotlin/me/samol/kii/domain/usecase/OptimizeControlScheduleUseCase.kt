package me.samol.kii.domain.usecase

import me.samol.kii.domain.calculator.createCalculator
import me.samol.kii.domain.model.StabilityInput
import me.samol.kii.domain.model.StabilityResult

/**
 * Selects a binary U schedule that keeps Xn above the critical threshold with minimal defense cost.
 */
class OptimizeControlScheduleUseCase {
    /**
     * Optimizes U schedule for the provided input.
     *
     * @param input dynamic model input.
     * @return optimized input and its recalculated result.
     */
    operator fun invoke(input: StabilityInput): OptimizedControlSchedule {
        val baseInput = input.copy(controlEnabled = true)
        return if (baseInput.steps <= ExhaustiveStepLimit) {
            exhaustive(baseInput)
        } else {
            greedy(baseInput)
        }
    }

    private fun exhaustive(input: StabilityInput): OptimizedControlSchedule {
        val variants = 1 shl input.steps
        var best: OptimizedControlSchedule? = null

        for (mask in 0 until variants) {
            val schedule = MutableList(input.steps + 1) { 0 }
            for (step in 1..input.steps) {
                schedule[step] = if ((mask and (1 shl (step - 1))) != 0) 1 else 0
            }
            val candidate = evaluate(input.copy(controlSchedule = schedule))
            if (best == null || candidate.isBetterThan(best)) {
                best = candidate
            }
        }

        return requireNotNull(best)
    }

    private fun greedy(input: StabilityInput): OptimizedControlSchedule {
        var schedule = MutableList(input.steps + 1) { 0 }
        var best = evaluate(input.copy(controlSchedule = schedule))
        val availableSteps = (1..input.steps).toMutableSet()

        while (!best.result.isStableAboveThreshold() && availableSteps.isNotEmpty()) {
            val next = availableSteps
                .map { step ->
                    val candidateSchedule = schedule.toMutableList()
                    candidateSchedule[step] = 1
                    step to evaluate(input.copy(controlSchedule = candidateSchedule))
                }
                .minWithOrNull { left, right -> compareCandidates(left.second, right.second) }
                ?: break

            val selectedStep = next.first
            schedule = next.second.input.controlSchedule.toMutableList()
            availableSteps -= selectedStep
            best = next.second
        }

        return best
    }

    private fun evaluate(input: StabilityInput): OptimizedControlSchedule {
        return OptimizedControlSchedule(input, createCalculator(input).calculate())
    }

    private fun OptimizedControlSchedule.isBetterThan(other: OptimizedControlSchedule): Boolean {
        return compareCandidates(this, other) < 0
    }

    private fun compareCandidates(left: OptimizedControlSchedule, right: OptimizedControlSchedule): Int {
        val leftStable = left.result.isStableAboveThreshold()
        val rightStable = right.result.isStableAboveThreshold()
        if (leftStable != rightStable) return if (leftStable) -1 else 1

        if (leftStable && rightStable) {
            val costCompare = left.result.spentDefense.compareTo(right.result.spentDefense)
            if (costCompare != 0) return costCompare
        } else {
            val minCompare = right.result.minX().compareTo(left.result.minX())
            if (minCompare != 0) return minCompare
        }

        val controlCompare = left.result.spentControl.compareTo(right.result.spentControl)
        if (controlCompare != 0) return controlCompare

        return right.result.minX().compareTo(left.result.minX())
    }

    private fun StabilityResult.isStableAboveThreshold(): Boolean {
        return steps.all { it.x >= xCritical }
    }

    private fun StabilityResult.minX(): Double {
        return steps.minOfOrNull { it.x } ?: 0.0
    }

    private companion object {
        const val ExhaustiveStepLimit = 18
    }
}

/**
 * Optimized U schedule and recalculated dynamic result.
 *
 * @property input input with optimized U schedule.
 * @property result recalculated result for the optimized input.
 */
data class OptimizedControlSchedule(
    val input: StabilityInput,
    val result: StabilityResult
)