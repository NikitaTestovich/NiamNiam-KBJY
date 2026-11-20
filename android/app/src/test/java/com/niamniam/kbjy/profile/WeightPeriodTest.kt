package com.niamniam.kbjy.profile

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class WeightPeriodTest {
    @Test fun weekStartsOnMondayAndOrdersMultipleMeasurementsOnSameDay() {
        val values = listOf(
            WeightMeasurement(2, "2026-11-16", 81.4),
            WeightMeasurement(3, "2026-11-15", 82.0),
            WeightMeasurement(1, "2026-11-16", 81.6),
            WeightMeasurement(4, "2026-11-21", 81.2),
        )
        val actual = weightsInPeriod(values, WeightPeriod.WEEK, LocalDate.parse("2026-11-20"))
        assertEquals(listOf(1L, 2L), actual.map { it.id })
        assertEquals(-0.2, actual.last().kilograms - actual.first().kilograms, 0.0001)
    }
}
