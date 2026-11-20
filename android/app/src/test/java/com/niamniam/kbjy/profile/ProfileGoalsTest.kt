package com.niamniam.kbjy.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ProfileGoalsTest {
    @Test fun restingEnergyUsesAdultMifflinEquation() {
        assertEquals(1780.0, estimateRestingEnergy(80.0, 180.0, 30, Sex.MALE)!!, 0.001)
        assertEquals(1614.0, estimateRestingEnergy(80.0, 180.0, 30, Sex.FEMALE)!!, 0.001)
        assertNull(estimateRestingEnergy(80.0, 180.0, 18, Sex.MALE))
        assertNull(estimateRestingEnergy(0.0, 180.0, 30, Sex.MALE))
    }

    @Test fun historyReturnsGoalActiveOnRequestedDate() {
        val old = DailyGoal(2000.0, 100.0, 70.0, 240.0, null)
        val recent = DailyGoal(2100.0, 110.0, 75.0, 250.0, 2000.0)
        val history = listOf(DatedGoal(LocalDate.parse("2026-01-01"), old), DatedGoal(LocalDate.parse("2026-02-01"), recent))
        assertNull(goalForDate(history, LocalDate.parse("2025-12-31")))
        assertEquals(old, goalForDate(history, LocalDate.parse("2026-01-31")))
        assertEquals(recent, goalForDate(history, LocalDate.parse("2026-02-01")))
    }
}
