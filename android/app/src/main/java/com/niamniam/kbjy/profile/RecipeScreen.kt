package com.niamniam.kbjy.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.niamniam.kbjy.PortionCalculator
import com.niamniam.kbjy.data.DiaryDao
import com.niamniam.kbjy.data.DiaryEntry
import com.niamniam.kbjy.data.Food
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RecipeIngredient(val food: Food, val amount: Double)
data class ExistingRecipe(val food: Food, val cookedWeight: Double, val ingredients: List<RecipeIngredient>)

fun calculateRecipe(name: String, ingredients: List<RecipeIngredient>, cookedWeight: Double): Food {
    require(name.isNotBlank() && ingredients.isNotEmpty() && cookedWeight.isFinite() && cookedWeight > 0)
    fun total(value: (Food) -> Double): Double = ingredients.sumOf {
        PortionCalculator.scale(value(it.food), it.amount)
    }
    return Food(
        name = name.trim(),
        unit = "g",
        caloriesPer100 = PortionCalculator.perHundred(total { it.caloriesPer100 }, cookedWeight),
        proteinPer100 = PortionCalculator.perHundred(total { it.proteinPer100 }, cookedWeight),
        fatPer100 = PortionCalculator.perHundred(total { it.fatPer100 }, cookedWeight),
        carbohydratePer100 = PortionCalculator.perHundred(total { it.carbohydratePer100 }, cookedWeight),
    )
}

@Composable
fun RecipeScreen(
    dao: DiaryDao,
    date: String,
    meal: String,
    language: AppLanguage,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    initialRecipe: ExistingRecipe? = null,
    saveRecipe: suspend (Food, Double, List<RecipeIngredient>, DiaryEntry?) -> Unit,
) {
    val foods by dao.observeFoods().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val russian = language == AppLanguage.RUSSIAN
    fun label(ru: String, en: String) = if (russian) ru else en
    fun parseNumber(value: String) = value.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 }
    var name by remember(initialRecipe) { mutableStateOf(initialRecipe?.food?.name.orEmpty()) }
    var search by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Food?>(null) }
    var ingredientAmount by remember { mutableStateOf("") }
    var ingredients by remember(initialRecipe) { mutableStateOf(initialRecipe?.ingredients.orEmpty()) }
    var cookedWeight by remember(initialRecipe) { mutableStateOf(initialRecipe?.cookedWeight?.toString().orEmpty()) }
    var portion by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onBack, enabled = !saving) { Text(label("Назад", "Back")) }
        Text(if (initialRecipe == null) label("Создать рецепт", "Create recipe") else label("Изменить рецепт", "Edit recipe"), style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(name, { name = it }, label = { Text(label("Название блюда", "Recipe name")) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(search, { search = it }, label = { Text(label("Найти ингредиент", "Find an ingredient")) }, modifier = Modifier.fillMaxWidth())
        foods.filter { search.isNotBlank() && it.name.contains(search.trim(), ignoreCase = true) }.take(12).forEach { food ->
            OutlinedButton(onClick = { selected = food }) { Text("${food.name} · ${food.unit}") }
        }
        selected?.let { food ->
            Text(label("Ингредиент: ", "Ingredient: ") + food.name)
            OutlinedTextField(
                ingredientAmount, { ingredientAmount = it },
                label = { Text(label("Количество", "Amount") + " (${food.unit})") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(onClick = {
                val amount = parseNumber(ingredientAmount)
                if (amount == null) {
                    error = label("Введите количество ингредиента.", "Enter an ingredient amount.")
                } else {
                    ingredients = ingredients + RecipeIngredient(food, amount)
                    ingredientAmount = ""
                    selected = null
                    search = ""
                    error = null
                }
            }) { Text(label("Добавить ингредиент", "Add ingredient")) }
        }
        ingredients.forEachIndexed { index, ingredient ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${ingredient.food.name}: ${ingredient.amount} ${ingredient.food.unit}", modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { ingredients = ingredients.filterIndexed { i, _ -> i != index } }) {
                    Text(label("Удалить", "Remove"))
                }
            }
        }
        OutlinedTextField(cookedWeight, { cookedWeight = it }, label = { Text(label("Вес готового блюда, г", "Cooked weight, g")) }, modifier = Modifier.fillMaxWidth())
        if (initialRecipe == null) OutlinedTextField(portion, { portion = it }, label = { Text(label("Съеденная порция, г", "Serving eaten, g")) }, modifier = Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(enabled = !saving, modifier = Modifier.fillMaxWidth(), onClick = {
            val weight = parseNumber(cookedWeight)
            val amount = parseNumber(portion)
            if (name.isBlank() || ingredients.isEmpty() || weight == null || (initialRecipe == null && amount == null)) {
                error = label("Укажите название, ингредиенты, вес и порцию.", "Enter the name, ingredients, cooked weight and serving.")
            } else {
                val food = calculateRecipe(name, ingredients, weight).copy(id = initialRecipe?.food?.id ?: 0)
                val entry = amount?.takeIf { initialRecipe == null }?.let { serving -> DiaryEntry(
                    date = date, meal = meal, name = food.name, amount = serving, unit = food.unit,
                    calories = PortionCalculator.scale(food.caloriesPer100, serving),
                    protein = PortionCalculator.scale(food.proteinPer100, serving),
                    fat = PortionCalculator.scale(food.fatPer100, serving),
                    carbohydrate = PortionCalculator.scale(food.carbohydratePer100, serving),
                ) }
                saving = true
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) { saveRecipe(food, weight, ingredients, entry) }
                        onSaved()
                    } catch (exception: kotlinx.coroutines.CancellationException) {
                        throw exception
                    } catch (_: Exception) {
                        saving = false
                        error = label("Не удалось сохранить рецепт.", "Could not save the recipe.")
                    }
                }
            }
        }) { Text(if (initialRecipe == null) label("Сохранить и добавить в дневник", "Save and add to diary") else label("Сохранить изменения", "Save changes")) }
    }
}
