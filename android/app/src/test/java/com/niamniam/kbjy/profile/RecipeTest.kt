package com.niamniam.kbjy.profile

import com.niamniam.kbjy.data.Food
import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeTest {
    @Test fun dividesIngredientTotalsByCookedWeight() {
        val first = Food(name = "Rice", unit = "g", caloriesPer100 = 100.0, proteinPer100 = 5.0, fatPer100 = 1.0, carbohydratePer100 = 20.0)
        val second = Food(name = "Chicken", unit = "g", caloriesPer100 = 200.0, proteinPer100 = 20.0, fatPer100 = 10.0, carbohydratePer100 = 0.0)
        val recipe = calculateRecipe("Dinner", listOf(RecipeIngredient(first, 300.0), RecipeIngredient(second, 300.0)), 600.0)
        assertEquals(150.0, recipe.caloriesPer100, 0.0001)
        assertEquals(12.5, recipe.proteinPer100, 0.0001)
        assertEquals(5.5, recipe.fatPer100, 0.0001)
        assertEquals(10.0, recipe.carbohydratePer100, 0.0001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsZeroCookedWeight() {
        val ingredient = Food(name = "Food", unit = "g", caloriesPer100 = 100.0, proteinPer100 = 0.0, fatPer100 = 0.0, carbohydratePer100 = 0.0)
        calculateRecipe("Recipe", listOf(RecipeIngredient(ingredient, 100.0)), 0.0)
    }
}
