package com.niamniam.kbjy

import org.junit.Assert.assertEquals
import org.junit.Test

class DiaryTotalsTest {
    @Test
    fun totalsWaterAndJuiceOnce() {
        val totals = DiaryTotals.sum(
            listOf(
                DiaryItem(calories = 0.0, protein = 0.0, fat = 0.0, carbohydrate = 0.0, drinkMl = 250.0),
                DiaryItem(calories = 100.0, protein = 0.0, fat = 0.0, carbohydrate = 25.0, drinkMl = 250.0),
            ),
        )
        assertEquals(500.0, totals.drinkMl, 0.001)
        assertEquals(100.0, totals.calories, 0.001)
    }
}
