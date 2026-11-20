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
    fun migratesOldDiaryWithoutLosingEntries() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "migration-test.db"
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        val old = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(file, null)
        old.execSQL("CREATE TABLE diary_entries (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date TEXT NOT NULL, meal TEXT NOT NULL, name TEXT NOT NULL, amount REAL NOT NULL, unit TEXT NOT NULL, calories REAL NOT NULL, protein REAL NOT NULL, fat REAL NOT NULL, carbohydrate REAL NOT NULL, drinkMl REAL NOT NULL)")
        old.execSQL("CREATE TABLE foods (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, unit TEXT NOT NULL, caloriesPer100 REAL NOT NULL, proteinPer100 REAL NOT NULL, fatPer100 REAL NOT NULL, carbohydratePer100 REAL NOT NULL)")
        old.execSQL("INSERT INTO diary_entries VALUES (1, '2026-11-20', 'lunch', 'Rice', 150, 'g', 180, 4, 1, 40, 0)")
        old.version = 1
        old.close()
        val db = Room.databaseBuilder(context, DiaryDatabase::class.java, name)
            .addMigrations(DiaryDatabase.migration1To2, DiaryDatabase.migration2To3, DiaryDatabase.migration3To4).build()
        try {
            val result = db.diaryDao().observeDay("2026-11-20").first()
            assertEquals("Rice", result.single().name)
            assertEquals(180.0, result.single().calories, 0.0)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun storesFoodAndEntryTogetherAndFindsBarcode() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DiaryDatabase::class.java,
        ).build()
        try {
            val dao = db.diaryDao()
            val food = Food(name = "Test juice", unit = "ml", caloriesPer100 = 40.0,
                proteinPer100 = 0.0, fatPer100 = 0.0, carbohydratePer100 = 10.0, defaultPortion = 50.0)
            val entry = DiaryEntry(date = "2026-11-20", meal = "lunch", name = food.name,
                amount = 250.0, unit = "ml", calories = 100.0, protein = 0.0, fat = 0.0,
                carbohydrate = 25.0, drinkMl = 250.0)
            dao.saveNewFoodAndEntry(food, entry, "4600000000000")
            assertNotNull(dao.findByBarcode("4600000000000"))
            assertEquals(50.0, dao.findByBarcode("4600000000000")!!.defaultPortion!!, 0.0)
            val stored = dao.observeDay("2026-11-20").first()
            assertEquals(1, stored.size)
            assertEquals(250.0, stored.single().drinkMl, 0.0)
            assertEquals(100.0, stored.single().calories, 0.0)
        } finally {
            db.close()
        }
    }
}
