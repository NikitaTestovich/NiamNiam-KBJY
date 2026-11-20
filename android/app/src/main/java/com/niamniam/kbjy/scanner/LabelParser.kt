package com.niamniam.kbjy.scanner

import java.util.Locale

enum class LabelBasis { PER_100_GRAMS, PER_100_MILLILITERS, PER_SERVING }

data class ParsedLabel(
    val basis: LabelBasis?,
    val calories: Double?,
    val protein: Double?,
    val fat: Double?,
    val carbs: Double?,
)

object LabelParser {
    fun parse(text: String): ParsedLabel {
        val normalized = text.lowercase(Locale.ROOT)
        val bases = buildSet {
            if (Regex("""(?:на|per)\s*100\s*(?:г|g)(?![а-яa-z])""").containsMatchIn(normalized)) add(LabelBasis.PER_100_GRAMS)
            if (Regex("""(?:на|per)\s*100\s*(?:мл|ml)(?![а-яa-z])""").containsMatchIn(normalized)) add(LabelBasis.PER_100_MILLILITERS)
            if (Regex("""(?:на\s+порци\w*|per\s+serving)""").containsMatchIn(normalized)) add(LabelBasis.PER_SERVING)
        }
        if (bases.size != 1) return ParsedLabel(null, null, null, null, null)

        val lines = normalized.lines()
        fun findValue(names: Regex, unit: Regex): Double? {
            val matching = lines.filter { names.containsMatchIn(it) }
            if (matching.size != 1) return null
            val values = unit.findAll(matching.single()).mapNotNull {
                it.groupValues[1].replace(',', '.').toDoubleOrNull()
            }.toList()
            return values.singleOrNull()?.takeIf { it.isFinite() && it >= 0 }
        }

        val grams = Regex("""(?<![\d.,-])(\d+(?:[.,]\d+)?)\s*(?:г|g)(?![а-яa-z])""")
        val kcal = Regex("""(?<![\d.,-])(\d+(?:[.,]\d+)?)\s*(?:ккал|kcal)(?![а-яa-z])""")
        return ParsedLabel(
            basis = bases.single(),
            calories = findValue(Regex("""энерг|калори|energy|calories"""), kcal),
            protein = findValue(Regex("""белк|protein"""), grams),
            fat = findValue(Regex("""жир|fat"""), grams),
            carbs = findValue(Regex("""углевод|carbohydrate|carbs"""), grams),
        )
    }
}
