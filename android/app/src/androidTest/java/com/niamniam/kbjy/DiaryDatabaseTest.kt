package com.niamniam.kbjy

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.niamniam.kbjy.data.DiaryDatabase
import com.niamniam.kbjy.data.DiaryEntry
import com.niamniam.kbjy.data.Food
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiaryDatabaseTest {
    @Test
    fun storesFoodAndEntryTogetherAndFindsBarcode() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DiaryDatabase::class.java,
        ).build()
        try {
            val dao = db.diaryDao()
            val food = Food(name = "Test juice", unit = "ml", caloriesPer100 = 40.0,
                proteinPer100 = 0.0, fatPer100 = 0.0, carbohydratePer100 = 10.0)
            val entry = DiaryEntry(date = "2026-11-20", meal = "lunch", name = food.name,
                amount = 250.0, unit = "ml", calories = 100.0, protein = 0.0, fat = 0.0,
                carbohydrate = 25.0, drinkMl = 250.0)
            dao.saveNewFoodAndEntry(food, entry, "4600000000000")
            assertNotNull(dao.findByBarcode("4600000000000"))
            val stored = dao.observeDay("2026-11-20").first()
            assertEquals(1, stored.size)
            assertEquals(250.0, stored.single().drinkMl, 0.0)
            assertEquals(100.0, stored.single().calories, 0.0)
        } finally {
            db.close()
        }
    }
}
