package com.niamniam.kbjy.scanner

import com.niamniam.kbjy.formatDecimal
import com.niamniam.kbjy.data.StarterCatalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import com.niamniam.kbjy.data.Food

enum class ManualFoodUnit { GRAMS, MILLILITERS }

data class ManualFoodEntry(
    val name: String,
    val unit: ManualFoodUnit,
    val caloriesPer100: Double,
    val proteinPer100: Double,
    val fatPer100: Double,
    val carbsPer100: Double,
    val portion: Double,
    val isDrink: Boolean,
)

@Composable
fun ManualFoodScreen(
    onSave: (ManualFoodEntry) -> Unit,
    modifier: Modifier = Modifier,
    english: Boolean = false,
    recognized: ParsedLabel? = null,
    catalogOnly: Boolean = false,
    initialFood: Food? = null,
    onRecognitionApplied: () -> Unit = {},
) {
    val initialName = initialFood?.let { StarterCatalog.displayName(it.name, english) }.orEmpty()
    var name by rememberSaveable { mutableStateOf(initialName) }
    val usable = recognized?.takeIf { it.basis != null }
    fun display(value: Double?) = value?.let { formatDecimal(it, english) }.orEmpty()
    var perServing by rememberSaveable { mutableStateOf(usable?.basis == LabelBasis.PER_SERVING) }
    var labelServingSize by rememberSaveable { mutableStateOf("") }
    var calories by rememberSaveable { mutableStateOf(display(usable?.calories ?: initialFood?.caloriesPer100)) }
    var protein by rememberSaveable { mutableStateOf(display(usable?.protein ?: initialFood?.proteinPer100)) }
    var fat by rememberSaveable { mutableStateOf(display(usable?.fat ?: initialFood?.fatPer100)) }
    var carbs by rememberSaveable { mutableStateOf(display(usable?.carbs ?: initialFood?.carbohydratePer100)) }
    var portion by rememberSaveable { mutableStateOf(initialFood?.defaultPortion?.let(::display) ?: if (catalogOnly) "100" else "") }
    var unit by rememberSaveable {
        val milliliters = usable?.basis == LabelBasis.PER_100_MILLILITERS || initialFood?.unit in listOf("мл", "ml")
        mutableStateOf(if (milliliters) ManualFoodUnit.MILLILITERS else ManualFoodUnit.GRAMS)
    }
    var isDrink by rememberSaveable { mutableStateOf(initialFood?.defaultIsDrink ?: (usable?.basis == LabelBasis.PER_100_MILLILITERS)) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    var showRecognitionNotice by rememberSaveable { mutableStateOf(recognized != null) }

    LaunchedEffect(recognized) {
        val label = recognized ?: return@LaunchedEffect
        showRecognitionNotice = true
        if (label.basis != null) {
            perServing = label.basis == LabelBasis.PER_SERVING
            when (label.basis) {
                LabelBasis.PER_100_GRAMS -> { unit = ManualFoodUnit.GRAMS; isDrink = false }
                LabelBasis.PER_100_MILLILITERS -> { unit = ManualFoodUnit.MILLILITERS; isDrink = true }
                LabelBasis.PER_SERVING -> Unit
            }
            label.calories?.let { calories = display(it) }
            label.protein?.let { protein = display(it) }
            label.fat?.let { fat = display(it) }
            label.carbs?.let { carbs = display(it) }
        }
        onRecognitionApplied()
    }

    fun number(value: String): Double? =
        value.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }

    fun read(text: String, original: Double?) = original?.takeIf { text == display(it) } ?: number(text)
    val stored = initialFood.takeUnless { showRecognitionNotice || perServing }
    val calorieValue = read(calories, stored?.caloriesPer100)
    val proteinValue = read(protein, stored?.proteinPer100)
    val fatValue = read(fat, stored?.fatPer100)
    val carbsValue = read(carbs, stored?.carbohydratePer100)
    val portionValue = read(portion, initialFood?.defaultPortion)?.takeIf { it > 0 }
    val labelSize = number(labelServingSize)?.takeIf { it > 0 }
    val multiplier = if (perServing && labelSize != null) 100.0 / labelSize else 1.0
    val valid = name.isNotBlank() && calorieValue != null && proteinValue != null &&
        fatValue != null && carbsValue != null && portionValue != null &&
        (!perServing || labelSize != null) && multiplier.isFinite() &&
        listOfNotNull(calorieValue, proteinValue, fatValue, carbsValue).all { (it * multiplier).isFinite() }
    val gramUnit = if (english) "g" else "г"
    val mlUnit = if (english) "ml" else "мл"
    val currentUnit = if (unit == ManualFoodUnit.GRAMS) gramUnit else mlUnit

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val title = if (initialFood == null) {
            if (english) "Add food" else "Добавить продукт"
        } else if (english) "Edit food" else "Изменить продукт"
        Text(title, style = MaterialTheme.typography.headlineSmall)
        if (showRecognitionNotice) Text(if (english) "Check every recognized value against the label." else "Проверьте все распознанные значения по этикетке.")
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(if (english) "Name" else "Название") },
            isError = attempted && name.isBlank(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = !perServing, onClick = { perServing = false })
            Text(if (english) "Per 100" else "На 100")
            RadioButton(selected = perServing, onClick = { perServing = true })
            Text(if (english) "Per serving" else "На порцию")
        }
        Text(if (perServing) {
            if (english) "Values per labeled serving" else "Пищевая ценность на порцию с этикетки"
        } else if (english) "Values per 100 $currentUnit" else "Пищевая ценность на 100 $currentUnit")
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = unit == ManualFoodUnit.GRAMS, onClick = { unit = ManualFoodUnit.GRAMS; isDrink = false })
            Text(gramUnit)
            RadioButton(selected = unit == ManualFoodUnit.MILLILITERS, onClick = { unit = ManualFoodUnit.MILLILITERS; isDrink = true })
            Text(mlUnit)
        }
        if (unit == ManualFoodUnit.MILLILITERS) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isDrink, onCheckedChange = { isDrink = it })
                Text(if (english) "Count as a drink" else "Учесть как напиток")
            }
        }
        if (perServing) {
            NumberField(labelServingSize, { labelServingSize = it },
                if (english) "Labeled serving size, $currentUnit" else "Размер порции с этикетки, $currentUnit",
                attempted && labelSize == null)
        }
        NumberField(calories, { calories = it }, if (english) "Calories, kcal" else "Калории, ккал", attempted && calorieValue == null)
        NumberField(protein, { protein = it }, if (english) "Protein, g" else "Белки, г", attempted && proteinValue == null)
        NumberField(fat, { fat = it }, if (english) "Fat, g" else "Жиры, г", attempted && fatValue == null)
        NumberField(carbs, { carbs = it }, if (english) "Carbohydrates, g" else "Углеводы, г", attempted && carbsValue == null)
        val portionLabel = when {
            catalogOnly && english -> "Usual portion, $currentUnit"
            catalogOnly -> "Обычная порция, $currentUnit"
            english -> "Portion, $currentUnit"
            else -> "Порция, $currentUnit"
        }
        NumberField(portion, { portion = it }, portionLabel, attempted && portionValue == null)
        if (attempted && !valid) {
            Text(
                if (perServing && labelSize == null) {
                    if (english) "Enter the labeled serving size." else "Укажите размер порции с этикетки."
                } else if (english) "Enter a name, non-negative nutrition values and a portion greater than zero."
                else "Укажите название, неотрицательные значения и порцию больше нуля.",
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(onClick = {
            attempted = true
            if (valid) {
                onSave(ManualFoodEntry(if (initialFood != null && name == initialName) initialFood.name else name.trim(), unit, calorieValue!! * multiplier, proteinValue!! * multiplier,
                    fatValue!! * multiplier, carbsValue!! * multiplier, portionValue!!, isDrink))
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text(if (english) "Save" else "Сохранить")
        }
    }
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, label: String, error: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        isError = error,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}
