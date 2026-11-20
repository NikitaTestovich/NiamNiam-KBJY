package com.niamniam.kbjy.profile

import java.time.LocalDate

enum class Sex { MALE, FEMALE }
enum class AppLanguage { RUSSIAN, ENGLISH }
enum class AppTheme { LIGHT, DARK, FOREST }
enum class MacroMode { GRAMS, PERCENT }

data class DailyGoal(
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbohydrate: Double,
    val waterMl: Double?,
    val macroMode: MacroMode = MacroMode.GRAMS,
)

data class DatedGoal(val date: LocalDate, val goal: DailyGoal)

fun goalForDate(history: List<DatedGoal>, date: LocalDate): DailyGoal? =
    history.filter { !it.date.isAfter(date) }.maxByOrNull { it.date }?.goal

fun estimateRestingEnergy(weightKg: Double, heightCm: Double, age: Int, sex: Sex): Double? {
    if (!weightKg.isFinite() || weightKg <= 0 || !heightCm.isFinite() || heightCm <= 0 || age !in 19..78) return null
    val sexAdjustment = if (sex == Sex.MALE) 5 else -161
    return 10 * weightKg + 6.25 * heightCm - 5 * age + sexAdjustment
}
