package com.niamniam.kbjy.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class LabelOcrFallbackTest {
    private val incompleteEnglish = "per 100 g\nEnergy 120 kcal\nProtein 5 g\nFat 3 a"
    private val incompleteRussian = "на 100 г\nЭнергия 120 ккал\nБелки 5 г\nЖиры Зг\nУглеводы 20 г"

    @Test
    fun fillsMissingFieldFromAnAgreeingEnglishPass() {
        val result = parseLabelWithRetry(incompleteEnglish, readSmallerRussian = ::unexpectedRead, readEnglish = {
            "per 100 g\nProtein 5 g\nFat 3 g"
        })
        assertEquals(ParsedLabel(LabelBasis.PER_100_GRAMS, 120.0, 5.0, 3.0, null), result)
    }

    @Test
    fun leavesTheMissingValueEmptyWhenTheRetryCannotBeCorroborated() {
        for (retry in listOf(
            "per 100 ml\nEnergy 120 kcal\nFat 3 g",
            "per 100 g\nEnergy 120 kcal\nProtein 6 g\nFat 3 g",
            "per 100 g\nFat 3 g",
        )) {
            val result = parseLabelWithRetry(incompleteEnglish, { retry }, ::unexpectedRead)
            assertEquals(LabelParser.parse(incompleteEnglish), result)
            assertNull(result.fat)
        }
    }

    @Test
    fun skipsRetryWhenTheLabelIsCompleteOrHasNoUsableLanguageMarker() {
        for (text in listOf(
            "per 100 g\nEnergy 120 kcal\nProtein 5 g\nFat 3 g\nCarbohydrates 20 g",
            "на 100 г\nЭнергия 120 ккал\nБелки 5 г\nЖиры 3 г\nУглеводы 20 г",
            "per serving and per 100 g\nEnergy 120 kcal",
            "Цена 100 г\nЭнергия 120 ккал",
        )) {
            assertEquals(LabelParser.parse(text), parseLabelWithRetry(text, ::unexpectedRead, ::unexpectedRead))
        }
    }

    @Test
    fun fillsRussianFatWhenTheSmallerImageReadsADigitAndCorroboratesOtherNutrients() {
        val result = parseLabelWithRetry(incompleteRussian, ::unexpectedRead) {
            "на 100 г\nЭнергия 120 ккал\nБелки 5 г\nЖиры 3 r\nУглеводы 20 r"
        }
        assertEquals(ParsedLabel(LabelBasis.PER_100_GRAMS, 120.0, 5.0, 3.0, 20.0), result)
        assertNull(LabelParser.parse(incompleteRussian.replace("Зг", "3 r")).fat)
    }

    @Test
    fun leavesRussianFatBlankForLettersConflictingValuesOrAnAmbiguousQuantity() {
        for (retry in listOf(
            "на 100 г\nЭнергия 120 ккал\nБелки 5 г\nЖиры З г",
            "на 100 г\nЭнергия 120 ккал\nБелки 6 г\nУглеводы 20 г\nЖиры 3 r",
            "на 100 мл\nЭнергия 120 ккал\nБелки 5 г\nЖиры 3 r",
            "на 100 г\nБелки 5 г\nЖиры 3 r",
            "на 100 г\nЭнергия 120 ккал\nБелки 5 г\nЖиры 3 r 5 г",
        )) {
            assertEquals(LabelParser.parse(incompleteRussian), parseLabelWithRetry(incompleteRussian, ::unexpectedRead) { retry })
        }
    }

    private fun unexpectedRead(): String {
        fail("Unexpected second OCR pass")
        return ""
    }
}
