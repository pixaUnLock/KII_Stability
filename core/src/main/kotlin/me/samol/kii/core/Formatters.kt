package me.samol.kii.core

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val decimalFormat = DecimalFormat("0.####", DecimalFormatSymbols(Locale.US))
private val moneyFormat = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.US))

/**
 * Formats a double for predictable UI output.
 *
 * @param value numeric value to format.
 * @return compact decimal text.
 */
fun formatDecimal(value: Double): String = decimalFormat.format(value)

/**
 * Formats a monetary value for cost blocks.
 *
 * @param value monetary value to format.
 * @return compact money text with the ruble suffix.
 */
fun formatMoney(value: Double): String = "${moneyFormat.format(value)} руб."