package me.samol.kii.data

import me.samol.kii.domain.model.AssetRelationType
import me.samol.kii.domain.model.CiaWeights
import me.samol.kii.domain.model.InitialStabilityInput
import me.samol.kii.domain.model.SecurityAssetInput
import me.samol.kii.domain.model.ThreatVulnerabilityProfile

/**
 * Provides demo input for the initial X0 calculation.
 *
 * @return demo input with three core IS assets.
 */
class DemoInitialStabilityProvider {
    /**
     * Creates demo assets and aggregation settings.
     *
     * @return ready-to-calculate initial stability input.
     */
    fun create(): InitialStabilityInput {
        return InitialStabilityInput(
            assets = listOf(
                SecurityAssetInput(
                    id = "db",
                    name = "Сервер базы данных",
                    ciaWeights = CiaWeights(5.0, 5.0, 4.0),
                    threatProfile = ThreatVulnerabilityProfile(0.60, 0.30, 0.70, 0.20, 0.80, 0.40),
                    assetWeight = 10.0
                ),
                SecurityAssetInput(
                    id = "app",
                    name = "Сервер приложений",
                    ciaWeights = CiaWeights(4.0, 4.0, 5.0),
                    threatProfile = ThreatVulnerabilityProfile(0.50, 0.30, 0.60, 0.40, 0.70, 0.50),
                    assetWeight = 8.0
                ),
                SecurityAssetInput(
                    id = "net",
                    name = "Сетевой шлюз",
                    ciaWeights = CiaWeights(3.0, 4.0, 5.0),
                    threatProfile = ThreatVulnerabilityProfile(0.40, 0.30, 0.50, 0.40, 0.90, 0.60),
                    assetWeight = 7.0
                )
            ),
            relationType = AssetRelationType.INDEPENDENT,
            feedbackCoefficient = 0.0,
            feedbackBaseRelationType = AssetRelationType.WEAK_DEPENDENCY
        )
    }
}