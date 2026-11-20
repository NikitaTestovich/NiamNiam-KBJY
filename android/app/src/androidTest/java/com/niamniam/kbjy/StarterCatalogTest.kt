package com.niamniam.kbjy

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.niamniam.kbjy.data.StarterCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StarterCatalogTest {
    @Test
    fun packagedProductHasBarcodeAndNutritionFromOfflineAsset() {
        val products = StarterCatalog.loadPackaged(ApplicationProvider.getApplicationContext())
        val product = products.single { it.code == "4810268047602" }
        assertTrue(product.food.name.contains("Сметана"))
        assertEquals(202.0, product.food.caloriesPer100, 0.0)
        assertEquals(20.0, product.food.fatPer100, 0.0)
    }
}
