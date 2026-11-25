package com.niamniam.kbjy

import android.app.Instrumentation
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.MediaStore
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.niamniam.kbjy.profile.*
import com.niamniam.kbjy.data.DiaryDatabase
import org.junit.Rule
import org.junit.Test

class ManualFoodNavigationTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test
    fun formSurvivesUnavailableCameraAndIsClearedAfterCancelAndSave() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val cameraUnavailable = object : Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
                if (intent.action == MediaStore.ACTION_IMAGE_CAPTURE) throw ActivityNotFoundException("Test camera unavailable")
                return null
            }
        }
        val prefs = context.getSharedPreferences("profile", 0)
        val original = prefs.getString("state", null)
        ProfileStore(context).save(
            Profile(Sex.MALE, 30, 175.0, 75.0, 1.2, AppLanguage.RUSSIAN, AppTheme.LIGHT),
            DailyGoal(2000.0, 100.0, 70.0, 200.0, null),
        )
        instrumentation.addMonitor(cameraUnavailable)
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                compose.onNodeWithText("Добавить на обед").performScrollTo().performClick()
                compose.waitUntil(10_000) {
                    compose.onAllNodes(hasText("Базовые продукты")).fetchSemanticsNodes().isNotEmpty()
                }
                compose.onNodeWithText("Готовые блюда").performClick()
                val catalog = compose.onNode(hasScrollAction())
                catalog.performScrollToNode(hasText("Бефстроганов из говядины", substring = true))
                compose.onNodeWithText("Бефстроганов из говядины", substring = true).assertExists()
                catalog.performScrollToIndex(0)
                compose.onNodeWithText("Готовые блюда").performClick()
                compose.onNodeWithText("Новый продукт").performClick()
                compose.onNodeWithText("Название").performTextInput("Рыба тест")
                compose.onNodeWithText("Порция, г").performScrollTo().performTextInput("250")
                compose.onNodeWithText("Считать этикетку").performClick()
                compose.onNodeWithText("Открыть камеру").performClick()
                compose.onNodeWithText("Приложение камеры недоступно. Введите данные вручную.").assertExists()
                compose.onNodeWithText("Ввести вручную").performClick()
                compose.onNodeWithText("Рыба тест").assertExists()
                compose.onNodeWithText("250").assertExists()
                compose.onNodeWithText("Назад").performClick()
                compose.onNodeWithText("Новый продукт").performClick()
                compose.onNodeWithText("Рыба тест").assertDoesNotExist()
                compose.onNodeWithText("250").assertDoesNotExist()
                compose.onNodeWithText("Название").performTextInput("Проверка формы OCR")
                compose.onNodeWithText("Калории, ккал").performScrollTo().performTextInput("200")
                compose.onNodeWithText("Белки, г").performScrollTo().performTextInput("10")
                compose.onNodeWithText("Жиры, г").performScrollTo().performTextInput("5")
                compose.onNodeWithText("Углеводы, г").performScrollTo().performTextInput("20")
                compose.onNodeWithText("Порция, г").performScrollTo().performTextInput("100")
                compose.onNodeWithText("Сохранить").performScrollTo().performClick()
                compose.waitUntil(10_000) {
                    compose.onAllNodes(hasText("Добавить на обед")).fetchSemanticsNodes().isNotEmpty()
                }
                compose.onNodeWithText("Добавить на обед").performScrollTo().performClick()
                compose.onNodeWithText("Новый продукт").performClick()
                compose.onNodeWithText("Проверка формы OCR").assertDoesNotExist()
            }
        } finally {
            instrumentation.removeMonitor(cameraUnavailable)
            prefs.edit().apply { if (original == null) remove("state") else putString("state", original) }.commit()
            val db = DiaryDatabase.get(context).openHelper.writableDatabase
            db.execSQL("DELETE FROM foods WHERE name = ?", arrayOf("Проверка формы OCR"))
            db.execSQL("DELETE FROM diary_entries WHERE name = ?", arrayOf("Проверка формы OCR"))
        }
    }
}
