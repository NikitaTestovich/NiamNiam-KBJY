package com.niamniam.kbjy

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.niamniam.kbjy.scanner.ManualFoodEntry
import com.niamniam.kbjy.scanner.ManualFoodScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ManualFoodScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun rejectsEmptyValuesAndSavesEnteredPortion() {
        var saved: ManualFoodEntry? = null
        compose.setContent { ManualFoodScreen(onSave = { saved = it }) }
        compose.onNodeWithText("Сохранить").performClick()
        compose.onNodeWithText("Укажите название, неотрицательные значения и порцию больше нуля.").assertExists()
        assertNull(saved)
        compose.onNodeWithText("Название").performTextInput("Тестовый продукт")
        compose.onNodeWithText("Калории, ккал").performTextInput("200")
        compose.onNodeWithText("Белки, г").performTextInput("10")
        compose.onNodeWithText("Жиры, г").performTextInput("5")
        compose.onNodeWithText("Углеводы, г").performTextInput("20")
        compose.onNodeWithText("Порция, г").performTextInput("250")
        compose.onNodeWithText("Сохранить").performClick()
        compose.runOnIdle {
            assertEquals(250.0, saved!!.portion, 0.0)
            assertEquals(200.0, saved!!.caloriesPer100, 0.0)
        }
    }
}
