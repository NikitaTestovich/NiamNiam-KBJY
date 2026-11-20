package com.niamniam.kbjy.diary

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.niamniam.kbjy.data.DiaryEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HistoryScreen(
    date: LocalDate,
    entries: List<DiaryEntry>,
    onDateSelected: (LocalDate) -> Unit,
    onDelete: (Long) -> Unit,
    onAdd: (LocalDate) -> Unit,
    onUpdate: (DiaryEntry) -> Unit,
    modifier: Modifier = Modifier,
    english: Boolean = false,
) {
    val context = LocalContext.current
    var pendingDelete by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val dayEntries = entries.filter { it.date == date.toString() }
    val locale = if (english) Locale.ENGLISH else Locale.forLanguageTag("ru")
    val formatter = DateTimeFormatter.ofPattern(if (english) "MMM d, yyyy" else "d MMM yyyy", locale)

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(if (english) "History" else "История", style = MaterialTheme.typography.headlineSmall)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = { onDateSelected(date.minusDays(1)) }) { Text("‹") }
            TextButton(onClick = {
                DatePickerDialog(
                    context,
                    { _, year, month, day -> onDateSelected(LocalDate.of(year, month + 1, day)) },
                    date.year,
                    date.monthValue - 1,
                    date.dayOfMonth,
                ).show()
            }) { Text(date.format(formatter)) }
            OutlinedButton(onClick = { onDateSelected(date.plusDays(1)) }) { Text("›") }
        }
        Text(
            (if (english) "%.0f kcal · P %.1f g · F %.1f g · C %.1f g" else "%.0f ккал · Б %.1f г · Ж %.1f г · У %.1f г")
                .format(dayEntries.sumOf { it.calories }, dayEntries.sumOf { it.protein }, dayEntries.sumOf { it.fat }, dayEntries.sumOf { it.carbohydrate })
        )
        Text((if (english) "Fluid: %.0f ml" else "Жидкость: %.0f мл").format(dayEntries.sumOf { it.drinkMl }))
        TextButton(onClick = { onAdd(date) }) { Text(if (english) "Add entry" else "Добавить запись") }
        if (dayEntries.isEmpty()) {
            Text(if (english) "No entries for this day" else "За этот день нет записей")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(dayEntries, key = { it.id }) { entry ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(entry.name, style = MaterialTheme.typography.titleMedium)
                        val meal = if (english && entry.meal == "Обед") "Lunch" else entry.meal
                        val unit = when (entry.unit) {
                            "г", "g" -> if (english) "g" else "г"
                            "мл", "ml" -> if (english) "ml" else "мл"
                            else -> entry.unit
                        }
                        Text("$meal · ${entry.amount} $unit")
                        Text(
                            if (english) "${entry.calories} kcal · P ${entry.protein} g · F ${entry.fat} g · C ${entry.carbohydrate} g"
                            else "${entry.calories} ккал · Б ${entry.protein} г · Ж ${entry.fat} г · У ${entry.carbohydrate} г"
                        )
                        if (entry.drinkMl > 0) {
                            Text(if (english) "Fluid: ${entry.drinkMl} ml" else "Жидкость: ${entry.drinkMl} мл")
                        }
                        TextButton(onClick = { editingId = entry.id }) {
                            Text(if (english) "Edit" else "Изменить")
                        }
                        TextButton(onClick = { pendingDelete = entry.id }) {
                            Text(if (english) "Delete" else "Удалить")
                        }
                    }
                }
            }
        }
    }
    dayEntries.find { it.id == editingId }?.let { entry ->
        EditEntryDialog(entry = entry, english = english, onDismiss = { editingId = null }, onSave = {
            editingId = null
            onUpdate(it)
        })
    }
    pendingDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(if (english) "Delete entry?" else "Удалить запись?") },
            confirmButton = {
                Button(onClick = { pendingDelete = null; onDelete(id) }) {
                    Text(if (english) "Delete" else "Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(if (english) "Cancel" else "Отмена")
                }
            },
        )
    }
}

@Composable
private fun EditEntryDialog(entry: DiaryEntry, english: Boolean, onDismiss: () -> Unit, onSave: (DiaryEntry) -> Unit) {
    var name by rememberSaveable(entry.id) { mutableStateOf(entry.name) }
    var amount by rememberSaveable(entry.id) { mutableStateOf(entry.amount.toString()) }
    var calories by rememberSaveable(entry.id) { mutableStateOf(entry.calories.toString()) }
    var protein by rememberSaveable(entry.id) { mutableStateOf(entry.protein.toString()) }
    var fat by rememberSaveable(entry.id) { mutableStateOf(entry.fat.toString()) }
    var carbs by rememberSaveable(entry.id) { mutableStateOf(entry.carbohydrate.toString()) }
    var fluid by rememberSaveable(entry.id) { mutableStateOf(entry.drinkMl.toString()) }
    var previousAmount by rememberSaveable(entry.id) { mutableStateOf(entry.amount) }
    fun number(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
    fun changeAmount(text: String) {
        amount = text
        val nextAmount = number(text)?.takeIf { it > 0 } ?: return
        if (previousAmount <= 0) return
        val scale = nextAmount / previousAmount
        if (!scale.isFinite()) return
        calories = number(calories)?.let { (it * scale).toString() } ?: calories
        protein = number(protein)?.let { (it * scale).toString() } ?: protein
        fat = number(fat)?.let { (it * scale).toString() } ?: fat
        carbs = number(carbs)?.let { (it * scale).toString() } ?: carbs
        fluid = number(fluid)?.let { (it * scale).toString() } ?: fluid
        previousAmount = nextAmount
    }
    val quantity = number(amount)?.takeIf { it > 0 }
    val kcal = number(calories)
    val proteins = number(protein)
    val fats = number(fat)
    val carbohydrates = number(carbs)
    val volume = number(fluid)
    val valid = name.isNotBlank() && quantity != null && kcal != null && proteins != null && fats != null && carbohydrates != null && volume != null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (english) "Edit entry" else "Изменить запись") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (english) "Enter nutrition values for this portion." else "Укажите КБЖУ для этой порции.")
                OutlinedTextField(name, { name = it }, label = { Text(if (english) "Name" else "Название") })
                OutlinedTextField(amount, { changeAmount(it) }, label = { Text((if (english) "Amount" else "Количество") + " (${entry.unit})") })
                OutlinedTextField(calories, { calories = it }, label = { Text(if (english) "Calories, kcal" else "Калории, ккал") })
                OutlinedTextField(protein, { protein = it }, label = { Text(if (english) "Protein, g" else "Белки, г") })
                OutlinedTextField(fat, { fat = it }, label = { Text(if (english) "Fat, g" else "Жиры, г") })
                OutlinedTextField(carbs, { carbs = it }, label = { Text(if (english) "Carbohydrates, g" else "Углеводы, г") })
                OutlinedTextField(fluid, { fluid = it }, label = { Text(if (english) "Fluid, ml" else "Жидкость, мл") })
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onSave(entry.copy(name = name.trim(), amount = quantity!!, calories = kcal!!,
                    protein = proteins!!, fat = fats!!, carbohydrate = carbohydrates!!, drinkMl = volume!!))
            }) { Text(if (english) "Save" else "Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (english) "Cancel" else "Отмена") } },
    )
}
