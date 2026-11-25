package com.niamniam.kbjy

import java.util.Locale

internal fun formatDecimal(value: Double, english: Boolean = true): String {
    val locale = if (english) Locale.US else Locale.forLanguageTag("ru")
    val formatted = String.format(locale, "%.2f", value)
    return formatted.trimEnd('0').trimEnd('.', ',').let { if (it == "-0") "0" else it }
}
