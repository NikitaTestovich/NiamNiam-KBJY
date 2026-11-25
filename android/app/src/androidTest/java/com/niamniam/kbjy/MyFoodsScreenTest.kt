package com.niamniam.kbjy

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.niamniam.kbjy.data.Food
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MyFoodsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun findsSavedFoodAndShowsNutritionWithoutAPortionForm() {
        val soup = Food(1, "Домашний суп", "г", 56.0, 2.0, 3.0, 6.0)
        val rice = soup.copy(id = 2, name = "Рис")
        compose.setContent { MyFoodsScreen(listOf(soup, rice), english = false, onBack = {}, onNewFood = {}, onNewRecipe = {}) }

        compose.onNodeWithText("Поиск").performTextInput("суп")
        compose.onNodeWithText("Рис").assertDoesNotExist()
        compose.onNodeWithText("Домашний суп").performClick()
        compose.onNodeWithText("Пищевая ценность на 100 г").assertExists()
        compose.onNodeWithText("Количество порций").assertDoesNotExist()
        compose.onNodeWithText("Добавить").assertDoesNotExist()
        compose.onNodeWithText("Назад").performClick()
        compose.onNodeWithText("Мои блюда").assertExists()
        compose.onNodeWithText("Рис").assertDoesNotExist()
    }

    @Test fun offersSeparateCreationActionsWithAnEmptyCatalog() {
        val actions = mutableListOf<String>()
        compose.setContent {
            MyFoodsScreen(emptyList(), english = false, onBack = {},
                onNewFood = { actions.add("food") }, onNewRecipe = { actions.add("recipe") })
        }
        compose.onNodeWithText("Новый продукт").performClick()
        compose.onNodeWithText("Новый рецепт").performClick()
        compose.runOnIdle { assertEquals(listOf("food", "recipe"), actions) }
    }

    @Test fun usesTheEditorForTheSelectedCardType() {
        val soup = Food(1, "Домашний суп", "г", 56.0, 2.0, 3.0, 6.0)
        val rice = soup.copy(id = 2, name = "Рис")
        var editedId: Long? = null
        var editedFood: Food? = null
        compose.setContent {
            MyFoodsScreen(listOf(soup, rice), english = false, onBack = {}, onNewFood = {}, onNewRecipe = {},
                recipeIds = setOf(soup.id), onEditRecipe = { editedId = it }, onEditFood = { editedFood = it })
        }
        compose.onNodeWithText("Рис").performClick()
        compose.onNodeWithText("Изменить рецепт").assertDoesNotExist()
        compose.onNodeWithText("Изменить продукт").performClick()
        compose.runOnIdle { assertEquals(rice, editedFood) }
        compose.onNodeWithText("Назад").performClick()
        compose.onNodeWithText("Домашний суп").performClick()
        compose.onNodeWithText("Изменить продукт").assertDoesNotExist()
        compose.onNodeWithText("Изменить рецепт").performClick()
        compose.runOnIdle { assertEquals(soup.id, editedId) }
    }

    @Test fun filtersByTypeAndKeepsTheFilterWhenReturningFromACard() {
        val soup = Food(1, "Домашний суп", "г", 56.0, 2.0, 3.0, 6.0)
        val mix = soup.copy(id = 2, name = "Овощи для супа")
        val rice = soup.copy(id = 3, name = "Рис")
        compose.setContent {
            MyFoodsScreen(listOf(soup, mix, rice), english = false, onBack = {}, onNewFood = {}, onNewRecipe = {},
                recipeIds = setOf(soup.id))
        }
        compose.onNodeWithText("Поиск").performTextInput("суп")
        compose.onNodeWithText("Рецепты").performClick()
        compose.onNodeWithText("Овощи для супа").assertDoesNotExist()
        compose.onNodeWithText("Домашний суп").performClick()
        compose.onNodeWithText("Назад").performClick()
        compose.onNodeWithText("Овощи для супа").assertDoesNotExist()
        compose.onNodeWithText("Продукты").performClick()
        compose.onNodeWithText("Домашний суп").assertDoesNotExist()
        compose.onNodeWithText("Рис").assertDoesNotExist()
        compose.onNodeWithText("Овощи для супа").assertExists()
    }
}
