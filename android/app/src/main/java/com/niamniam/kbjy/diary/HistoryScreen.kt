package com.niamniam.kbjy.diary

import com.niamniam.kbjy.formatDecimal
import com.niamniam.kbjy.data.StarterCatalog
import com.niamniam.kbjy.data.hasNoNutrition
import com.niamniam.kbjy.data.isWaterEntry
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.niamniam.kbjy.data.DiaryEntry
import com.niamniam.kbjy.macroColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(
    date: LocalDate,
    entries: List<DiaryEntry>,
    onDateSelected: (LocalDate) -> Unit,
    onDelete: suspend (Long) -> Unit,
    onAdd: (LocalDate) -> Unit,
    onUpdate: suspend (DiaryEntry) -> Unit,
    modifier: Modifier = Modifier,
    english: Boolean = false,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingDelete by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf(false) }
    val dayEntries = entries.filter { it.date == date.toString() }
    val (waterEntries, mealEntries) = dayEntries.partition(DiaryEntry::isWaterEntry)
    val locale = if (english) Locale.ENGLISH else Locale.forLanguageTag("ru")
    fun number(value: Double) = "%.2f".format(locale, value)
    val formatter = DateTimeFormatter.ofPattern(if (english) "MMM d, yyyy" else "d MMM yyyy", locale)

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(if (english) "History" else "История", style = MaterialTheme.typography.headlineSmall)
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FilledTonalButton(onClick = {
                DatePickerDialog(
                    context,
                    { _, year, month, day -> onDateSelected(LocalDate.of(year, month + 1, day)) },
                    date.year,
                    date.monthValue - 1,
                    date.dayOfMonth,
                ).show()
            }) { Text(date.format(formatter), style = MaterialTheme.typography.titleMedium) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onDateSelected(date.minusDays(1)) }, modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp)) { Text(if (english) "← Previous day" else "← День назад") }
                OutlinedButton(onClick = { onDateSelected(date.plusDays(1)) }, modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp)) { Text(if (english) "Next day →" else "День вперёд →") }
            }
        }
        LazyColumn(Modifier.testTag("history-list"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (english) "Daily totals" else "Итоги за день", style = MaterialTheme.typography.titleMedium)
                val (proteinColor, fatColor, carbColor) = macroColors()
                val grams = if (english) "g" else "г"
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HistoryMetric(if (english) "Calories" else "Калории", number(dayEntries.sumOf { it.calories }) + if (english) " kcal" else " ккал",
                        MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    HistoryMetric(if (english) "Fluid" else "Жидкость", number(dayEntries.sumOf { it.drinkMl }) + if (english) " ml" else " мл",
                        MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HistoryMetric(if (english) "Protein" else "Белки", "${number(dayEntries.sumOf { it.protein })} $grams", proteinColor, Modifier.weight(1f))
                    HistoryMetric(if (english) "Fat" else "Жиры", "${number(dayEntries.sumOf { it.fat })} $grams", fatColor, Modifier.weight(1f))
                    HistoryMetric(if (english) "Carbs" else "Углеводы", "${number(dayEntries.sumOf { it.carbohydrate })} $grams", carbColor, Modifier.weight(1f))
                }
                Button(onClick = { onAdd(date) }, modifier = Modifier.fillMaxWidth()) { Text(if (english) "Add entry" else "Добавить запись") }
            }
          }
            if (dayEntries.isEmpty()) {
                item { Text(if (english) "No entries for this day" else "За этот день нет записей") }
            } else {
                val sections = if (waterEntries.isEmpty()) listOf(null to mealEntries) else listOf(
                    (if (english) "Water entries" else "Записи воды") to waterEntries,
                    (if (english) "Meals" else "Приёмы пищи") to mealEntries,
                )
                for ((heading, sectionEntries) in sections) {
                    if (sectionEntries.isEmpty()) continue
                    if (heading != null) item(key = heading) { Text(heading, style = MaterialTheme.typography.titleMedium) }
                    items(sectionEntries, key = { it.id }) { entry ->
                        HistoryEntryCard(entry, english, ::number, onEdit = { editingId = entry.id },
                            onDelete = { pendingDelete = entry.id })
                    }
                }
            }
        }
    }
    dayEntries.find { it.id == editingId }?.let { entry ->
        EditEntryDialog(entry = entry, english = english, busy = busy, error = error,
            onDismiss = { if (!busy) { editingId = null; error = false } },
            onSave = { updated ->
                if (!busy) {
                    busy = true
                    error = false
                    scope.launch {
                        try {
                            onUpdate(updated)
                            editingId = null
                        } catch (exception: CancellationException) {
                            throw exception
                        } catch (_: Exception) {
                            error = true
                        } finally {
                            busy = false
                        }
                    }
                }
            })
    }
    pendingDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { if (!busy) { pendingDelete = null; error = false } },
            title = { Text(if (english) "Delete entry?" else "Удалить запись?") },
            text = { if (error) Text(if (english) "Could not delete. Try again." else "Не удалось удалить. Попробуйте ещё раз.") },
            confirmButton = {
                Button(enabled = !busy, onClick = {
                    busy = true
                    error = false
                    scope.launch {
                        try {
                            onDelete(id)
                            pendingDelete = null
                        } catch (exception: CancellationException) {
                            throw exception
                        } catch (_: Exception) {
                            error = true
                        } finally {
                            busy = false
                        }
                    }
                }) {
                    Text(if (english) "Delete" else "Удалить")
                }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { pendingDelete = null; error = false }) {
                    Text(if (english) "Cancel" else "Отмена")
                }
            },
        )
    }
}

