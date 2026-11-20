package com.niamniam.kbjy

import kotlin.math.abs

data class MacroGoals(val protein: Double, val fat: Double, val carbohydrate: Double)

object GoalCalculator {
    fun macros(calories: Double, protein: Double, fat: Double, carbohydrate: Double): MacroGoals {
        require(calories.isFinite() && calories > 0)
        require(listOf(protein, fat, carbohydrate).all { it.isFinite() && it >= 0 })
        require(abs(protein + fat + carbohydrate - 100.0) < 0.0001)
        return MacroGoals(
            protein = calories * protein / 100 / 4,
            fat = calories * fat / 100 / 9,
            carbohydrate = calories * carbohydrate / 100 / 4,
        )
    }
}
