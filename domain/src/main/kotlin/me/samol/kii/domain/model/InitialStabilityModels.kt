package me.samol.kii.domain.model

/**
 * Describes how assets influence each other during X0 aggregation.
 *
 * @return enum value used by the initial stability calculator.
 */
enum class AssetRelationType {
    /** Independent assets are aggregated as a weighted sum. */
    INDEPENDENT,

    /** Weakly dependent assets are aggregated by a recurrent pair formula. */
    WEAK_DEPENDENCY,

    /** Strongly dependent assets are aggregated by a pair/correlation formula. */
    STRONG_DEPENDENCY,

    /** Feedback transforms the selected base aggregate by a feedback formula. */
    FEEDBACK
}

/**
 * CIA importance weights for an asset.
 *
 * @property confidentiality confidentiality importance.
 * @property integrity integrity importance.
 * @property availability availability importance.
 */
data class CiaWeights(
    val confidentiality: Double,
    val integrity: Double,
    val availability: Double
)

/**
 * Threat and vulnerability profile of an asset.
 *
 * @property threatC confidentiality threat level in [0; 1].
 * @property vulnerabilityC confidentiality vulnerability level in [0; 1].
 * @property threatI integrity threat level in [0; 1].
 * @property vulnerabilityI integrity vulnerability level in [0; 1].
 * @property threatA availability threat level in [0; 1].
 * @property vulnerabilityA availability vulnerability level in [0; 1].
 */
data class ThreatVulnerabilityProfile(
    val threatC: Double,
    val vulnerabilityC: Double,
    val threatI: Double,
    val vulnerabilityI: Double,
    val threatA: Double,
    val vulnerabilityA: Double
)

/**
 * Input model for one information-system asset.
 *
 * @property id stable asset identifier.
 * @property name human-readable asset name.
 * @property ciaWeights CIA importance values.
 * @property threatProfile threats and vulnerabilities.
 * @property assetWeight asset weight in the whole system.
 */
data class SecurityAssetInput(
    val id: String,
    val name: String,
    val ciaWeights: CiaWeights,
    val threatProfile: ThreatVulnerabilityProfile,
    val assetWeight: Double
)

/**
 * Detailed calculation result for one asset.
 *
 * @property assetId stable asset identifier.
 * @property assetName human-readable asset name.
 * @property normalizedC normalized confidentiality weight.
 * @property normalizedI normalized integrity weight.
 * @property normalizedA normalized availability weight.
 * @property riskC confidentiality risk.
 * @property riskI integrity risk.
 * @property riskA availability risk.
 * @property riskCIA combined CIA risk.
 * @property assetResilience resulting asset resilience.
 * @property normalizedAssetWeight normalized asset weight.
 */
data class AssetCalculationResult(
    val assetId: String,
    val assetName: String,
    val normalizedC: Double,
    val normalizedI: Double,
    val normalizedA: Double,
    val riskC: Double,
    val riskI: Double,
    val riskA: Double,
    val riskCIA: Double,
    val assetResilience: Double,
    val normalizedAssetWeight: Double
)

/**
 * Input model for initial system stability calculation.
 *
 * @property assets assets participating in the calculation.
 * @property relationType selected aggregation relation type.
 * @property feedbackCoefficient feedback coefficient for FEEDBACK mode.
 * @property feedbackBaseRelationType base relation type used before feedback is applied.
 */
data class InitialStabilityInput(
    val assets: List<SecurityAssetInput>,
    val relationType: AssetRelationType,
    val feedbackCoefficient: Double = 0.0,
    val feedbackBaseRelationType: AssetRelationType = AssetRelationType.WEAK_DEPENDENCY
)

/**
 * Result of initial system stability calculation.
 *
 * @property assetResults detailed per-asset results.
 * @property x0 initial system stability passed to dynamic models.
 * @property relationType relation type used by the calculation.
 */
data class InitialStabilityResult(
    val assetResults: List<AssetCalculationResult>,
    val x0: Double,
    val relationType: AssetRelationType
)