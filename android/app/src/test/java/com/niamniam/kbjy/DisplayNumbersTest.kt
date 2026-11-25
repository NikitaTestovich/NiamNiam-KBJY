package com.niamniam.kbjy

import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayNumbersTest {
    @Test fun formatsNutritionWithoutBinaryFloatingPointNoise() {
        assertEquals("2,9", formatDecimal(2.9000000000000004, english = false))
        assertEquals("1.24", formatDecimal(1.239))
        assertEquals("415", formatDecimal(415.0))
        assertEquals("0", formatDecimal(-0.001))
    }
}
