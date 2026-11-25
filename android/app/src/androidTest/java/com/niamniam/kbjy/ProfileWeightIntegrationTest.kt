package com.niamniam.kbjy

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.niamniam.kbjy.profile.*
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProfileWeightIntegrationTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun profileUsesLastMeasurementWithoutChangingGoalOrFallbackWeight() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences("profile", 0)
        val original = preferences.getString("state", null)
        val store = ProfileStore(context)
        val weight = WeightDatabase.get(context)
        val date = LocalDate.now().toString()
        val kilos = 73.62
        val goal = DailyGoal(2100.0, 120.0, 70.0, 240.0, null)
        store.save(Profile(Sex.MALE, 30, 180.0, 85.0, 1.2, AppLanguage.RUSSIAN, AppTheme.LIGHT), goal)
        runBlocking { weight.weightDao().insert(WeightMeasurement(date = date, kilograms = kilos)) }
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                compose.onNodeWithText("Настройки").performClick()
                compose.onNodeWithText("Профиль").performScrollTo().performClick()
                compose.onNodeWithText("Последний замер: $date").performScrollTo().assertExists()
                compose.onNodeWithText("73.62").assertExists()
                compose.onNodeWithText("Расчёт поддержания: 2059 kcal").performScrollTo().assertExists()
                compose.onNodeWithText("Сохранить").performScrollTo().performClick()
                compose.waitUntil(10_000) {
                    compose.onAllNodes(androidx.compose.ui.test.hasText("Профиль")).fetchSemanticsNodes().isNotEmpty()
                }
                assertEquals(goal, store.load()!!.goals.last().goal)
                assertEquals(85.0, store.load()!!.profile.weightKg, 0.0)
            }
        } finally {
            weight.openHelper.writableDatabase.execSQL("DELETE FROM weight_measurements WHERE date = ? AND kilograms = ?", arrayOf(date, kilos))
            preferences.edit().apply { if (original == null) remove("state") else putString("state", original) }.commit()
        }
    }
}
