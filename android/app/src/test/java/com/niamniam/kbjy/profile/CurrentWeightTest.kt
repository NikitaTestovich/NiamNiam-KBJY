package com.niamniam.kbjy.profile

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CurrentWeightTest {
    private val today = LocalDate.parse("2026-11-21")

    @Test fun latestDatedMeasurementWinsOverNewerInsertedHistoricalMeasurement() {
        val weights = listOf(
            WeightMeasurement(9, "2026-11-01", 90.0),
            WeightMeasurement(2, "2026-11-20", 80.0),
        )
        val result = currentWeight(weights, 85.0, today)
        assertEquals(80.0, result.kilograms, 0.0)
        assertEquals(LocalDate.parse("2026-11-20"), result.measurementDate)
    }

    @Test fun sameDateUsesLastInsertedMeasurement() {
        val weights = listOf(
            WeightMeasurement(5, "2026-11-20", 79.0),
            WeightMeasurement(3, "2026-11-20", 80.0),
        )
        assertEquals(79.0, currentWeight(weights, 85.0, today).kilograms, 0.0)
    }

    @Test fun deletingLastMeasurementFallsBackToPreviousThenToProfileWithoutInventingDate() {
        val older = WeightMeasurement(1, "2026-11-18", 82.0)
        val recent = WeightMeasurement(2, "2026-11-20", 80.0)
        assertEquals(80.0, currentWeight(listOf(older, recent), 85.0, today).kilograms, 0.0)
        assertEquals(82.0, currentWeight(listOf(older), 85.0, today).kilograms, 0.0)
        val fallback = currentWeight(emptyList(), 85.0, today)
        assertEquals(85.0, fallback.kilograms, 0.0)
        assertNull(fallback.measurementDate)
    }

    @Test fun futureMeasurementDoesNotReplaceCurrentWeight() {
        val weights = listOf(WeightMeasurement(5, "2026-11-22", 79.0))
        assertEquals(85.0, currentWeight(weights, 85.0, today).kilograms, 0.0)
    }
}
