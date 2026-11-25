package com.niamniam.kbjy

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.niamniam.kbjy.data.DiaryDatabase
import com.niamniam.kbjy.data.DiaryEntry
import com.niamniam.kbjy.data.Food
import com.niamniam.kbjy.profile.RecipeIngredientRecord
import com.niamniam.kbjy.profile.RecipeRecord
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiaryDatabaseTest {
    @Test
    fun migratesV1DiaryAndCatalog() = verifyMigrationFrom(1)

    @Test
    fun migratesV2RecipeComposition() = verifyMigrationFrom(2)

    @Test
    fun migratesV3BarcodeLinks() = verifyMigrationFrom(3)

    @Test
    fun migratesV4PortionsAndDrinkDefaults() = verifyMigrationFrom(4)

    private fun verifyMigrationFrom(version: Int): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-${version}-${UUID.randomUUID()}.db"
        try {
            val file = context.getDatabasePath(name)
            file.parentFile?.mkdirs()
            SQLiteDatabase.openOrCreateDatabase(file, null).use { database ->
                createLegacyFixture(database, version)
            }
            val db = Room.databaseBuilder(context, DiaryDatabase::class.java, name)
                .addMigrations(
                    DiaryDatabase.migration1To2, DiaryDatabase.migration2To3,
                    DiaryDatabase.migration3To4, DiaryDatabase.migration4To5,
                ).build()
            try {
                val diary = db.diaryDao()
                val entries = diary.observeDay("2026-11-20").first()
                assertEquals(listOf(
                    DiaryEntry(1, "2026-11-20", "lunch", "Рис", 150.0, "г", 180.0, 4.0, 1.0, 40.0, 0.0),
                    DiaryEntry(2, "2026-11-20", "snack", "Сок", 250.0, "мл", 100.0, 0.0, 0.0, 25.0, 250.0),
                ), entries)
                assertEquals(5, db.openHelper.readableDatabase.version)
                val rice = Food(1, "Рис", "г", 120.0, 2.5, 0.5, 27.0, 150.0.takeIf { version >= 4 })
                val juice = Food(2, "Сок", "мл", 40.0, 0.0, 0.0, 10.0, 250.0.takeIf { version >= 4 }, true)
                val water = Food(3, "Вода", "ml", 0.0, 0.0, 0.0, 0.0, defaultIsDrink = true)
                assertEquals(listOf(rice, juice, water), diary.observeFoods().first().sortedBy { it.id })
                if (version >= 3) assertEquals(juice, diary.findByBarcode("04600000000000"))
                if (version >= 2) {
                    assertEquals(RecipeRecord(1, 300.0), db.recipeDao().observeRecipe(1).first())
                    assertEquals(
                        listOf(RecipeIngredientRecord(1, 1, 3, "Вода", "ml", 0.0, 0.0, 0.0, 0.0, 180.0)),
                        db.recipeDao().observeIngredients(1).first(),
                    )
                }
            } finally {
                db.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    private fun createLegacyFixture(db: SQLiteDatabase, version: Int) {
        db.execSQL("""
            CREATE TABLE diary_entries (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date TEXT NOT NULL,
                meal TEXT NOT NULL, name TEXT NOT NULL, amount REAL NOT NULL, unit TEXT NOT NULL,
                calories REAL NOT NULL, protein REAL NOT NULL, fat REAL NOT NULL, carbohydrate REAL NOT NULL,
                drinkMl REAL NOT NULL)
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE foods (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL,
                unit TEXT NOT NULL, caloriesPer100 REAL NOT NULL, proteinPer100 REAL NOT NULL,
                fatPer100 REAL NOT NULL, carbohydratePer100 REAL NOT NULL)
        """.trimIndent())
        db.execSQL("INSERT INTO diary_entries VALUES (1, '2026-11-20', 'lunch', 'Рис', 150, 'г', 180, 4, 1, 40, 0)")
        db.execSQL("INSERT INTO diary_entries VALUES (2, '2026-11-20', 'snack', 'Сок', 250, 'мл', 100, 0, 0, 25, 250)")
        db.execSQL("INSERT INTO foods VALUES (1, 'Рис', 'г', 120, 2.5, 0.5, 27)")
        db.execSQL("INSERT INTO foods VALUES (2, 'Сок', 'мл', 40, 0, 0, 10)")
        db.execSQL("INSERT INTO foods VALUES (3, 'Вода', 'ml', 0, 0, 0, 0)")
        if (version >= 2) {
            db.execSQL("CREATE TABLE recipes (foodId INTEGER NOT NULL PRIMARY KEY, cookedWeight REAL NOT NULL)")
            db.execSQL("""
                CREATE TABLE recipe_ingredients (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    recipeFoodId INTEGER NOT NULL, sourceFoodId INTEGER NOT NULL, name TEXT NOT NULL,
                    unit TEXT NOT NULL, caloriesPer100 REAL NOT NULL, proteinPer100 REAL NOT NULL,
                    fatPer100 REAL NOT NULL, carbohydratePer100 REAL NOT NULL, amount REAL NOT NULL)
            """.trimIndent())
            db.execSQL("CREATE INDEX index_recipe_ingredients_recipeFoodId ON recipe_ingredients(recipeFoodId)")
            db.execSQL("INSERT INTO recipes VALUES (1, 300)")
            db.execSQL("INSERT INTO recipe_ingredients VALUES (1, 1, 3, 'Вода', 'ml', 0, 0, 0, 0, 180)")
        }
        if (version >= 3) {
            db.execSQL("CREATE TABLE food_barcodes (code TEXT NOT NULL PRIMARY KEY, foodId INTEGER NOT NULL)")
            db.execSQL("INSERT INTO food_barcodes VALUES ('04600000000000', 2)")
        }
        if (version >= 4) {
            db.execSQL("ALTER TABLE foods ADD COLUMN defaultPortion REAL")
            db.execSQL("UPDATE foods SET defaultPortion = 150 WHERE id = 1")
            db.execSQL("UPDATE foods SET defaultPortion = 250 WHERE id = 2")
        }
        db.version = version
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
