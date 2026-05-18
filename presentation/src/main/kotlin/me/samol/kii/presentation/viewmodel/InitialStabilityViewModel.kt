package me.samol.kii.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import me.samol.kii.data.DemoInitialStabilityProvider
import me.samol.kii.domain.model.AssetRelationType
import me.samol.kii.domain.model.CiaWeights
import me.samol.kii.domain.model.InitialStabilityInput
import me.samol.kii.domain.model.InitialStabilityResult
import me.samol.kii.domain.model.SecurityAssetInput
import me.samol.kii.domain.model.ThreatVulnerabilityProfile
import me.samol.kii.domain.usecase.CalculateInitialStabilityUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for initial X0 input and calculation.
 *
 * @param calculateUseCase use case that runs domain calculation.
 * @param demoProvider provider used to prefill editable demo rows.
 */
class InitialStabilityViewModel(
    private val calculateUseCase: CalculateInitialStabilityUseCase,
    private val demoProvider: DemoInitialStabilityProvider
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Editable asset rows shown in the table. */
    val assets = mutableStateListOf<EditableAssetState>()

    /** Selected relation type for asset aggregation. */
    var relationType: AssetRelationType by mutableStateOf(AssetRelationType.INDEPENDENT)

    /** Feedback coefficient used when FEEDBACK is selected. */
    var feedbackCoefficient: String by mutableStateOf("0.0")

    /** Base relation type used before feedback is applied. */
    var feedbackBaseRelationType: AssetRelationType by mutableStateOf(AssetRelationType.WEAK_DEPENDENCY)

    /** Latest calculation result. */
    var result: InitialStabilityResult? by mutableStateOf(null)
        private set

    /** Latest input that was actually submitted to the calculation use case. */
    var lastInput: InitialStabilityInput? by mutableStateOf(null)
        private set

    /** Latest validation or calculation error. */
    var error: String? by mutableStateOf(null)
        private set

    /** Whether calculation is running in a background coroutine. */
    var isCalculating: Boolean by mutableStateOf(false)
        private set

    init {
        loadDemo()
    }

    /**
     * Loads demo assets into editable UI state.
     *
     * @return Unit after replacing current rows.
     */
    fun loadDemo() {
        val input = demoProvider.create()
        assets.clear()
        assets.addAll(input.assets.map { EditableAssetState.fromDomain(it) })
        relationType = input.relationType
        feedbackCoefficient = input.feedbackCoefficient.toString()
        feedbackBaseRelationType = input.feedbackBaseRelationType
        result = null
        lastInput = null
        error = null
    }

    /**
     * Adds a new editable asset row.
     *
     * @return Unit after the row is appended.
     */
    fun addAsset() {
        assets += EditableAssetState(
            id = "asset${assets.size + 1}",
            name = "Новый актив",
            confidentiality = "0",
            integrity = "0",
            availability = "0",
            threatC = "0",
            vulnerabilityC = "0",
            threatI = "0",
            vulnerabilityI = "0",
            threatA = "0",
            vulnerabilityA = "0",
            assetWeight = "0"
        )
    }

    /**
     * Removes the last editable asset row.
     *
     * @return Unit after removal or no-op when one row remains.
     */
    fun removeLastAsset() {
        if (assets.size > 1) assets.removeAt(assets.lastIndex)
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
    }

    /**
     * Starts asynchronous X0 calculation.
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
                        error = throwable.message ?: "Ошибка расчета X0"
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
     * Converts editable fields to a domain input.
     *
     * @return domain input for the use case.
     */
    fun toInput(): InitialStabilityInput {
        return InitialStabilityInput(
            assets = assets.map { it.toDomain() },
            relationType = relationType,
            feedbackCoefficient = parseFeedbackCoefficient(),
            feedbackBaseRelationType = feedbackBaseRelationType
        )
    }

    /**
     * Parses the feedback coefficient shown outside the asset table.
     *
     * @return parsed coefficient.
     */
    private fun parseFeedbackCoefficient(): Double {
        return feedbackCoefficient.toDoubleOrNull()
            ?: throw IllegalArgumentException("Поле \"feedback\" должно быть числом")
    }
}

/**
 * Editable asset state for Compose text fields.
 *
 * @return mutable row model used only by the presentation layer.
 */
