package com.niamniam.kbjy.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.niamniam.kbjy.profile.RecipeRecord
import com.niamniam.kbjy.profile.RecipeIngredientRecord
import com.niamniam.kbjy.profile.RecipeDao
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "diary_entries")
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val meal: String,
    val name: String,
    val amount: Double,
    val unit: String,
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbohydrate: Double,
    val drinkMl: Double = 0.0,
)

@Entity(tableName = "foods")
data class Food(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val unit: String,
    val caloriesPer100: Double,
    val proteinPer100: Double,
    val fatPer100: Double,
    val carbohydratePer100: Double,
    val defaultPortion: Double? = null,
    @androidx.room.ColumnInfo(defaultValue = "0") val defaultIsDrink: Boolean = false,
)

@Entity(tableName = "food_barcodes")
data class FoodBarcode(@PrimaryKey val code: String, val foodId: Long)

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary_entries WHERE date = :date ORDER BY id")
    fun observeDay(date: String): Flow<List<DiaryEntry>>

    @Insert
    suspend fun insert(entry: DiaryEntry): Long

    @Update
    suspend fun update(entry: DiaryEntry)

    @Query("DELETE FROM diary_entries WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM diary_entries WHERE date BETWEEN :start AND :end ORDER BY date, id")
    suspend fun entriesBetween(start: String, end: String): List<DiaryEntry>

    @Query("SELECT * FROM foods ORDER BY name COLLATE NOCASE")
    fun observeFoods(): Flow<List<Food>>

    @Query("SELECT * FROM foods WHERE id = :id")
    suspend fun findFood(id: Long): Food?

    @Insert
    suspend fun insertFood(food: Food): Long

    @Query("SELECT foods.* FROM foods INNER JOIN food_barcodes ON foods.id = food_barcodes.foodId WHERE food_barcodes.code = :code LIMIT 1")
    suspend fun findByBarcode(code: String): Food?

    @Insert
    suspend fun insertBarcode(barcode: FoodBarcode)

    @Update
    suspend fun updateFood(food: Food)

    @Transaction
    suspend fun saveNewFoodAndEntry(food: Food, entry: DiaryEntry, code: String? = null) {
        val id = insertFood(food)
        if (code != null) insertBarcode(FoodBarcode(code, id))
        insert(entry)
    }
}

@Database(entities = [DiaryEntry::class, Food::class, RecipeRecord::class, RecipeIngredientRecord::class, FoodBarcode::class], version = 5, exportSchema = false)
abstract class DiaryDatabase : RoomDatabase() {
    abstract fun diaryDao(): DiaryDao
    abstract fun recipeDao(): RecipeDao

    companion object {
        @Volatile private var instance: DiaryDatabase? = null
        val migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS recipes (foodId INTEGER NOT NULL, cookedWeight REAL NOT NULL, PRIMARY KEY(foodId))")
                db.execSQL("CREATE TABLE IF NOT EXISTS recipe_ingredients (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, recipeFoodId INTEGER NOT NULL, sourceFoodId INTEGER NOT NULL, name TEXT NOT NULL, unit TEXT NOT NULL, caloriesPer100 REAL NOT NULL, proteinPer100 REAL NOT NULL, fatPer100 REAL NOT NULL, carbohydratePer100 REAL NOT NULL, amount REAL NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recipe_ingredients_recipeFoodId ON recipe_ingredients(recipeFoodId)")
            }
        }
        val migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS food_barcodes (code TEXT NOT NULL, foodId INTEGER NOT NULL, PRIMARY KEY(code))")
            }
        }
        val migration3To4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE foods ADD COLUMN defaultPortion REAL")
            }
        }
        val migration4To5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE foods ADD COLUMN defaultIsDrink INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE foods SET defaultIsDrink = 1 WHERE unit IN ('мл', 'ml')")
            }
        }

        fun get(context: Context): DiaryDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                DiaryDatabase::class.java,
                "diary.db",
            ).addMigrations(migration1To2, migration2To3, migration3To4, migration4To5).build().also { instance = it }
        }
    }
}
