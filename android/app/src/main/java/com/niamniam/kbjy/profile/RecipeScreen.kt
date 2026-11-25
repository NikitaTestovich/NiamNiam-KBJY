package com.niamniam.kbjy.profile

import com.niamniam.kbjy.formatDecimal
import com.niamniam.kbjy.displayUnit

import com.niamniam.kbjy.BackButton

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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.niamniam.kbjy.PortionCalculator
import com.niamniam.kbjy.data.DiaryDao
import com.niamniam.kbjy.data.DiaryEntry
import com.niamniam.kbjy.data.Food
import com.niamniam.kbjy.data.StarterCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class RecipeIngredient(val food: Food, val amount: Double)
data class ExistingRecipe(val food: Food, val cookedWeight: Double, val ingredients: List<RecipeIngredient>)

private val ingredientsSaver = Saver<List<RecipeIngredient>, String>(
    save = { ingredients ->
        JSONArray().apply {
            ingredients.forEach { ingredient ->
                val food = ingredient.food
                put(JSONObject().put("id", food.id).put("name", food.name).put("unit", food.unit)
                    .put("calories", food.caloriesPer100).put("protein", food.proteinPer100)
                    .put("fat", food.fatPer100).put("carbohydrate", food.carbohydratePer100)
                    .put("amount", ingredient.amount))
            }
        }.toString()
    },
    restore = { saved ->
        val array = JSONArray(saved)
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            RecipeIngredient(
                Food(id = item.getLong("id"), name = item.getString("name"), unit = item.getString("unit"),
                    caloriesPer100 = item.getDouble("calories"), proteinPer100 = item.getDouble("protein"),
                    fatPer100 = item.getDouble("fat"), carbohydratePer100 = item.getDouble("carbohydrate")),
                item.getDouble("amount"),
            )
        }
    },
)

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
    starterFoods: List<Food> = emptyList(),
    allowDiaryEntry: Boolean = true,
    saveRecipe: suspend (Food, Double, List<RecipeIngredient>, DiaryEntry?) -> Unit,
) {
    val foods by dao.observeFoods().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val russian = language == AppLanguage.RUSSIAN
    fun label(ru: String, en: String) = if (russian) ru else en
    fun parseNumber(value: String) = value.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 }
    var name by rememberSaveable(initialRecipe?.food?.id) { mutableStateOf(initialRecipe?.food?.name.orEmpty()) }
    var search by rememberSaveable { mutableStateOf("") }
    var selection by rememberSaveable(stateSaver = ingredientsSaver) { mutableStateOf<List<RecipeIngredient>>(emptyList()) }
    val selected = selection.firstOrNull()?.food
    var ingredientAmount by rememberSaveable { mutableStateOf("") }
    var ingredients by rememberSaveable(initialRecipe?.food?.id, stateSaver = ingredientsSaver) { mutableStateOf(initialRecipe?.ingredients.orEmpty()) }
    val originalCookedWeight = initialRecipe?.cookedWeight
    val displayCookedWeight = originalCookedWeight?.let { formatDecimal(it, !russian) }.orEmpty()
    var cookedWeight by rememberSaveable(initialRecipe?.food?.id) { mutableStateOf(displayCookedWeight) }
    var portion by rememberSaveable { mutableStateOf("") }
    val showServing = allowDiaryEntry && initialRecipe == null
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BackButton(english = !russian, enabled = !saving, onClick = onBack)
        Text(if (initialRecipe == null) label("Создать рецепт", "Create recipe") else label("Изменить рецепт", "Edit recipe"), style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(name, { name = it }, label = { Text(label("Название блюда", "Recipe name")) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(search, { search = it }, label = { Text(label("Найти ингредиент", "Find an ingredient")) }, modifier = Modifier.fillMaxWidth())
        (foods + starterFoods).filter { search.isNotBlank() && StarterCatalog.matches(it.name, search) }.take(12).forEach { food ->
            OutlinedButton(onClick = { selection = listOf(RecipeIngredient(food, 0.0)) }) {
                Text("${StarterCatalog.displayName(food.name, !russian)} · ${displayUnit(food.unit, !russian)}")
            }
        }
        selected?.let { food ->
            Text(label("Ингредиент: ", "Ingredient: ") + StarterCatalog.displayName(food.name, !russian))
            OutlinedTextField(
                ingredientAmount, { ingredientAmount = it },
                label = { Text(label("Количество", "Amount") + " (${displayUnit(food.unit, !russian)})") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(onClick = {
                val amount = parseNumber(ingredientAmount)
                if (amount == null) {
                    error = label("Введите количество ингредиента.", "Enter an ingredient amount.")
                } else {
                    ingredients = ingredients + RecipeIngredient(food, amount)
                    ingredientAmount = ""
                    selection = emptyList()
                    search = ""
                    error = null
                }
            }) { Text(label("Добавить ингредиент", "Add ingredient")) }
        }
        ingredients.forEachIndexed { index, ingredient ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${StarterCatalog.displayName(ingredient.food.name, !russian)}: ${formatDecimal(ingredient.amount, !russian)} ${displayUnit(ingredient.food.unit, !russian)}", modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { ingredients = ingredients.filterIndexed { i, _ -> i != index } }) {
                    Text(label("Удалить", "Remove"))
                }
            }
        }
        OutlinedTextField(cookedWeight, { cookedWeight = it }, label = { Text(label("Вес готового блюда, г", "Cooked weight, g")) }, modifier = Modifier.fillMaxWidth())
        if (showServing) OutlinedTextField(portion, { portion = it }, label = { Text(label("Съеденная порция, г (необязательно)", "Serving eaten, g (optional)")) }, modifier = Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(enabled = !saving, modifier = Modifier.fillMaxWidth(), onClick = {
            val weight = if (cookedWeight == displayCookedWeight) originalCookedWeight else parseNumber(cookedWeight)
            val amount = parseNumber(portion)
            if (name.isBlank() || ingredients.isEmpty() || weight == null || (showServing && portion.isNotBlank() && amount == null)) {
                error = if (showServing) label("Проверьте название, ингредиенты, вес и порцию.", "Check the name, ingredients, cooked weight and serving.")
                    else label("Проверьте название, ингредиенты и вес готового блюда.", "Check the name, ingredients and cooked weight.")
            } else {
                val food = calculateRecipe(name, ingredients, weight).copy(
                    id = initialRecipe?.food?.id ?: 0,
                    defaultPortion = initialRecipe?.food?.defaultPortion,
                    defaultIsDrink = initialRecipe?.food?.defaultIsDrink ?: false,
                )
                val entry = amount?.takeIf { showServing }?.let { serving -> DiaryEntry(
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
        }) { Text(if (showServing && portion.isNotBlank()) label("Сохранить и добавить в дневник", "Save and add to diary") else label("Сохранить рецепт", "Save recipe")) }
    }
}
