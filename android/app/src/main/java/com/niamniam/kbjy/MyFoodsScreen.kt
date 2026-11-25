package com.niamniam.kbjy

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.niamniam.kbjy.data.Food
import com.niamniam.kbjy.data.StarterCatalog
import java.text.NumberFormat
import java.util.Locale

private enum class MyFoodsFilter(val russian: String, val english: String) {
    ALL("Все", "All"),
    PRODUCTS("Продукты", "Products"),
    RECIPES("Рецепты", "Recipes"),
}

@Composable
internal fun MyFoodsScreen(foods: List<Food>, english: Boolean, onBack: () -> Unit,
                           onNewFood: () -> Unit, onNewRecipe: () -> Unit,
                           recipeIds: Set<Long>? = emptySet(), onEditRecipe: (Long) -> Unit = {},
                           onEditFood: (Food) -> Unit = {}) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(MyFoodsFilter.ALL) }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selected = foods.firstOrNull { it.id == selectedId }
    val goBack = { if (selectedId == null) onBack() else selectedId = null }
    BackHandler(onBack = goBack)

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BackButton(english, onClick = goBack)
        if (selected != null) {
            val isRecipe = recipeIds?.contains(selected.id)
            val editLabel = when (isRecipe) {
                true -> if (english) "Edit recipe" else "Изменить рецепт"
                false -> if (english) "Edit food" else "Изменить продукт"
                null -> null
            }
            FoodNutritionCard(selected, english, editLabel) {
                if (isRecipe == true) onEditRecipe(selected.id) else onEditFood(selected)
            }
        } else {
            Text(if (english) "My foods" else "Мои блюда", style = MaterialTheme.typography.headlineLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onNewFood, modifier = Modifier.weight(1f)) {
                    Text(if (english) "New food" else "Новый продукт")
                }
                OutlinedButton(onClick = onNewRecipe, modifier = Modifier.weight(1f)) {
                    Text(if (english) "New recipe" else "Новый рецепт")
                }
            }
            if (foods.isEmpty()) {
                Text(if (english) "Saved foods and recipes will appear here." else "Здесь появятся сохранённые продукты и рецепты.")
            } else {
                OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text(if (english) "Search" else "Поиск") })
                if (recipeIds != null) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MyFoodsFilter.entries.forEach { option ->
                            FilterChip(selected = filter == option, onClick = { filter = option },
                                label = { Text(if (english) option.english else option.russian) })
                        }
                    }
                }
                val matches = foods.filter { food ->
                    val matchesType = when (filter) {
                        MyFoodsFilter.ALL -> true
                        MyFoodsFilter.PRODUCTS -> recipeIds?.contains(food.id) == false
                        MyFoodsFilter.RECIPES -> recipeIds?.contains(food.id) == true
                    }
                    matchesType && StarterCatalog.matches(food.name, query)
                }
                LazyColumn {
                    if (matches.isEmpty()) item { Text(if (english) "No matches" else "Ничего не найдено") }
                    items(matches, key = { it.id }) { food ->
                        TextButton(onClick = { selectedId = food.id }, modifier = Modifier.fillMaxWidth()) { Text(StarterCatalog.displayName(food.name, english)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodNutritionCard(food: Food, english: Boolean, editLabel: String?, onEdit: () -> Unit) {
    val formatter = NumberFormat.getNumberInstance(if (english) Locale.ENGLISH else Locale.forLanguageTag("ru"))
        .apply { maximumFractionDigits = 2 }
    val unit = displayUnit(food.unit, english)
    val grams = if (english) "g" else "г"
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text(StarterCatalog.displayName(food.name, english), style = MaterialTheme.typography.headlineLarge) }
        item { Text(if (english) "Nutrition per 100 $unit" else "Пищевая ценность на 100 $unit") }
        item {
            val calories = formatter.format(food.caloriesPer100)
            Text(if (english) "Calories: $calories kcal" else "Калории: $calories ккал")
        }
        item { Text("${if (english) "Protein" else "Белки"}: ${formatter.format(food.proteinPer100)} $grams") }
        item { Text("${if (english) "Fat" else "Жиры"}: ${formatter.format(food.fatPer100)} $grams") }
        item { Text("${if (english) "Carbohydrates" else "Углеводы"}: ${formatter.format(food.carbohydratePer100)} $grams") }
        if (editLabel != null) item {
            OutlinedButton(onClick = onEdit) {
                Text(editLabel)
            }
        }
    }
}
