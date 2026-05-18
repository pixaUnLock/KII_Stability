package me.samol.kii.presentation.viewmodel

import me.samol.kii.domain.model.StabilityModelType

/**
 * ViewModel for the dynamic model selection screen.
 *
 * @return list of available model descriptors.
 */
class ModelSelectionViewModel {
    /**
     * Models available to the user.
     *
     * @return model labels paired with model types.
     */
    fun models(): List<Pair<StabilityModelType, String>> {
        return listOf(
            StabilityModelType.EXPONENTIAL to "Экспоненциальная модель",
            StabilityModelType.NEAR_EXPONENTIAL to "Почти экспоненциальная модель",
            StabilityModelType.AGING to "Модель старения"
        )
    }
}