package me.samol.kii.domain.model

/**
 * Dynamic stability model kind.
 *
 * @return enum value used by the stability calculator factory.
 */
enum class StabilityModelType {
    /** Exponential model. */
    EXPONENTIAL,

    /** Near-exponential model. */
    NEAR_EXPONENTIAL,

    /** Aging model. */
    AGING
}

/**
 * Summary status of a dynamic calculation.
 *
 * @return status that can be shown to the user.
 */
enum class StabilityStatus {
    /** Stability never fell below the critical threshold. */
    STABLE,

    /** At least one calculated step fell below the critical threshold. */
    CRITICAL
}

/**
 * Input model for a dynamic stability calculation.
 *
 * @property modelType selected dynamic model.
 * @property steps number of calculated steps.
 * @property dt time delta.
 * @property t model time constant.
 * @property x0 initial stability.
 * @property xCritical critical stability threshold.
 * @property a nonlinear coefficient for models 2 and 3.
 * @property controlEnabled whether control impulse U is enabled.
 * @property controlCost cost of one control action.
 * @property controlSchedule U schedule by step, where 1 enables impulse.
 * @property regulationEnabled whether regulation impulse R is enabled.
 * @property regulationCost cost of one regulation action.
 * @property disorderThreshold deviation threshold for R.
 * @property attackEnabled whether attack impulse S is enabled.
 * @property attackThreshold random threshold that starts an attack event.
 * @property attackCost attack cost and attack magnitude base.
 * @property kControl calculated scale coefficient for U effect.
 * @property kRegulation calculated scale coefficient for R effect.
 * @property kAttack calculated scale coefficient for S effect.
 * @property randomSeed seed used to generate attack events for one calculation.
 */
data class StabilityInput(
    val modelType: StabilityModelType,
    val steps: Int,
    val dt: Double,
    val t: Double,
    val x0: Double,
    val xCritical: Double,
    val a: Double? = null,
    val controlEnabled: Boolean,
    val controlCost: Double,
    val controlSchedule: List<Int>,
    val regulationEnabled: Boolean,
    val regulationCost: Double,
    val disorderThreshold: Double,
    val attackEnabled: Boolean,
    val attackThreshold: Double,
    val attackCost: Double,
    val kControl: Double = 0.000001,
    val kRegulation: Double = 0.000001,
    val kAttack: Double = 0.000001,
    val randomSeed: Long = System.nanoTime()
)

/**
 * One row of the dynamic model result table.
 *
 * @property step calculated step number.
 * @property x calculated stability Xn.
 * @property baselineX model baseline without active compensation.
 * @property deviation absolute deviation from baseline.
 * @property controlImpulse U impulse for the step.
 * @property regulationImpulse R impulse for the step.
 * @property randomValue generated value used for attack triggering.
 * @property attackEvent whether an attack happened on the step.
 * @property attackValue S attack value for the formula.
 */
data class StabilityStepResult(
    val step: Int,
    val x: Double,
    val baselineX: Double,
    val deviation: Double,
    val controlImpulse: Int,
    val regulationImpulse: Int,
    val randomValue: Double,
    val attackEvent: Boolean,
    val attackValue: Double
)

/**
 * Full result of a dynamic stability calculation.
 *
 * @property modelType selected model.
 * @property steps table rows by step.
 * @property spentControl total U cost.
 * @property spentRegulation total R cost.
 * @property spentDefense total defender cost.
 * @property spentAttacker total attacker cost.
 * @property xCritical critical threshold used in the chart.
 * @property status final status.
 */
data class StabilityResult(
    val modelType: StabilityModelType,
    val steps: List<StabilityStepResult>,
    val spentControl: Double,
    val spentRegulation: Double,
    val spentDefense: Double,
    val spentAttacker: Double,
    val xCritical: Double,
    val status: StabilityStatus
)