class EditableAssetState(
    id: String,
    name: String,
    confidentiality: String,
    integrity: String,
    availability: String,
    threatC: String,
    vulnerabilityC: String,
    threatI: String,
    vulnerabilityI: String,
    threatA: String,
    vulnerabilityA: String,
    assetWeight: String
) {
    var id: String by mutableStateOf(id)
    var name: String by mutableStateOf(name)
    var confidentiality: String by mutableStateOf(confidentiality)
    var integrity: String by mutableStateOf(integrity)
    var availability: String by mutableStateOf(availability)
    var threatC: String by mutableStateOf(threatC)
    var vulnerabilityC: String by mutableStateOf(vulnerabilityC)
    var threatI: String by mutableStateOf(threatI)
    var vulnerabilityI: String by mutableStateOf(vulnerabilityI)
    var threatA: String by mutableStateOf(threatA)
    var vulnerabilityA: String by mutableStateOf(vulnerabilityA)
    var assetWeight: String by mutableStateOf(assetWeight)

    /**
     * Converts editable state to a domain asset.
     *
     * @return domain asset input.
     */
    fun toDomain(): SecurityAssetInput {
        return SecurityAssetInput(
            id = id,
            name = name,
            ciaWeights = CiaWeights(
                parsePositive("C", confidentiality),
                parsePositive("I", integrity),
                parsePositive("A", availability)
            ),
            threatProfile = ThreatVulnerabilityProfile(
                parseUnitRange("tC", threatC),
                parseUnitRange("vC", vulnerabilityC),
                parseUnitRange("tI", threatI),
                parseUnitRange("vI", vulnerabilityI),
                parseUnitRange("tA", threatA),
                parseUnitRange("vA", vulnerabilityA)
            ),
            assetWeight = parsePositive("weight", assetWeight)
        )
    }

    /**
     * Parses a positive numeric asset field.
     *
     * @param fieldName visible field name.
     * @param value raw text value.
     * @return parsed number.
     */
    private fun parsePositive(fieldName: String, value: String): Double {
        val parsed = parseNumber(fieldName, value)
        if (parsed <= 0.0) {
            throw IllegalArgumentException(assetFieldError(fieldName, "должно быть больше 0"))
        }
        return parsed
    }

    /**
     * Parses a numeric asset field from the [0; 1] range.
     *
     * @param fieldName visible field name.
     * @param value raw text value.
     * @return parsed number.
     */
    private fun parseUnitRange(fieldName: String, value: String): Double {
        val parsed = parseNumber(fieldName, value)
        if (parsed !in 0.0..1.0) {
            throw IllegalArgumentException(assetFieldError(fieldName, "должно быть в диапазоне [0; 1]"))
        }
        return parsed
    }

    /**
     * Parses a finite numeric asset field.
     *
     * @param fieldName visible field name.
     * @param value raw text value.
     * @return parsed number.
     */
    private fun parseNumber(fieldName: String, value: String): Double {
        val parsed = value.toDoubleOrNull()
            ?: throw IllegalArgumentException(assetFieldError(fieldName, "должно быть числом"))
        if (!parsed.isFinite()) {
            throw IllegalArgumentException(assetFieldError(fieldName, "должно быть конечным числом"))
        }
        return parsed
    }

    /**
     * Formats a validation error for one asset field.
     *
     * @param fieldName visible field name.
     * @param message validation message.
     * @return formatted validation error.
     */
    private fun assetFieldError(fieldName: String, message: String): String {
        return "Поле \"$fieldName\" для актива \"$name\" $message"
    }

    companion object {
        /**
         * Creates editable state from a domain asset.
         *
         * @param asset domain asset input.
         * @return editable state populated from the asset.
         */
        fun fromDomain(asset: SecurityAssetInput): EditableAssetState {
            return EditableAssetState(
                id = asset.id,
                name = asset.name,
                confidentiality = asset.ciaWeights.confidentiality.toString(),
                integrity = asset.ciaWeights.integrity.toString(),
                availability = asset.ciaWeights.availability.toString(),
                threatC = asset.threatProfile.threatC.toString(),
                vulnerabilityC = asset.threatProfile.vulnerabilityC.toString(),
                threatI = asset.threatProfile.threatI.toString(),
                vulnerabilityI = asset.threatProfile.vulnerabilityI.toString(),
                threatA = asset.threatProfile.threatA.toString(),
                vulnerabilityA = asset.threatProfile.vulnerabilityA.toString(),
                assetWeight = asset.assetWeight.toString()
            )
        }
    }
}