package com.niamniam.kbjy

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.niamniam.kbjy.profile.DailyGoal
import com.niamniam.kbjy.profile.Profile
import com.niamniam.kbjy.profile.ProfileScreen
import com.niamniam.kbjy.profile.ProfileState
import com.niamniam.kbjy.profile.Sex
import com.niamniam.kbjy.profile.AppTheme
import com.niamniam.kbjy.profile.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun savesManuallyEnteredGoals() {
        var profile: Profile? = null
        var goal: DailyGoal? = null
        val existing = Profile(Sex.MALE, 30, 180.0, 80.0, 1.2, AppLanguage.RUSSIAN, AppTheme.LIGHT)
        compose.setContent { ProfileScreen(initial = ProfileState(existing, emptyList()), onSave = { p, g -> profile = p; goal = g }) }
        compose.onNodeWithText("Мужской").assertIsSelected()
        compose.onNodeWithText("Умеренная активность").performScrollTo().performClick().assertIsSelected()
        fun fill(label: String, value: String) {
            compose.onNodeWithText(label).performScrollTo().performTextInput(value)
        }
        fill("Цель, ккал", "2100")
        fill("Белки", "120")
        fill("Жиры", "70")
        fill("Углеводы", "240")
        compose.onNodeWithText("Сохранить").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(30, profile!!.age)
            assertEquals(1.55, profile!!.activityFactor, 0.0)
            assertEquals(2100.0, goal!!.calories, 0.0)
            assertEquals(120.0, goal!!.protein, 0.0)
        }
    }

    @Test
    fun firstLaunchSavesBasicsWithoutRequiringNutritionGoals() {
        var profile: Profile? = null
        var goal: DailyGoal? = DailyGoal(2000.0, 100.0, 70.0, 220.0, null)
        compose.setContent { ProfileScreen(initial = null, onSave = { p, g -> profile = p; goal = g }) }
        compose.onNodeWithText("Цель, ккал").assertDoesNotExist()
        for ((label, value) in listOf("Возраст" to "30", "Рост, см" to "180", "Вес, кг" to "80")) {
            compose.onNodeWithText(label).performScrollTo().performTextInput(value)
        }
        compose.onNodeWithText("Открыть дневник").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(80.0, profile!!.weightKg, 0.0)
            assertNull(goal)
        }
    }

    @Test
    fun partiallyEnteredGoalsStillRequireAllNutritionValues() {
        var saved = false
        val existing = Profile(Sex.MALE, 30, 180.0, 80.0, 1.2, AppLanguage.RUSSIAN, AppTheme.LIGHT)
        compose.setContent {
            ProfileScreen(initial = ProfileState(existing, emptyList()), onSave = { _, _ -> saved = true })
        }
        compose.onNodeWithText("Цель, ккал").performScrollTo().performTextInput("2100")
        compose.onNodeWithText("Сохранить").performScrollTo().performClick()
        compose.onNodeWithText("Проверьте значения и сумму процентов.").assertExists()
        compose.runOnIdle { org.junit.Assert.assertFalse(saved) }
    }
}
