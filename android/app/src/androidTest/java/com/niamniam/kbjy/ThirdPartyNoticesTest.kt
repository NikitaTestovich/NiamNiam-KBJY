package com.niamniam.kbjy

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.niamniam.kbjy.profile.AppLanguage
import com.niamniam.kbjy.profile.AppTheme
import com.niamniam.kbjy.profile.DailyGoal
import com.niamniam.kbjy.profile.Profile
import com.niamniam.kbjy.profile.ProfileStore
import com.niamniam.kbjy.profile.Sex
import org.junit.Rule
import org.junit.Test

class ThirdPartyNoticesTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test
    fun settingsOpenBundledNoticesAndBackReturnsThroughBothScreens() {
        withProfile(AppLanguage.RUSSIAN) { activity ->
            compose.onNodeWithText("Настройки").performClick()
            compose.onNodeWithText("Источники и лицензии").performScrollTo().performClick()
            compose.onNodeWithText("Об источниках и компонентах").performClick()
            waitForNoticeText("Open Food Facts contributors")
            activity.recreate()
            compose.onNodeWithText("Об источниках и компонентах").assertExists()
            activity.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            compose.onNodeWithText("Источники и лицензии").assertExists()
            compose.onNodeWithText("Назад").performClick()
            compose.onNodeWithText("Настройки").assertExists()
        }
    }

    @Test
    fun englishSettingsOpenAnOriginalLicenseText() {
        withProfile(AppLanguage.ENGLISH) {
            compose.onNodeWithText("Settings").performClick()
            compose.onNodeWithText("Sources and licenses").performScrollTo().performClick()
            compose.onNodeWithText("Leptonica 1.85.0").performClick()
            waitForNoticeText("Copyright (C) 2001-2020 Leptonica")
        }
    }

    @Test
    fun opensGoogleDependencyNoticesFromTheBundledList() {
        withProfile(AppLanguage.ENGLISH) {
            compose.onNodeWithText("Settings").performClick()
            compose.onNodeWithText("Sources and licenses").performScrollTo().performClick()
            val title = "Google SDK open-source notices"
            compose.onNode(hasScrollAction()).performScrollToNode(hasText(title))
            compose.onNodeWithText(title).performClick()
            waitForNoticeText("Abseil")
        }
    }

    private fun waitForNoticeText(text: String) {
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun withProfile(language: AppLanguage, verify: (ActivityScenario<MainActivity>) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("profile", 0)
        val previous = prefs.getString("state", null)
        try {
            ProfileStore(context).save(
                Profile(Sex.FEMALE, 33, 174.0, 73.0, 1.375, language, AppTheme.LIGHT),
                DailyGoal(1950.0, 110.0, 67.0, 227.0, null),
            )
            ActivityScenario.launch(MainActivity::class.java).use(verify)
        } finally {
            prefs.edit().putString("state", previous).commit()
        }
    }
}
