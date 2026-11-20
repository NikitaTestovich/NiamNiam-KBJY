package com.niamniam.kbjy.scanner

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment

enum class ManualFoodUnit { GRAMS, MILLILITERS }

data class ManualFoodEntry(
    val name: String,
    val unit: ManualFoodUnit,
    val caloriesPer100: Double,
    val proteinPer100: Double,
    val fatPer100: Double,
    val carbsPer100: Double,
    val portion: Double,
)

@Composable
fun ManualFoodScreen(
    onSave: (ManualFoodEntry) -> Unit,
    modifier: Modifier = Modifier,
    english: Boolean = false,
    recognized: ParsedLabel? = null,
) {
    var name by rememberSaveable { mutableStateOf("") }
    val usable = recognized?.takeIf { it.basis == LabelBasis.PER_100_GRAMS || it.basis == LabelBasis.PER_100_MILLILITERS }
    var calories by rememberSaveable { mutableStateOf(usable?.calories?.toString() ?: "") }
    var protein by rememberSaveable { mutableStateOf(usable?.protein?.toString() ?: "") }
    var fat by rememberSaveable { mutableStateOf(usable?.fat?.toString() ?: "") }
    var carbs by rememberSaveable { mutableStateOf(usable?.carbs?.toString() ?: "") }
    var portion by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf(if (usable?.basis == LabelBasis.PER_100_MILLILITERS) ManualFoodUnit.MILLILITERS else ManualFoodUnit.GRAMS) }
    var attempted by rememberSaveable { mutableStateOf(false) }

    fun number(value: String): Double? =
        value.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }

    val calorieValue = number(calories)
    val proteinValue = number(protein)
    val fatValue = number(fat)
    val carbsValue = number(carbs)
    val portionValue = number(portion)?.takeIf { it > 0 }
    val valid = name.isNotBlank() && calorieValue != null && proteinValue != null &&
        fatValue != null && carbsValue != null && portionValue != null
    val gramUnit = if (english) "g" else "г"
    val mlUnit = if (english) "ml" else "мл"
    val currentUnit = if (unit == ManualFoodUnit.GRAMS) gramUnit else mlUnit

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(if (english) "Add food" else "Добавить продукт", style = MaterialTheme.typography.headlineSmall)
        if (recognized != null) Text(if (english) "Check every recognized value against the label." else "Проверьте все распознанные значения по этикетке.")
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(if (english) "Name" else "Название") },
            isError = attempted && name.isBlank(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(if (english) "Values per 100 $currentUnit" else "Пищевая ценность на 100 $currentUnit")
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = unit == ManualFoodUnit.GRAMS, onClick = { unit = ManualFoodUnit.GRAMS })
            Text(gramUnit)
            RadioButton(selected = unit == ManualFoodUnit.MILLILITERS, onClick = { unit = ManualFoodUnit.MILLILITERS })
            Text(mlUnit)
        }
        NumberField(calories, { calories = it }, if (english) "Calories, kcal" else "Калории, ккал", attempted && calorieValue == null)
        NumberField(protein, { protein = it }, if (english) "Protein, g" else "Белки, г", attempted && proteinValue == null)
        NumberField(fat, { fat = it }, if (english) "Fat, g" else "Жиры, г", attempted && fatValue == null)
        NumberField(carbs, { carbs = it }, if (english) "Carbohydrates, g" else "Углеводы, г", attempted && carbsValue == null)
        NumberField(portion, { portion = it }, if (english) "Portion, $currentUnit" else "Порция, $currentUnit", attempted && portionValue == null)
        if (attempted && !valid) {
            Text(
                if (english) "Enter a name, non-negative nutrition values and a portion greater than zero."
                else "Укажите название, неотрицательные значения и порцию больше нуля.",
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(onClick = {
            attempted = true
            if (valid) {
                onSave(ManualFoodEntry(name.trim(), unit, calorieValue!!, proteinValue!!, fatValue!!, carbsValue!!, portionValue!!))
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
