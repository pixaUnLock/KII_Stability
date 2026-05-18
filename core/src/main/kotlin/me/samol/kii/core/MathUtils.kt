package me.samol.kii.core

import kotlin.math.round

/**
 * Rounds a numeric value for compact table and chart labels.
 *
 * @param digits number of digits after the decimal separator.
 * @return rounded value with the requested precision.
 */
fun Double.roundToDigits(digits: Int = 4): Double {
    val factor = List(digits) { 10.0 }.fold(1.0) { acc, value -> acc * value }
    return round(this * factor) / factor
}

/**
 * Validates that a number is finite and can safely participate in formulas.
 *
 * @param name human-readable field name for validation errors.
 * @return the validated numeric value.
 */
fun Double.requireFinite(name: String): Double {
    require(isFinite()) { "$name must be a finite number" }
    return this
}

/**
 * Validates that a number belongs to the [0; 1] range.
 *
 * @param name human-readable field name for validation errors.
 * @return the validated numeric value.
 */
fun Double.requireUnitRange(name: String): Double {
    requireFinite(name)
    require(this in 0.0..1.0) { "$name must be in range [0; 1]" }
    return this
}

/**
 * Validates that a number is greater than zero.
 *
 * @param name human-readable field name for validation errors.
 * @return the validated numeric value.
 */
fun Double.requirePositive(name: String): Double {
    requireFinite(name)
    require(this > 0.0) { "$name must be greater than 0" }
    return this
}