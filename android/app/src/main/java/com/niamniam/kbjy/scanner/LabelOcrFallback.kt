package com.niamniam.kbjy.scanner

private val englishBasis = Regex("""\bper\s*(?:100\s*(?:ml|g)\b|serving\b)""", RegexOption.IGNORE_CASE)
private val russianBasis = Regex("""(?<![\p{L}\p{N}])на\s*(?:100\s*(?:мл|г)(?!\p{L})|порци\p{L}*)""", RegexOption.IGNORE_CASE)

internal fun parseLabelWithRetry(
    text: String,
    readEnglish: () -> String,
    readSmallerRussian: () -> String,
): ParsedLabel {
    val primary = LabelParser.parse(text)
    val primaryValues = primary.nutrients()
    if (primary.basis == null || primaryValues.none { it == null }) return primary
    val (retry, minimumOverlap) = when {
        englishBasis.containsMatchIn(text) -> LabelParser.parse(readEnglish()) to 1
        russianBasis.containsMatchIn(text) -> LabelParser.parse(readSmallerRussian(), allowRussianGramLookalike = true) to 2
        else -> return primary
    }
    if (retry.basis != primary.basis) return primary
    val overlap = primaryValues.zip(retry.nutrients()).filter { (first, second) -> first != null && second != null }
    if (overlap.size < minimumOverlap || overlap.any { (first, second) -> first != second }) return primary

    return primary.copy(
        calories = primary.calories ?: retry.calories,
        protein = primary.protein ?: retry.protein,
        fat = primary.fat ?: retry.fat,
        carbs = primary.carbs ?: retry.carbs,
    )
}

private fun ParsedLabel.nutrients() = listOf(calories, protein, fat, carbs)
