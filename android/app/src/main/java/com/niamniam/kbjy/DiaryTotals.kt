package com.niamniam.kbjy

data class DiaryItem(
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbohydrate: Double,
    val drinkMl: Double,
)

object DiaryTotals {
    fun sum(items: List<DiaryItem>): DiaryItem = DiaryItem(
        calories = items.sumOf { it.calories },
        protein = items.sumOf { it.protein },
        fat = items.sumOf { it.fat },
        carbohydrate = items.sumOf { it.carbohydrate },
        drinkMl = items.sumOf { it.drinkMl },
    )
}
