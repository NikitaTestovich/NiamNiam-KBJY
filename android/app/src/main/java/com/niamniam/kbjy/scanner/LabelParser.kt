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
    private val basisPatterns = listOf(
        LabelBasis.PER_100_GRAMS to Regex("""(?:на|per)\s*100\s*(?:г|g)(?![а-яa-z])"""),
        LabelBasis.PER_100_MILLILITERS to Regex("""(?:на|per)\s*100\s*(?:мл|ml)(?![а-яa-z])"""),
        LabelBasis.PER_SERVING to Regex("""(?:на\s+порци\p{L}*|per\s+serving)"""),
    )
    private val columnGap = Regex(
        """[\p{Zs}\t|:/_—–-]*(?:\(?[\p{Zs}\t]*\d+(?:[.,]\d+)?[\p{Zs}\t]*(?:мл|ml|г|g)[\p{Zs}\t]*\)?[\p{Zs}\t|:/_—–-]*)?"""
    )
    // A printed divider can add one stray OCR character attached to the next heading.
    private val dividerNoise = Regex("""([|_—–-])[a-zа-я]$""")

    fun parse(text: String): ParsedLabel = parse(text, allowRussianGramLookalike = false)

    internal fun parse(text: String, allowRussianGramLookalike: Boolean): ParsedLabel {
        val normalized = text.lowercase(Locale.ROOT)
        val column = findColumn(normalized) ?: return ParsedLabel(null, null, null, null, null)

        val lines = normalized.lines()
        fun findValue(names: Regex, unit: Regex): Double? {
            val matching = lines.filter { names.containsMatchIn(it) }
            if (matching.size != 1) return null
            val values = unit.findAll(matching.single()).map {
                it.groupValues[1].replace(',', '.').toDoubleOrNull()
            }.toList()
            if (values.size != column.count) return null
            return values[column.index]?.takeIf(Double::isFinite)
        }

        val gramUnit = if (allowRussianGramLookalike) "г|g|r" else "г|g"
        val grams = Regex("""(?<![\d.,-])(\d+(?:[.,]\d+)?)\s*(?:$gramUnit)(?![а-яa-z])""")
        val kcal = Regex("""(?<![\d.,-])(\d+(?:[.,]\d+)?)\s*(?:ккал|kcal)(?![а-яa-z])""")
        return ParsedLabel(
            basis = column.basis,
            calories = findValue(Regex("""энерг|калори|energy|calories"""), kcal),
            protein = findValue(Regex("""белк|protein"""), grams),
            fat = findValue(Regex("""жир|fat"""), grams),
            carbs = findValue(Regex("""углевод|carbohydrate|carbs"""), grams),
        )
    }

    private data class Column(val basis: LabelBasis, val index: Int, val count: Int)

    private fun findColumn(text: String): Column? {
        val headers = basisPatterns.flatMap { (basis, pattern) ->
            pattern.findAll(text).map { basis to it.range }.toList()
        }.sortedBy { it.second.first }
        val bases = headers.map { it.first }.distinct()
        bases.singleOrNull()?.let { return Column(it, index = 0, count = 1) }
        if (headers.size != 2 || LabelBasis.PER_SERVING !in bases) return null
        val headerText = text.substring(headers.first().second.first, headers.last().second.last + 1)
        if (headerText.lines().size != 1) return null
        val gap = text.substring(headers.first().second.last + 1, headers.last().second.first)
        if (!columnGap.matches(gap.replace(dividerNoise, "$1"))) return null
        val per100 = bases.single { it != LabelBasis.PER_SERVING }
        return Column(per100, index = bases.indexOf(per100), count = 2)
    }
}
