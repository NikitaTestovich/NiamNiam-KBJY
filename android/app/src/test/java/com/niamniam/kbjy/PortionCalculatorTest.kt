package com.niamniam.kbjy

import org.junit.Assert.assertEquals
import org.junit.Test

class PortionCalculatorTest {
    @Test
    fun calculatesActualPortionWeight() {
        assertEquals(500.0, PortionCalculator.scale(200.0, 250.0), 0.001)
    }

    @Test
    fun calculatesRecipePerHundredGrams() {
        assertEquals(150.0, PortionCalculator.perHundred(900.0, 600.0), 0.001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsZeroRecipeWeight() {
        PortionCalculator.perHundred(900.0, 0.0)
    }
}
