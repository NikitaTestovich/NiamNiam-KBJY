package com.niamniam.kbjy

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.niamniam.kbjy.update.ReleaseCheckResult
import com.niamniam.kbjy.update.UpdatesScreen
import org.junit.Rule
import org.junit.Test

class UpdatesScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun offlineCheckExplainsHowToRetry() {
        compose.setContent { UpdatesScreen(false, onBack = {}, check = { ReleaseCheckResult.Offline }) }
        compose.onNodeWithText("Проверить обновления").performClick()
        compose.onNodeWithText("Подключитесь к интернету и повторите попытку.").assertExists()
    }

    @Test fun unavailableReleaseIsReportedWithoutBlockingBack() {
        compose.setContent {
            UpdatesScreen(english = false, onBack = {}, check = { ReleaseCheckResult.NoRelease })
        }
        compose.onNodeWithText("Проверить обновления").performClick()
        compose.onNodeWithText("Опубликованный релиз пока недоступен.").assertExists()
        compose.onNodeWithText("Назад").assertExists()
    }
}
