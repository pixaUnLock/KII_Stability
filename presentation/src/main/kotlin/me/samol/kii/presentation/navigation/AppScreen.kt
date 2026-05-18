package me.samol.kii.presentation.navigation

/**
 * Simple navigation state for the Compose Desktop application.
 *
 * @return screen object used by the root app.
 */
sealed class AppScreen {
    /** Start mode selection screen. */
    object Start : AppScreen()

    /** Initial X0 input screen. */
    object InitialInput : AppScreen()

    /** Dynamic model selection screen. */
    object ModelSelection : AppScreen()

    /** Dynamic model parameter input screen. */
    object DynamicInput : AppScreen()

    /** Calculation result screen. */
    object Result : AppScreen()
}