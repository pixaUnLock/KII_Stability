package me.samol.kii.data

import me.samol.kii.domain.model.StabilityInput
import me.samol.kii.domain.model.StabilityModelType

/**
 * Provides demo inputs for the three dynamic models.
 *
 * @return selected dynamic input with default coefficients and schedules.
 */
class DemoDynamicModelProvider {
    /**
     * Creates demo input for the selected dynamic model.
     *
     * @param modelType selected model.
     * @param x0 initial stability to pass from the asset calculation.
     * @return ready-to-edit dynamic stability input.
     */
    fun create(modelType: StabilityModelType, x0: Double = 1.0): StabilityInput {
        return when (modelType) {
            StabilityModelType.EXPONENTIAL -> createExponentialInput(x0)
            StabilityModelType.NEAR_EXPONENTIAL -> createNearExponentialInput(x0)
            StabilityModelType.AGING -> createAgingInput(x0)
        }
    }

    /**
     * Creates demo input for the exponential model.
     *
     * @param x0 initial stability.
     * @return exponential model input.
     */
    fun createExponentialInput(x0: Double = 1.0): StabilityInput {
        return StabilityInput(
            modelType = StabilityModelType.EXPONENTIAL,
            steps = 13,
            dt = 1.0,
            t = 8.0,
            x0 = x0,
            xCritical = 0.4,
            a = null,
            controlEnabled = true,
            controlCost = 50_000.0,
            controlSchedule = listOf(0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0),
            regulationEnabled = true,
            regulationCost = 50_000.0,
            disorderThreshold = 0.05,
            attackEnabled = true,
            attackThreshold = 0.8,
            attackCost = 50_000.0
        )
    }

    /**
     * Creates demo input for the near-exponential model.
     *
     * @param x0 initial stability.
     * @return near-exponential model input.
     */
    fun createNearExponentialInput(x0: Double = 1.0): StabilityInput {
        return StabilityInput(
            modelType = StabilityModelType.NEAR_EXPONENTIAL,
            steps = 13,
            dt = 1.0,
            t = 6.0,
            x0 = x0,
            xCritical = 0.4,
            a = 0.6,
            controlEnabled = false,
            controlCost = 50_000.0,
            controlSchedule = listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1, 0),
            regulationEnabled = true,
            regulationCost = 50_000.0,
            disorderThreshold = 0.05,
            attackEnabled = false,
            attackThreshold = 0.8,
            attackCost = 50_000.0
        )
    }

    /**
     * Creates demo input for the aging model.
     *
     * @param x0 initial stability.
     * @return aging model input.
     */
    fun createAgingInput(x0: Double = 1.0): StabilityInput {
        return StabilityInput(
            modelType = StabilityModelType.AGING,
            steps = 13,
            dt = 1.0,
            t = 6.0,
            x0 = x0,
            xCritical = 0.4,
            a = 0.6,
            controlEnabled = true,
            controlCost = 50_000.0,
            controlSchedule = listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1),
            regulationEnabled = false,
            regulationCost = 50_000.0,
            disorderThreshold = 0.05,
            attackEnabled = false,
            attackThreshold = 0.8,
            attackCost = 50_000.0
        )
    }
}