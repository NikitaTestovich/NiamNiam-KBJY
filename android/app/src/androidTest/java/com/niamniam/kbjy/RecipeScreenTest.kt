package com.niamniam.kbjy

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.niamniam.kbjy.data.DiaryDatabase
import com.niamniam.kbjy.data.Food
import com.niamniam.kbjy.profile.AppLanguage
import com.niamniam.kbjy.profile.RecipeScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class RecipeScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun savesCatalogRecipeWithoutShowingOrRecordingAnEatenPortion() {
        val db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DiaryDatabase::class.java,
        ).build()
        try {
            var savedFood: Food? = null
            var savedEntry: com.niamniam.kbjy.data.DiaryEntry? = null
            val rice = Food(name = "Рис варёный", unit = "g", caloriesPer100 = 150.0,
                proteinPer100 = 3.0, fatPer100 = 1.0, carbohydratePer100 = 30.0)
            compose.setContent {
                RecipeScreen(
                    dao = db.diaryDao(), date = "2026-11-21", meal = "lunch",
                    language = AppLanguage.RUSSIAN, starterFoods = listOf(rice),
                    allowDiaryEntry = false,
                    onSaved = {}, onBack = {},
                    saveRecipe = { food, _, _, entry -> savedFood = food; savedEntry = entry },
                )
            }
            compose.onNodeWithText("Название блюда").performTextInput("Рисовая тарелка")
            compose.onNodeWithText("Найти ингредиент").performTextInput("рис")
            compose.onNodeWithText("Рис варёный · г").performClick()
            compose.onNodeWithText("Количество (г)").performTextInput("600")
            compose.onNodeWithText("Добавить ингредиент").performClick()
            compose.onNodeWithText("Вес готового блюда, г").performScrollTo().performTextInput("600")
            compose.onNodeWithText("Съеденная порция, г (необязательно)").assertDoesNotExist()
            compose.onNodeWithText("Сохранить рецепт").performScrollTo().performClick()
            compose.waitUntil(10_000) { savedFood != null }
            assertEquals(150.0, savedFood!!.caloriesPer100, 0.001)
            assertNull(savedEntry)
        } finally {
            db.close()
        }
    }
}
