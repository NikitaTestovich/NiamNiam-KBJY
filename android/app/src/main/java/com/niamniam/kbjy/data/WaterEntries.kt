package com.niamniam.kbjy.data

internal const val WATER_MEAL = "water"

internal fun DiaryEntry.hasNoNutrition(): Boolean =
    calories == 0.0 && protein == 0.0 && fat == 0.0 && carbohydrate == 0.0

internal fun DiaryEntry.isWaterEntry(): Boolean = meal == WATER_MEAL || (
    drinkMl > 0 && hasNoNutrition() &&
        name.trim().lowercase() in setOf("вода", "water")
)
