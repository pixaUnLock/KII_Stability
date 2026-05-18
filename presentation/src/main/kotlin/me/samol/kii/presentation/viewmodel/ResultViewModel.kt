package me.samol.kii.presentation.viewmodel

import me.samol.kii.domain.model.StabilityResult
import me.samol.kii.domain.model.StabilityStatus

/**
 * ViewModel for result presentation helpers.
 *
 * @param result dynamic calculation result to summarize.
 */
class ResultViewModel(private val result: StabilityResult) {
    /**
     * Builds a human-readable status label.
     *
     * @return localized status label.
     */
    fun statusLabel(): String {
        return when (result.status) {
            StabilityStatus.STABLE -> "Устойчивость выше критического порога"
            StabilityStatus.CRITICAL -> "Есть шаги ниже критического порога"
        }
    }
}