@Composable
private fun HistoryEntryCard(
    entry: DiaryEntry,
    english: Boolean,
    number: (Double) -> String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (entry.isWaterEntry()) {
                Text(if (english) "Water · ${number(entry.drinkMl)} ml" else "Вода · ${number(entry.drinkMl)} мл",
                    style = MaterialTheme.typography.titleMedium)
            } else {
                Text(StarterCatalog.displayName(entry.name, english), style = MaterialTheme.typography.titleMedium)
                val meal = when (entry.meal) {
                    "breakfast" -> if (english) "Breakfast" else "Завтрак"
                    "lunch", "Обед" -> if (english) "Lunch" else "Обед"
                    "dinner" -> if (english) "Dinner" else "Ужин"
                    "snack" -> if (english) "Snack" else "Перекус"
                    else -> entry.meal
                }
                val unit = when (entry.unit) {
                    "г", "g" -> if (english) "g" else "г"
                    "мл", "ml" -> if (english) "ml" else "мл"
                    else -> entry.unit
                }
                Text("$meal · ${number(entry.amount)} $unit")
                Text(
                    if (english) "${number(entry.calories)} kcal · P ${number(entry.protein)} g · F ${number(entry.fat)} g · C ${number(entry.carbohydrate)} g"
                    else "${number(entry.calories)} ккал · Б ${number(entry.protein)} г · Ж ${number(entry.fat)} г · У ${number(entry.carbohydrate)} г"
                )
                if (entry.drinkMl > 0) {
                    Text(if (english) "Fluid: ${number(entry.drinkMl)} ml" else "Жидкость: ${number(entry.drinkMl)} мл")
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onEdit, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp)) {
                    Text(if (english) "Edit" else "Изменить", maxLines = 1)
                }
                OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp)) {
                    Text(if (english) "Delete" else "Удалить", color = MaterialTheme.colorScheme.error, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun HistoryMetric(label: String, value: String, color: Color, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))) {
        Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = color)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun EditEntryDialog(entry: DiaryEntry, english: Boolean, busy: Boolean, error: Boolean, onDismiss: () -> Unit, onSave: (DiaryEntry) -> Unit) {
    if (entry.isWaterEntry() && entry.drinkMl > 0 && entry.hasNoNutrition()) {
        EditWaterDialog(entry, english, busy, error, onDismiss, onSave)
        return
    }
    val displayedName = StarterCatalog.displayName(entry.name, english)
    val displayedAmount = formatDecimal(entry.amount, english)
    var name by rememberSaveable(entry.id) { mutableStateOf(displayedName) }
    var amount by rememberSaveable(entry.id) { mutableStateOf(displayedAmount) }
    fun per100(value: Double): String = if (entry.amount > 0) formatDecimal(value / entry.amount * 100, english) else ""
    var calories by rememberSaveable(entry.id) { mutableStateOf(per100(entry.calories)) }
    var protein by rememberSaveable(entry.id) { mutableStateOf(per100(entry.protein)) }
    var fat by rememberSaveable(entry.id) { mutableStateOf(per100(entry.fat)) }
    var carbs by rememberSaveable(entry.id) { mutableStateOf(per100(entry.carbohydrate)) }
    fun number(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
    val quantity = (if (amount == displayedAmount) entry.amount else number(amount))?.takeIf { it > 0 }
    fun scaled(text: String, original: Double): Double? {
        val portion = quantity ?: return null
        if (text == per100(original) && entry.amount > 0) return (original * (portion / entry.amount)).takeIf { it.isFinite() }
        val value = number(text) ?: return null
        return (value / 100 * portion).takeIf { it.isFinite() }
    }
    val kcal = scaled(calories, entry.calories)
    val proteins = scaled(protein, entry.protein)
    val fats = scaled(fat, entry.fat)
    val carbohydrates = scaled(carbs, entry.carbohydrate)
    val volume = quantity?.let {
        when {
            entry.drinkMl == 0.0 -> 0.0
            entry.unit == "мл" || entry.unit == "ml" -> it
            entry.amount > 0 -> entry.drinkMl / entry.amount * it
            else -> Double.NaN
        }
    }?.takeIf { it.isFinite() && it >= 0 }
    val valid = name.isNotBlank() && quantity != null && kcal != null && proteins != null && fats != null && carbohydrates != null && volume != null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (english) "Edit entry" else "Изменить запись") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (english) "Nutrition values per 100 ${entry.unit}." else "Пищевая ценность на 100 ${entry.unit}.")
                OutlinedTextField(name, { name = it }, label = { Text(if (english) "Name" else "Название") })
                OutlinedTextField(amount, { amount = it }, label = { Text((if (english) "Amount" else "Количество") + " (${entry.unit})") })
                OutlinedTextField(calories, { calories = it }, label = { Text(if (english) "Calories, kcal" else "Калории, ккал") })
                OutlinedTextField(protein, { protein = it }, label = { Text(if (english) "Protein, g" else "Белки, г") })
                OutlinedTextField(fat, { fat = it }, label = { Text(if (english) "Fat, g" else "Жиры, г") })
                OutlinedTextField(carbs, { carbs = it }, label = { Text(if (english) "Carbohydrates, g" else "Углеводы, г") })
                if (entry.drinkMl > 0 && volume != null) {
                    Text((if (english) "Fluid: %.2f ml" else "Жидкость: %.2f мл").format(volume))
                }
                if (error) Text(if (english) "Could not save. Try again." else "Не удалось сохранить. Попробуйте ещё раз.")
            }
        },
        confirmButton = {
            TextButton(enabled = valid && !busy, onClick = {
                onSave(entry.copy(name = if (name == displayedName) entry.name else name.trim(), amount = quantity!!, calories = kcal!!,
                    protein = proteins!!, fat = fats!!, carbohydrate = carbohydrates!!, drinkMl = volume!!))
            }) { Text(if (english) "Save" else "Сохранить") }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text(if (english) "Cancel" else "Отмена") } },
    )
}

