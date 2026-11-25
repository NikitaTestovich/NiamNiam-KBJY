package com.niamniam.kbjy.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LabelParserTest {
    @Test fun parsesRussianPer100GramsWithDecimalComma() {
        val result = LabelParser.parse("Пищевая ценность на 100 г\nЭнергетическая ценность 120 ккал\nБелки 1,5 г\nЖиры 3 г\nУглеводы 20 г")
        assertEquals(LabelBasis.PER_100_GRAMS, result.basis)
        assertEquals(120.0, result.calories!!, 0.0)
        assertEquals(1.5, result.protein!!, 0.0)
        assertEquals(3.0, result.fat!!, 0.0)
        assertEquals(20.0, result.carbs!!, 0.0)
    }

    @Test fun doesNotTreatKilojoulesAsKilocaloriesOrGuessColumns() {
        val result = LabelParser.parse("на 100 мл на порцию\nЭнергия 500 кДж 60 ккал\nБелки 1 г")
        assertEquals(LabelBasis.PER_100_MILLILITERS, result.basis)
        assertNull(result.calories)
        assertNull(result.protein)
    }

    @Test fun readsPer100GramsFromTheFirstColumnAndIgnoresKilojoules() {
        val result = LabelParser.parse(
            """
            На 100 г     На порцию 50 г
            Энергия 840 кДж 200 ккал     420 кДж 100 ккал
            Белки 10 г     5 г
            Жиры 3,2 г     1,6 г
            Углеводы 20 г     10 г
            """.trimIndent()
        )
        assertEquals(ParsedLabel(LabelBasis.PER_100_GRAMS, 200.0, 10.0, 3.2, 20.0), result)
    }

    @Test fun readsPer100MillilitersFromTheSecondColumnAndSkipsAnIncompleteRow() {
        val result = LabelParser.parse(
            """
            Per serving (200 ml)  __—sPer 100 ml
            Energy 90 kcal    45 kcal
            Protein 1 g    0.5 g
            Fat 0.1 g
            Carbohydrates 20 g    10 g
            """.trimIndent()
        )
        assertEquals(ParsedLabel(LabelBasis.PER_100_MILLILITERS, 45.0, 0.5, null, 10.0), result)
    }

    @Test fun doesNotReadColumnsWhenHeadersAreOnDifferentLinesOrRepeat() {
        for (header in listOf("Per 100 g\nPer serving", "Per 100 g  Per serving  Per serving", "Per 100 g  Per 100 ml")) {
            val result = LabelParser.parse("$header\nEnergy 200 kcal 100 kcal\nProtein 10 g 5 g")
            assertNull(result.basis)
            assertNull(result.calories)
        }
    }

    @Test fun ignoresANonFiniteAmountWithoutShiftingToTheServingColumn() {
        val huge = "9".repeat(400)
        val result = LabelParser.parse("Per 100 g  Per serving\nEnergy 200 kcal 100 kcal\nProtein $huge g 5 g")
        assertEquals(200.0, result.calories)
        assertNull(result.protein)
    }

    @Test fun missingAndAmbiguousValuesRemainEmpty() {
        val result = LabelParser.parse("per 100 ml\nEnergy 300 kJ\nProtein 2 g\nProtein 3 g\nFat unknown")
        assertEquals(LabelBasis.PER_100_MILLILITERS, result.basis)
        assertNull(result.calories)
        assertNull(result.protein)
        assertNull(result.fat)
        assertNull(result.carbs)
    }
}
