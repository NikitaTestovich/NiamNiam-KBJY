package com.niamniam.kbjy.profile

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.niamniam.kbjy.data.Food
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "recipes", primaryKeys = ["foodId"])
data class RecipeRecord(val foodId: Long, val cookedWeight: Double)

@Entity(tableName = "recipe_ingredients", indices = [Index("recipeFoodId")])
data class RecipeIngredientRecord(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeFoodId: Long,
    val sourceFoodId: Long,
    val name: String,
    val unit: String,
    val caloriesPer100: Double,
    val proteinPer100: Double,
    val fatPer100: Double,
    val carbohydratePer100: Double,
    val amount: Double,
) {
    fun toIngredient() = RecipeIngredient(
        Food(sourceFoodId, name, unit, caloriesPer100, proteinPer100, fatPer100, carbohydratePer100), amount,
    )
}

@Dao
interface RecipeDao {
    @Query("SELECT foodId FROM recipes")
    fun observeRecipeFoodIds(): Flow<List<Long>>

    @Query("SELECT * FROM recipes WHERE foodId = :foodId")
    fun observeRecipe(foodId: Long): Flow<RecipeRecord?>

    @Query("SELECT * FROM recipe_ingredients WHERE recipeFoodId = :foodId ORDER BY id")
    fun observeIngredients(foodId: Long): Flow<List<RecipeIngredientRecord>>

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: RecipeRecord)

    @Insert suspend fun insertIngredients(ingredients: List<RecipeIngredientRecord>)
    @Query("DELETE FROM recipe_ingredients WHERE recipeFoodId = :foodId")
    suspend fun deleteIngredients(foodId: Long)

    @Transaction
    suspend fun replaceComposition(foodId: Long, cookedWeight: Double, ingredients: List<RecipeIngredient>) {
        insertRecipe(RecipeRecord(foodId, cookedWeight))
        deleteIngredients(foodId)
        insertIngredients(ingredients.map {
            RecipeIngredientRecord(
                recipeFoodId = foodId, sourceFoodId = it.food.id, name = it.food.name, unit = it.food.unit,
                caloriesPer100 = it.food.caloriesPer100, proteinPer100 = it.food.proteinPer100,
                fatPer100 = it.food.fatPer100, carbohydratePer100 = it.food.carbohydratePer100, amount = it.amount,
            )
        })
    }
}
