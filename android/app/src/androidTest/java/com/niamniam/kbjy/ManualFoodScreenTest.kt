package com.niamniam.kbjy

import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import com.niamniam.kbjy.scanner.ManualFoodEntry
import com.niamniam.kbjy.scanner.ManualFoodScreen
import com.niamniam.kbjy.scanner.LabelBasis
import com.niamniam.kbjy.scanner.LabelParser
import com.niamniam.kbjy.scanner.ParsedLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ManualFoodScreenTest {
    @get:Rule val compose = createComposeRule()
    private val compactForm = Modifier.heightIn(max = 500.dp)

    @Test
    fun appliesNewLabelWithoutLosingNameOrPortion() {
        var saved: ManualFoodEntry? = null
        val label = mutableStateOf<ParsedLabel?>(null)
        compose.setContent {
            ManualFoodScreen(onSave = { saved = it }, modifier = compactForm, recognized = label.value)
        }
        compose.onNodeWithText("Название").performTextInput("Рыба")
        compose.onNodeWithText("Порция, г").performScrollTo().performTextInput("250")
        compose.runOnIdle { label.value = ParsedLabel(LabelBasis.PER_100_GRAMS, 200.0, 10.0, 5.0, 20.0) }
        compose.onNodeWithText("Сохранить").performScrollTo().performClick()
        compose.runOnIdle {
            val entry = checkNotNull(saved)
            assertEquals("Рыба", entry.name)
            assertEquals(250.0, entry.portion, 0.0)
            assertEquals(200.0, entry.caloriesPer100, 0.0)
        }
    }

    @Test
    fun editsAValueFromATwoColumnLabelBeforeSaving() {
        val label = LabelParser.parse(
            "Per serving  Per 100 g\nEnergy 100 kcal 200 kcal\nProtein 5 g 10 g\nFat 4 g 8 g\nCarbohydrates 11 g 22 g"
        )
        var saved: ManualFoodEntry? = null
        compose.setContent {
            ManualFoodScreen(onSave = { saved = it }, modifier = compactForm, recognized = label)
        }
        compose.onNodeWithText("Название").performTextInput("Тестовая этикетка")
        compose.onNodeWithText("Жиры, г").performScrollTo().performTextReplacement("7,5")
        compose.onNodeWithText("Порция, г").performScrollTo().performTextInput("50")
        compose.onNodeWithText("Сохранить").performScrollTo().performClick()
        compose.runOnIdle {
            val entry = checkNotNull(saved)
            assertEquals(200.0, entry.caloriesPer100, 0.0)
            assertEquals(10.0, entry.proteinPer100, 0.0)
            assertEquals(7.5, entry.fatPer100, 0.0)
            assertEquals(22.0, entry.carbsPer100, 0.0)
        }
    }

    @Test
    fun convertsLabelValuesFromKnownServingWeight() {
        var saved: ManualFoodEntry? = null
        compose.setContent {
            ManualFoodScreen(
                onSave = { saved = it },
                modifier = compactForm,
                recognized = ParsedLabel(LabelBasis.PER_SERVING, 100.0, 5.0, 2.0, 10.0),
            )
        }
        compose.onNodeWithText("Название").performTextInput("Тест")
        compose.onNodeWithText("Размер порции с этикетки, г").performScrollTo().performTextInput("50")
        compose.onNodeWithText("Порция, г").performScrollTo().performTextInput("250")
        compose.onNodeWithText("Сохранить").performScrollTo().performClick()
        compose.runOnIdle {
            val entry = checkNotNull(saved)
            assertEquals(200.0, entry.caloriesPer100, 0.0)
            assertEquals(10.0, entry.proteinPer100, 0.0)
        }
    }

    @Test
    fun rejectsEmptyValuesAndSavesEnteredPortion() {
        var saved: ManualFoodEntry? = null
        compose.setContent { ManualFoodScreen(onSave = { saved = it }, modifier = compactForm) }
        compose.onNodeWithText("Сохранить").performScrollTo().performClick()
        compose.onNodeWithText("Укажите название, неотрицательные значения и порцию больше нуля.")
            .performScrollTo().assertIsDisplayed()
        assertNull(saved)
        compose.onNodeWithText("Название").performScrollTo().performTextInput("Тестовый продукт")
        compose.onNodeWithText("Калории, ккал").performScrollTo().performTextInput("200")
        compose.onNodeWithText("Белки, г").performScrollTo().performTextInput("10")
        compose.onNodeWithText("Жиры, г").performScrollTo().performTextInput("5")
        compose.onNodeWithText("Углеводы, г").performScrollTo().performTextInput("20")
        compose.onNodeWithText("Порция, г").performScrollTo().performTextInput("250")
        compose.onNodeWithText("Сохранить").performScrollTo().performClick()
        compose.runOnIdle {
            val entry = checkNotNull(saved)
            assertEquals(250.0, entry.portion, 0.0)
            assertEquals(200.0, entry.caloriesPer100, 0.0)
        }
    }
}
