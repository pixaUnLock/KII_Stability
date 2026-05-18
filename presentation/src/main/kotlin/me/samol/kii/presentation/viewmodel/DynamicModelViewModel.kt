package me.samol.kii.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import me.samol.kii.data.DemoDynamicModelProvider
import me.samol.kii.domain.model.StabilityInput
import me.samol.kii.domain.model.StabilityModelType
import me.samol.kii.domain.model.StabilityResult
import me.samol.kii.domain.usecase.CalculateDynamicStabilityUseCase
import me.samol.kii.domain.usecase.OptimizeControlScheduleUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for dynamic model parameter input and calculation.
 *
 * @param calculateUseCase use case that runs dynamic calculations.
 * @param demoProvider provider of demo inputs.
 */
class DynamicModelViewModel(
    private val calculateUseCase: CalculateDynamicStabilityUseCase,
    private val demoProvider: DemoDynamicModelProvider,
    private val optimizeControlScheduleUseCase: OptimizeControlScheduleUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Currently selected model type. */
    var modelType: StabilityModelType by mutableStateOf(StabilityModelType.EXPONENTIAL)
        private set

    private var stepsValue: String by mutableStateOf("13")

    /** Number of steps. */
    var steps: String
        get() = stepsValue
        set(value) {
            stepsValue = value
            syncControlScheduleLength(value.toIntOrNull())
        }

    /** Dt model parameter. */
    var dt: String by mutableStateOf("1.0")

    /** T model parameter. */
    var t: String by mutableStateOf("8.0")

    /** X0 model parameter. */
    var x0: String by mutableStateOf("1.0")

    /** Critical stability threshold. */
    var xCritical: String by mutableStateOf("0.4")

    /** Nonlinear coefficient for models 2 and 3. */
    var a: String by mutableStateOf("0.6")

    /** Whether U is enabled. */
    var controlEnabled: Boolean by mutableStateOf(true)

    /** Cost of U. */
    var controlCost: String by mutableStateOf("50000.0")

    /** U schedule by step. */
    val controlSchedule = mutableStateListOf<Int>()

    /** Whether R is enabled. */
    var regulationEnabled: Boolean by mutableStateOf(true)

    /** Cost of R. */
    var regulationCost: String by mutableStateOf("50000.0")

    /** Disorder threshold for R. */
    var disorderThreshold: String by mutableStateOf("0.05")

    /** Whether S attacks are enabled. */
    var attackEnabled: Boolean by mutableStateOf(true)

    /** Attack threshold. */
    var attackThreshold: String by mutableStateOf("0.8")

    /** Attack cost and base value. */
    var attackCost: String by mutableStateOf("50000.0")

    /** U coefficient shown in the coefficient block. */
    var kControl: String by mutableStateOf("0.000001")

    /** R coefficient shown in the coefficient block. */
    var kRegulation: String by mutableStateOf("0.000001")

    /** S coefficient shown in the coefficient block. */
    var kAttack: String by mutableStateOf("0.000001")

    /** Latest dynamic calculation result. */
    var result: StabilityResult? by mutableStateOf(null)
        private set

    /** Latest input that was actually submitted to the calculation use case. */
    var lastInput: StabilityInput? by mutableStateOf(null)
        private set

    /** Latest validation or calculation error. */
    var error: String? by mutableStateOf(null)
        private set

    /** Whether calculation is running in the background. */
    var isCalculating: Boolean by mutableStateOf(false)
        private set

    /** Whether U schedule optimization is running. */
    var isOptimizingControl: Boolean by mutableStateOf(false)
        private set

    init {
        loadDemo(StabilityModelType.EXPONENTIAL, 1.0)
    }

    /**
     * Loads demo defaults for a selected model and X0.
     *
     * @param type selected model type.
     * @param initialX0 X0 passed from initial calculation or demo input.
     * @return Unit after replacing editable state.
     */
    fun loadDemo(type: StabilityModelType, initialX0: Double) {
        applyInput(demoProvider.create(type, initialX0))
        result = null
        lastInput = null
        error = null
    }

    /**
     * Loads blank defaults for manual entry while keeping known previous-step values.
     *
     * @param type selected model type.
     * @param initialX0 X0 passed from initial calculation.
     * @return Unit after replacing editable state.
     */
    fun loadBlank(type: StabilityModelType, initialX0: Double) {
        modelType = type
        stepsValue = "0"
        dt = "0"
        t = "0"
        x0 = initialX0.toString()
        xCritical = "0"
        a = "0"
        controlEnabled = false
        controlCost = "0"
        controlSchedule.clear()
        regulationEnabled = false
        regulationCost = "0"
        disorderThreshold = "0"
        attackEnabled = false
        attackThreshold = "0"
        attackCost = "0"
        kControl = "0"
        kRegulation = "0"
        kAttack = "0"
        result = null
        lastInput = null
        error = null
    }

    /**
     * Toggles a U schedule cell.
     *
     * @param index zero-based schedule index.
     * @return Unit after changing 0 to 1 or 1 to 0.
     */
    fun toggleControlSchedule(index: Int) {
        if (index in controlSchedule.indices) {
            controlSchedule[index] = if (controlSchedule[index] == 0) 1 else 0
        }
    }

    /**
     * Clears the latest calculation result before returning to input editing.
     *
     * @return Unit after result reset.
     */
    fun clearResult() {
        result = null
        lastInput = null
        error = null
        isCalculating = false
        isOptimizingControl = false
    }

    /**
     * Starts asynchronous dynamic calculation.
     *
     * @return Unit immediately while calculation continues in the background.
     */
    fun calculate() {
        isCalculating = true
        error = null
        result = null
        scope.launch {
            runCatching {
                val input = toInput()
                input to calculateUseCase(input)
            }
                .onSuccess { (input, value) ->
                    withContext(Dispatchers.Main) {
                        lastInput = input
                        result = value
                        isCalculating = false
                    }
                }
                .onFailure { throwable ->
                    withContext(Dispatchers.Main) {
                        error = throwable.message ?: "Ошибка расчета динамики"
                        isCalculating = false
                    }
                }
        }
    }

    /**
     * Releases the internal coroutine scope.
     *
     * @return Unit after cancellation.
     */
    fun dispose() {
        scope.cancel()
    }

    /**
     * Converts editable state to domain input.
     *
     * @return dynamic stability input for the use case.
     */
    fun toInput(): StabilityInput {
        val parsedSteps = parsePositiveInt("Количество шагов моделирования", steps)
        val parsedT = parsePositive("T", t)
        val parsedControlCost = parseOptionalPositive("U0, руб", controlCost, controlEnabled)
        val parsedRegulationCost = parseOptionalPositive("R0, руб", regulationCost, regulationEnabled)
        val parsedAttackCost = parseOptionalPositive("S0, руб", attackCost, attackEnabled)
        val normalizedSchedule = List(parsedSteps + 1) { index -> controlSchedule.getOrElse(index) { 0 } }
        return StabilityInput(
            modelType = modelType,
            steps = parsedSteps,
            dt = parsePositive("Dt", dt),
            t = parsedT,
            x0 = parseUnitRange("X0", x0),
            xCritical = parseUnitRange("Xкр", xCritical),
            a = if (modelType == StabilityModelType.EXPONENTIAL) null else parseUnitRange("a", a),
            controlEnabled = controlEnabled,
            controlCost = parsedControlCost,
            controlSchedule = normalizedSchedule,
            regulationEnabled = regulationEnabled,
            regulationCost = parsedRegulationCost,
            disorderThreshold = parseOptionalUnitRange("Порог R", disorderThreshold, regulationEnabled),
            attackEnabled = attackEnabled,
            attackThreshold = parseOptionalUnitRange("Порог S", attackThreshold, attackEnabled),
            attackCost = parsedAttackCost,
            kControl = automaticCoefficient(parsedT, parsedControlCost),
            kRegulation = automaticCoefficient(parsedT, parsedRegulationCost),
            kAttack = automaticCoefficient(parsedT, parsedAttackCost)
        )
    }

    /**
     * Optimizes U schedule and recalculates the dynamic model.
     *
     * @return Unit while optimization continues in the background.
     */
    fun optimizeControlSchedule() {
        isOptimizingControl = true
        error = null
        result = null
        scope.launch {
            runCatching {
                optimizeControlScheduleUseCase(toInput().copy(controlEnabled = true))
            }
                .onSuccess { optimized ->
                    withContext(Dispatchers.Main) {
                        applyInput(optimized.input)
                        controlEnabled = true
                        lastInput = optimized.input
                        result = optimized.result
                        isOptimizingControl = false
                    }
                }
                .onFailure { throwable ->
                    withContext(Dispatchers.Main) {
                        error = throwable.message ?: "Не удалось подобрать управление U"
                        isOptimizingControl = false
                    }
                }
        }
    }

    /**
     * Calculates U/R/S coefficient from T and impulse cost.
     *
     * @param parsedT model time constant.
     * @param cost impulse cost.
     * @return calculated coefficient.
     */
    private fun automaticCoefficient(parsedT: Double, cost: Double): Double {
        return if (cost <= 0.0) 0.0 else 1.0 / (parsedT * cost + 0.00001)
    }

    /**
     * Parses a positive integer field.
     *
     * @param fieldName visible field name.
     * @param value raw field value.
     * @return parsed positive integer.
     */
    private fun parsePositiveInt(fieldName: String, value: String): Int {
        val parsed = value.toIntOrNull()
            ?: throw IllegalArgumentException("Поле \"$fieldName\" должно быть целым числом")
        if (parsed <= 0) {
            throw IllegalArgumentException("Поле \"$fieldName\" должно быть больше 0")
        }
        return parsed
    }

    /**
     * Parses a positive numeric field.
     *
     * @param fieldName visible field name.
     * @param value raw field value.
     * @return parsed positive number.
     */
    private fun parsePositive(fieldName: String, value: String): Double {
        val parsed = parseNumber(fieldName, value)
        if (parsed <= 0.0) {
            throw IllegalArgumentException("Поле \"$fieldName\" должно быть больше 0")
        }
        return parsed
    }

    /**
     * Parses a positive numeric field only when the owning block is enabled.
     *
     * @param fieldName visible field name.
     * @param value raw field value.
     * @param enabled whether validation should be strict.
     * @return parsed number or 0 for disabled blank/zero field.
     */
    private fun parseOptionalPositive(fieldName: String, value: String, enabled: Boolean): Double {
        if (!enabled && value.isBlank()) return 0.0
        val parsed = parseNumber(fieldName, value)
        if (enabled && parsed <= 0.0) {
            throw IllegalArgumentException("Поле \"$fieldName\" должно быть больше 0")
        }
        return parsed
    }

    /**
     * Parses a numeric field from the [0; 1] range.
     *
     * @param fieldName visible field name.
     * @param value raw field value.
     * @return parsed number.
     */
    private fun parseUnitRange(fieldName: String, value: String): Double {
        val parsed = parseNumber(fieldName, value)
        if (parsed !in 0.0..1.0) {
            throw IllegalArgumentException("Поле \"$fieldName\" должно быть в диапазоне [0; 1]")
        }
        return parsed
    }

    /**
     * Parses a unit-range numeric field only when the owning block is enabled.
     *
     * @param fieldName visible field name.
     * @param value raw field value.
     * @param enabled whether validation should be strict.
     * @return parsed number or 0 for disabled blank/zero field.
     */
    private fun parseOptionalUnitRange(fieldName: String, value: String, enabled: Boolean): Double {
        if (!enabled && value.isBlank()) return 0.0
        val parsed = parseNumber(fieldName, value)
        if (enabled && parsed !in 0.0..1.0) {
            throw IllegalArgumentException("Поле \"$fieldName\" должно быть в диапазоне [0; 1]")
        }
        return parsed
    }

    /**
     * Parses a finite numeric field.
     *
     * @param fieldName visible field name.
     * @param value raw field value.
     * @return parsed number.
     */
    private fun parseNumber(fieldName: String, value: String): Double {
        val parsed = value.toDoubleOrNull()
            ?: throw IllegalArgumentException("Поле \"$fieldName\" должно быть числом")
        if (!parsed.isFinite()) {
            throw IllegalArgumentException("Поле \"$fieldName\" должно быть конечным числом")
        }
        return parsed
    }

    /**
     * Applies a domain input to editable state.
     *
     * @param input domain input.
     * @return Unit after state replacement.
     */
    private fun applyInput(input: StabilityInput) {
        modelType = input.modelType
        stepsValue = input.steps.toString()
        dt = input.dt.toString()
        t = input.t.toString()
        x0 = input.x0.toString()
        xCritical = input.xCritical.toString()
        a = (input.a ?: 0.6).toString()
        controlEnabled = input.controlEnabled
        controlCost = input.controlCost.toString()
        controlSchedule.clear()
        controlSchedule.addAll(input.controlSchedule)
        syncControlScheduleLength(input.steps)
        regulationEnabled = input.regulationEnabled
        regulationCost = input.regulationCost.toString()
        disorderThreshold = input.disorderThreshold.toString()
        attackEnabled = input.attackEnabled
        attackThreshold = input.attackThreshold.toString()
        attackCost = input.attackCost.toString()
        kControl = input.kControl.toString()
        kRegulation = input.kRegulation.toString()
        kAttack = input.kAttack.toString()
    }

    /**
     * Resizes the U schedule to match the current step count.
     *
     * @param stepCount requested step count, or null when input is not parseable yet.
     * @return Unit after schedule normalization.
     */
    private fun syncControlScheduleLength(stepCount: Int?) {
        if (stepCount == null || stepCount < 0) return
        val scheduleSize = stepCount + 1
        while (controlSchedule.size < scheduleSize) {
            controlSchedule += 0
        }
        while (controlSchedule.size > scheduleSize) {
            controlSchedule.removeAt(controlSchedule.lastIndex)
        }
    }
}