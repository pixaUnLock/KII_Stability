package me.samol.kii.domain.calculator

import me.samol.kii.domain.model.AssetCalculationResult
import me.samol.kii.domain.model.AssetRelationType
import me.samol.kii.domain.model.InitialStabilityInput
import me.samol.kii.domain.model.InitialStabilityResult
import me.samol.kii.domain.model.SecurityAssetInput

/**
 * Calculates initial stability X0 from CIA risks and aggregation rules.
 *
 * @param input validated or user-entered initial stability input.
 */
class InitialStabilityCalculator(
    private val input: InitialStabilityInput
) {
    /**
     * Runs the full initial stability calculation.
     *
     * @return per-asset details and resulting X0.
     */
    fun calculate(): InitialStabilityResult {
        validateInput()
        val totalAssetWeight = input.assets.sumOf { it.assetWeight }
        val assetResults = input.assets.map { asset -> calculateAsset(asset, totalAssetWeight) }
        val x0 = aggregate(assetResults).coerceIn(0.0, 1.0)
        return InitialStabilityResult(assetResults = assetResults, x0 = x0, relationType = input.relationType)
    }

    /**
     * Calculates all intermediate values for one asset.
     *
     * @param asset input asset.
     * @param totalAssetWeight sum of all asset weights.
     * @return detailed asset calculation row.
     */
    private fun calculateAsset(asset: SecurityAssetInput, totalAssetWeight: Double): AssetCalculationResult {
        val ciaSum = asset.ciaWeights.confidentiality + asset.ciaWeights.integrity + asset.ciaWeights.availability
        val normalizedC = asset.ciaWeights.confidentiality / ciaSum
        val normalizedI = asset.ciaWeights.integrity / ciaSum
        val normalizedA = asset.ciaWeights.availability / ciaSum
        val profile = asset.threatProfile

        val riskC = normalizedC * profile.threatC * profile.vulnerabilityC
        val riskI = normalizedI * profile.threatI * profile.vulnerabilityI
        val riskA = normalizedA * profile.threatA * profile.vulnerabilityA
        val riskCI = riskC + riskI - riskC * riskI
        val riskCIA = (riskCI + riskA - riskCI * riskA).coerceIn(0.0, 1.0)
        val assetResilience = (1.0 - riskCIA).coerceIn(0.0, 1.0)

        return AssetCalculationResult(
            assetId = asset.id,
            assetName = asset.name,
            normalizedC = normalizedC,
            normalizedI = normalizedI,
            normalizedA = normalizedA,
            riskC = riskC,
            riskI = riskI,
            riskA = riskA,
            riskCIA = riskCIA,
            assetResilience = assetResilience,
            normalizedAssetWeight = asset.assetWeight / totalAssetWeight
        )
    }

    /**
     * Aggregates asset results according to the selected relation type.
     *
     * @param results calculated per-asset rows.
     * @return resulting initial stability X0.
     */
    private fun aggregate(results: List<AssetCalculationResult>): Double {
        return when (input.relationType) {
            AssetRelationType.INDEPENDENT -> aggregateIndependent(results)
            AssetRelationType.WEAK_DEPENDENCY -> aggregateWeakDependency(results)
            AssetRelationType.STRONG_DEPENDENCY -> aggregateStrongDependency(results)
            AssetRelationType.FEEDBACK -> aggregateFeedback(results)
        }
    }

    /**
     * Aggregates independent assets as a weighted sum.
     *
     * @param results calculated per-asset rows.
     * @return weighted X0.
     */
    private fun aggregateIndependent(results: List<AssetCalculationResult>): Double {
        return results.sumOf { it.normalizedAssetWeight * it.assetResilience }
    }

    /**
     * Aggregates weakly dependent assets by the recurrent pair formula.
     *
     * @param results calculated per-asset rows.
     * @return weak-dependency X0.
     */
    private fun aggregateWeakDependency(results: List<AssetCalculationResult>): Double {
        var aggregatedS = results.first().assetResilience
        var aggregatedW = results.first().normalizedAssetWeight
        for (index in 1 until results.size) {
            val s = results[index].assetResilience
            val w = results[index].normalizedAssetWeight
            val denominator = aggregatedW + w - aggregatedW * w
            aggregatedS = if (denominator == 0.0) {
                0.0
            } else {
                (aggregatedW * aggregatedS + w * s - aggregatedW * w * aggregatedS * s) / denominator
            }
            aggregatedW = denominator
        }
        return aggregatedS
    }

    /**
     * Aggregates strongly dependent assets by the strong-dependency formula.
     *
     * @param results calculated per-asset rows.
     * @return strong-dependency X0.
     */
    private fun aggregateStrongDependency(results: List<AssetCalculationResult>): Double {
        if (results.size == 1) return results.first().assetResilience

        val denominator = strongDenominator(results.map { it.normalizedAssetWeight })
        if (denominator == 0.0) return 0.0

        val weightedResiliencies = results.map { it.normalizedAssetWeight * it.assetResilience }
        return (strongNumerator(weightedResiliencies) / denominator).coerceIn(0.0, 1.0)
    }

    /**
     * Applies positive feedback to the selected base aggregation.
     *
     * @param results calculated per-asset rows.
     * @return feedback-adjusted X0.
     */
    private fun aggregateFeedback(results: List<AssetCalculationResult>): Double {
        val base = when (input.feedbackBaseRelationType) {
            AssetRelationType.INDEPENDENT -> aggregateIndependent(results)
            AssetRelationType.WEAK_DEPENDENCY -> aggregateWeakDependency(results)
            AssetRelationType.STRONG_DEPENDENCY -> aggregateStrongDependency(results)
            AssetRelationType.FEEDBACK -> error("Feedback cannot be used as a base relation type")
        }
        val denominator = 1.0 - input.feedbackCoefficient * (1.0 - base)
        return if (denominator == 0.0) {
            0.0
        } else {
            (base / denominator).coerceIn(0.0, 1.0)
        }
    }

    /**
     * Calculates the strong-dependency numerator:
     * sum of second-order products minus two third-order products, generalized to N assets.
     *
     * @param values weighted resiliencies wi * si.
     * @return numerator value.
     */
    private fun strongNumerator(values: List<Double>): Double {
        return elementaryCombinationSum(values, startOrder = 2)
    }

    /**
     * Calculates the strong-dependency denominator:
     * first-order weights minus second-order weights plus two third-order weights, generalized to N assets.
     *
     * @param weights normalized asset weights.
     * @return denominator value.
     */
    private fun strongDenominator(weights: List<Double>): Double {
        return weights.sum() - elementaryCombinationSum(weights, startOrder = 2)
    }

    /**
     * Generalized inclusion-exclusion sum for strong dependency.
     *
     * @param values values whose products are combined.
     * @param startOrder first combination order to include.
     * @return combined sum.
     */
    private fun elementaryCombinationSum(values: List<Double>, startOrder: Int): Double {
        var total = 0.0

        fun visit(startIndex: Int, order: Int, product: Double) {
            if (order >= startOrder) {
                val coefficient = order - 1
                val sign = if (order % 2 == 0) 1.0 else -1.0
                total += sign * coefficient * product
            }
            for (index in startIndex until values.size) {
                visit(index + 1, order + 1, product * values[index])
            }
        }

        visit(startIndex = 0, order = 0, product = 1.0)
        return total
    }

    /**
     * Validates all input values used by the formulas.
     *
     * @return Unit when input is valid.
     */
    private fun validateInput() {
        require(input.assets.isNotEmpty()) { "Нужен хотя бы один актив" }
        require(input.feedbackCoefficient in 0.0..1.0) { "Поле \"feedback\" должно быть в диапазоне [0; 1]" }
        require(input.feedbackBaseRelationType != AssetRelationType.FEEDBACK) { "Базовый тип связи для feedback не может быть обратной связью" }
        input.assets.forEach { asset ->
            require(asset.id.isNotBlank()) { "Поле \"id\" для актива \"${asset.name}\" не должно быть пустым" }
            require(asset.name.isNotBlank()) { "Поле \"name\" для актива \"${asset.id}\" не должно быть пустым" }
            requirePositiveAssetField(asset.name, "weight", asset.assetWeight)
            requirePositiveAssetField(asset.name, "C", asset.ciaWeights.confidentiality)
            requirePositiveAssetField(asset.name, "I", asset.ciaWeights.integrity)
            requirePositiveAssetField(asset.name, "A", asset.ciaWeights.availability)
            requireUnitRangeAssetField(asset.name, "tC", asset.threatProfile.threatC)
            requireUnitRangeAssetField(asset.name, "vC", asset.threatProfile.vulnerabilityC)
            requireUnitRangeAssetField(asset.name, "tI", asset.threatProfile.threatI)
            requireUnitRangeAssetField(asset.name, "vI", asset.threatProfile.vulnerabilityI)
            requireUnitRangeAssetField(asset.name, "tA", asset.threatProfile.threatA)
            requireUnitRangeAssetField(asset.name, "vA", asset.threatProfile.vulnerabilityA)
        }
    }

    /**
     * Validates a positive asset field.
     *
     * @param assetName visible asset name.
     * @param fieldName visible field name.
     * @param value field value.
     */
    private fun requirePositiveAssetField(assetName: String, fieldName: String, value: Double) {
        require(value.isFinite()) { "Поле \"$fieldName\" для актива \"$assetName\" должно быть конечным числом" }
        require(value > 0.0) { "Поле \"$fieldName\" для актива \"$assetName\" должно быть больше 0" }
    }

    /**
     * Validates an asset field in the [0; 1] range.
     *
     * @param assetName visible asset name.
     * @param fieldName visible field name.
     * @param value field value.
     */
    private fun requireUnitRangeAssetField(assetName: String, fieldName: String, value: Double) {
        require(value.isFinite()) { "Поле \"$fieldName\" для актива \"$assetName\" должно быть конечным числом" }
        require(value in 0.0..1.0) { "Поле \"$fieldName\" для актива \"$assetName\" должно быть в диапазоне [0; 1]" }
    }
}