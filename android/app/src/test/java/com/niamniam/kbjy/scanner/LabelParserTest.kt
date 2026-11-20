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
        val result = LabelParser.parse("на 100 мл на порцию\nЭнергия 500 кДж 120 ккал 60 ккал\nБелки 2 г 1 г")
        assertNull(result.basis)
        assertNull(result.calories)
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
