package com.niamniam.kbjy

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.niamniam.kbjy.profile.AppLanguage
import com.niamniam.kbjy.profile.AppTheme
import com.niamniam.kbjy.profile.DailyGoal
import com.niamniam.kbjy.profile.DatedGoal
import com.niamniam.kbjy.profile.Profile
import com.niamniam.kbjy.profile.ProfileStore
import com.niamniam.kbjy.profile.Sex
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsPreferencesTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val profile = Profile(Sex.FEMALE, 33, 174.0, 73.0, 1.375, AppLanguage.RUSSIAN, AppTheme.LIGHT)

    @Test
    fun allThemesCanBeSelectedAndKeepExistingGoals() = withProfile { store ->
        val firstDate = LocalDate.of(2026, 1, 10)
        val firstGoal = DailyGoal(1900.0, 110.0, 65.0, 220.0, null)
        val secondGoal = firstGoal.copy(calories = 2100.0, waterMl = 1800.0)
        store.save(profile, firstGoal, firstDate)
        store.save(profile, secondGoal, firstDate.plusDays(1))

        ActivityScenario.launch(MainActivity::class.java).use { activity ->
            compose.onNodeWithText("Настройки").performClick()
            compose.onNodeWithText("Тема").performClick()
            assertThemeSelected("Светлая")
            compose.onNodeWithText("Тёмная").performScrollTo().performClick()
            assertThemeSelected("Тёмная")
            activity.recreate()
            assertThemeSelected("Тёмная")
            compose.onNodeWithText("Лесная").performScrollTo().performClick()
            assertThemeSelected("Лесная")
        }

        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText("Настройки").performClick()
            compose.onNodeWithText("Тема").performClick()
            assertThemeSelected("Лесная")
            assertEquals(profile.copy(theme = AppTheme.FOREST), store.load()?.profile)
            compose.onNodeWithText("Светлая").performScrollTo().performClick()
            assertThemeSelected("Светлая")
        }
        assertEquals(profile, store.load()?.profile)
        assertEquals(listOf(DatedGoal(firstDate, firstGoal), DatedGoal(firstDate.plusDays(1), secondGoal)), store.load()?.goals)
    }

    @Test
    fun primaryNavigationFillsEveryScreenAndHidesTheCurrentDestination() = withProfile { store ->
        store.save(profile, null)
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithTag("navigation-today").assertDoesNotExist()
            compose.onNodeWithTag("navigation-settings").performClick()
            compose.onNodeWithTag("navigation-settings").assertDoesNotExist()
            compose.onNodeWithText("Вес").performClick()
            compose.onNodeWithText("Записать вес").assertExists()
            compose.onNodeWithTag("navigation-history").performClick()
            compose.onNodeWithTag("navigation-history").assertDoesNotExist()
            compose.onNodeWithTag("navigation-today").performClick()
            compose.onNodeWithTag("navigation-today").assertDoesNotExist()
        }
    }

    @Test
    fun languageCanBeSavedAndRestoredBeforeNutritionGoalsAreSet() = withProfile { store ->
        store.save(profile, null)
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText("Настройки").performClick()
            compose.onNodeWithText("Профиль").performClick()
            compose.waitUntil(5_000) {
                compose.onAllNodes(hasText("English")).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("English").performClick().assertIsSelected()
            compose.onNodeWithText("Save").performScrollTo().performClick()
            compose.onNodeWithText("Settings").assertExists()
        }
        assertEquals(profile.copy(language = AppLanguage.ENGLISH), store.load()?.profile)
        assertEquals(emptyList<DatedGoal>(), store.load()?.goals)

        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText("Settings").performClick()
            compose.onNodeWithText("Profile").performClick()
            compose.waitUntil(5_000) {
                compose.onAllNodes(hasText("Русский")).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("English").assertIsSelected()
            compose.onNodeWithText("Русский").performClick().assertIsSelected()
            compose.onNodeWithText("Сохранить").performScrollTo().performClick()
            compose.onNodeWithText("Настройки").assertExists()
        }
        assertEquals(profile, store.load()?.profile)
    }

    private fun assertThemeSelected(title: String) {
        compose.onNodeWithText("✓ $title").performScrollTo().assertIsNotEnabled()
    }

    private fun withProfile(verify: (ProfileStore) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences("profile", 0)
        val previous = preferences.getString("state", null)
        try {
            verify(ProfileStore(context))
        } finally {
            preferences.edit().putString("state", previous).commit()
        }
    }
}
