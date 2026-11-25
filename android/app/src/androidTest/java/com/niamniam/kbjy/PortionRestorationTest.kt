package com.niamniam.kbjy

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.niamniam.kbjy.profile.*
import org.junit.Rule
import org.junit.Test

class PortionRestorationTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun unfinishedCatalogPortionSurvivesRecreation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("profile", 0)
        val original = prefs.getString("state", null)
        ProfileStore(context).save(Profile(Sex.MALE, 30, 175.0, 75.0, 1.2, AppLanguage.RUSSIAN, AppTheme.LIGHT), null)
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                compose.onNodeWithText("Добавить на обед").performScrollTo().performClick()
                compose.onNodeWithText("Поиск", substring = false).performTextReplacement("Рис белый варёный")
                compose.waitUntil(10_000) { compose.onAllNodesWithText("Рис белый варёный", substring = true).fetchSemanticsNodes().size > 1 }
                compose.onNodeWithText("Рис белый варёный без добавленного жира", substring = true).performClick()
                compose.onNodeWithText("Размер одной порции", substring = true).performTextReplacement("250")
                compose.onNodeWithText("Количество порций").performTextReplacement("2")
                scenario.recreate()
                compose.onNodeWithText("Размер одной порции", substring = true).assertExists()
                compose.onNodeWithText("250").assertExists()
                compose.onNodeWithText("2").assertExists()
            }
        } finally {
            prefs.edit().putString("state", original).commit()
        }
    }
}
