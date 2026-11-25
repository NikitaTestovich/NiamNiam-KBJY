package com.niamniam.kbjy

import com.niamniam.kbjy.data.Food
import com.niamniam.kbjy.profile.RecipeIngredientRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CatalogProposalTest {
    @Test fun recipePayloadContainsOnlyNutritionAndRecipeFields() {
        val food = Food(42, "Суп", "г", 60.0, 3.0, 2.0, 8.0, defaultPortion = 250.0)
        val ingredient = RecipeIngredientRecord(12, 42, 7, "Вода", "мл", 0.0, 0.0, 0.0, 0.0, 750.0)
        val payload = catalogProposal("test-id", food, 900.0, listOf(ingredient))
        assertEquals("recipe", payload.getString("source"))
        assertEquals(60.0, payload.getDouble("kcal"), 0.0)
        assertEquals(900.0, payload.getDouble("cookedGrams"), 0.0)
        assertEquals(setOf("id", "name", "unit", "kcal", "protein", "fat", "carbs", "source", "ingredients", "cookedGrams"),
            payload.keys().asSequence().toSet())
        val row = payload.getJSONArray("ingredients").getJSONObject(0)
        assertEquals(setOf("name", "amount", "unit"), row.keys().asSequence().toSet())
        assertEquals("ml", row.getString("unit"))
        assertFalse(payload.has("defaultPortion"))
    }
}
