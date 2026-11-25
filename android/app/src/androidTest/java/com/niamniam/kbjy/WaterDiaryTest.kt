package com.niamniam.kbjy

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.niamniam.kbjy.data.DiaryDatabase
import com.niamniam.kbjy.data.DiaryEntry
import com.niamniam.kbjy.profile.AppLanguage
import com.niamniam.kbjy.profile.AppTheme
import com.niamniam.kbjy.profile.Profile
import com.niamniam.kbjy.profile.ProfileStore
import com.niamniam.kbjy.profile.Sex
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WaterDiaryTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test
    fun quickWaterHasItsOwnSectionRegardlessOfSelectedMeal() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("profile", 0)
        val previous = prefs.getString("state", null)
        val dao = DiaryDatabase.get(context).diaryDao()
        val date = LocalDate.now().toString()
        val before = runBlocking { dao.entriesBetween(date, date).map { it.id }.toSet() }
        val legacy = DiaryEntry(date = date, meal = "breakfast", name = "Вода", amount = 318.5, unit = "мл",
            calories = 0.0, protein = 0.0, fat = 0.0, carbohydrate = 0.0, drinkMl = 318.5)
        val (legacyId, teaId) = runBlocking {
            dao.insert(legacy) to dao.insert(legacy.copy(name = "Чай без сахара", amount = 192.0, drinkMl = 192.0))
        }
        try {
            ProfileStore(context).save(Profile(Sex.MALE, 30, 180.0, 80.0, 1.2, AppLanguage.RUSSIAN, AppTheme.LIGHT), null)
            ActivityScenario.launch(MainActivity::class.java).use {
                compose.onNodeWithText("Добавить на завтрак").performScrollTo().performClick()
                compose.onNodeWithText("Назад").performClick()
                compose.onNodeWithText("Добавить воду").performScrollTo().performClick()
                compose.onNodeWithText("Объём, мл").performTextReplacement("361,25")
                compose.onNodeWithText("Добавить").performClick()
                val newWater = hasText("361,25 мл") and hasAnyAncestor(hasTestTag("today-water"))
                compose.waitUntil(8_000) { compose.onAllNodes(hasText("361,25 мл", substring = true)).fetchSemanticsNodes().isNotEmpty() }
                val saved = runBlocking { dao.entriesBetween(date, date).single { it.id !in before && it.id != legacyId && it.id != teaId } }
                assertEquals("water", saved.meal)
                assertEquals(361.25, saved.drinkMl, 0.0)
                compose.onNode(newWater).performScrollTo().assertExists()
                compose.onNode(hasText("318,5 мл") and hasAnyAncestor(hasTestTag("today-water"))).assertExists()
                compose.onNode(hasText("Чай без сахара") and hasAnyAncestor(hasTestTag("today-water"))).assertDoesNotExist()
                compose.onNodeWithText("Чай без сахара").assertExists()
                compose.onNodeWithText("История").performClick()
                compose.onNodeWithTag("history-list").performScrollToNode(hasText("Вода · 361,25 мл"))
                compose.onNodeWithText("Вода · 361,25 мл").assertExists()
            }
            val preserved = runBlocking { dao.entriesBetween(date, date).single { it.id == legacyId } }
            assertEquals(legacy.copy(id = legacyId), preserved)
        } finally {
            runBlocking {
                dao.entriesBetween(date, date).filter { it.id !in before }.forEach { dao.delete(it.id) }
            }
            prefs.edit().putString("state", previous).commit()
        }
    }
}
