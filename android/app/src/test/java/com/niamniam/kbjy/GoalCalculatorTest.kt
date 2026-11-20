package com.niamniam.kbjy

import org.junit.Assert.assertEquals
import org.junit.Test

class GoalCalculatorTest {
    @Test
    fun convertsPercentagesToGrams() {
        val result = GoalCalculator.macros(2000.0, 20.0, 35.0, 45.0)
        assertEquals(100.0, result.protein, 0.001)
        assertEquals(77.7778, result.fat, 0.001)
        assertEquals(225.0, result.carbohydrate, 0.001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsPercentagesAboveOneHundred() {
        GoalCalculator.macros(2000.0, 30.0, 40.0, 40.0)
    }
}
