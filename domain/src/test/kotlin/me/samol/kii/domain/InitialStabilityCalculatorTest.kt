package me.samol.kii.domain

import me.samol.kii.domain.calculator.InitialStabilityCalculator
import me.samol.kii.domain.model.AssetRelationType
import me.samol.kii.domain.model.CiaWeights
import me.samol.kii.domain.model.InitialStabilityInput
import me.samol.kii.domain.model.SecurityAssetInput
import me.samol.kii.domain.model.ThreatVulnerabilityProfile
import kotlin.test.Test
import kotlin.test.assertEquals

class InitialStabilityCalculatorTest {
    @Test
    fun `normalizes cia weights and calculates risk cia`() {
        val result = InitialStabilityCalculator(input(listOf(asset("a", 10.0)), AssetRelationType.INDEPENDENT)).calculate()
        val row = result.assetResults.single()

        assertEquals(0.5, row.normalizedC, 1e-9)
        assertEquals(0.3, row.normalizedI, 1e-9)
        assertEquals(0.2, row.normalizedA, 1e-9)
        assertEquals(0.1798336, row.riskCIA, 1e-8)
    }

    @Test
    fun `calculates asset resilience`() {
        val result = InitialStabilityCalculator(input(listOf(asset("a", 10.0)), AssetRelationType.INDEPENDENT)).calculate()
        val row = result.assetResults.single()

        assertEquals(0.8201664, row.assetResilience, 1e-8)
    }

    @Test
    fun `aggregates independent assets`() {
        val result = InitialStabilityCalculator(
            input(listOf(asset("a", 1.0), asset("b", 3.0)), AssetRelationType.INDEPENDENT)
        ).calculate()

        assertEquals(0.8201664, result.x0, 1e-8)
    }

    @Test
    fun `aggregates weak dependency assets`() {
        val result = InitialStabilityCalculator(
            input(listOf(asset("a", 1.0), asset("b", 3.0)), AssetRelationType.WEAK_DEPENDENCY)
        ).calculate()

        assertEquals(0.8542033560717783, result.x0, 1e-8)
    }

    @Test
    fun `aggregates strong dependency assets`() {
        val result = InitialStabilityCalculator(
            input(listOf(asset("a", 1.0), asset("b", 3.0)), AssetRelationType.STRONG_DEPENDENCY)
        ).calculate()

        assertEquals(0.15523221315899077, result.x0, 1e-8)
    }

    @Test
    fun `applies feedback formula`() {
        val result = InitialStabilityCalculator(
            InitialStabilityInput(
                assets = listOf(asset("a", 1.0), asset("b", 3.0)),
                relationType = AssetRelationType.FEEDBACK,
                feedbackCoefficient = 0.5,
                feedbackBaseRelationType = AssetRelationType.WEAK_DEPENDENCY
            )
        ).calculate()

        assertEquals(0.9213696580524484, result.x0, 1e-8)
    }

    @Test
    fun `applies feedback to selected base relation type`() {
        val result = InitialStabilityCalculator(
            InitialStabilityInput(
                assets = listOf(asset("a", 1.0), asset("b", 3.0)),
                relationType = AssetRelationType.FEEDBACK,
                feedbackCoefficient = 0.5,
                feedbackBaseRelationType = AssetRelationType.INDEPENDENT
            )
        ).calculate()

        assertEquals(0.9011993628714385, result.x0, 1e-8)
    }

    private fun input(assets: List<SecurityAssetInput>, relationType: AssetRelationType): InitialStabilityInput {
        return InitialStabilityInput(assets, relationType)
    }

    private fun asset(id: String, weight: Double): SecurityAssetInput {
        return SecurityAssetInput(
            id = id,
            name = id,
            ciaWeights = CiaWeights(5.0, 3.0, 2.0),
            threatProfile = ThreatVulnerabilityProfile(0.4, 0.5, 0.3, 0.2, 0.9, 0.4),
            assetWeight = weight
        )
    }
}