@Composable
private fun EditWaterDialog(
    entry: DiaryEntry,
    english: Boolean,
    busy: Boolean,
    error: Boolean,
    onDismiss: () -> Unit,
    onSave: (DiaryEntry) -> Unit,
) {
    val displayedVolume = formatDecimal(entry.drinkMl, english)
    var text by rememberSaveable(entry.id) { mutableStateOf(displayedVolume) }
    val volume = (if (text == displayedVolume) entry.drinkMl else text.trim().replace(',', '.').toDoubleOrNull())
        ?.takeIf { it.isFinite() && it > 0 }
    val amount = volume?.let {
        when {
            it == entry.drinkMl -> entry.amount
            entry.unit == "мл" || entry.unit == "ml" -> it
            else -> entry.amount * (it / entry.drinkMl)
        }
    }?.takeIf { it.isFinite() && it > 0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (english) "Edit water entry" else "Изменить запись воды") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(if (english) "Water volume, ml" else "Объём воды, мл") },
                    singleLine = true,
                    enabled = !busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                if (error) Text(if (english) "Could not save. Try again." else "Не удалось сохранить. Попробуйте ещё раз.",
                    color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(enabled = !busy && volume != null && amount != null, onClick = {
                onSave(entry.copy(drinkMl = checkNotNull(volume), amount = checkNotNull(amount)))
            }) { Text(if (english) "Save" else "Сохранить") }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text(if (english) "Cancel" else "Отмена") } },
    )
}
