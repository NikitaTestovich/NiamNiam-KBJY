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
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.niamniam.kbjy.GoalCalculator
import java.time.LocalDate

@Composable
fun NiamNiamTheme(theme: AppTheme, content: @Composable () -> Unit) {
    val colors = when (theme) {
        AppTheme.LIGHT -> lightColorScheme(primary = Color(0xFF285444), background = Color(0xFFFFFDF7))
        AppTheme.DARK -> darkColorScheme(primary = Color(0xFFB8DBA5))
        AppTheme.FOREST -> lightColorScheme(primary = Color(0xFF285444), background = Color(0xFFE8F1E3), surface = Color(0xFFF2F8EC))
    }
    MaterialTheme(colorScheme = colors, content = content)
}

@Composable
fun ProfileScreen(
    initial: ProfileState?,
    onThemeChange: (AppTheme) -> Unit = {},
    onLanguageChange: (AppLanguage) -> Unit = {},
    onSave: (Profile, DailyGoal) -> Unit,
) {
    val original = initial?.profile
    val originalGoal = initial?.goals?.let { goalForDate(it, LocalDate.now()) }
    var language by rememberSaveable { mutableStateOf(original?.language ?: AppLanguage.RUSSIAN) }
    var theme by rememberSaveable { mutableStateOf(original?.theme ?: AppTheme.LIGHT) }
    var sex by rememberSaveable { mutableStateOf(original?.sex ?: Sex.MALE) }
    var mode by rememberSaveable { mutableStateOf(originalGoal?.macroMode ?: MacroMode.GRAMS) }
    var age by rememberSaveable { mutableStateOf(original?.age?.toString() ?: "") }
    var height by rememberSaveable { mutableStateOf(original?.heightCm?.toString() ?: "") }
    var weight by rememberSaveable { mutableStateOf(original?.weightKg?.toString() ?: "") }
    var activity by rememberSaveable { mutableStateOf(original?.activityFactor?.toString() ?: "1.2") }
    var calories by rememberSaveable { mutableStateOf(originalGoal?.calories?.toString() ?: "") }
    fun initialMacro(value: Double?, kcalPerGram: Int): String {
        if (value == null || originalGoal == null) return ""
        val result = if (originalGoal.macroMode == MacroMode.PERCENT) value * kcalPerGram * 100 / originalGoal.calories else value
        return "%.4f".format(java.util.Locale.US, result).trimEnd('0').trimEnd('.')
    }
    var protein by rememberSaveable { mutableStateOf(initialMacro(originalGoal?.protein, 4)) }
    var fat by rememberSaveable { mutableStateOf(initialMacro(originalGoal?.fat, 9)) }
    var carbs by rememberSaveable { mutableStateOf(initialMacro(originalGoal?.carbohydrate, 4)) }
    var water by rememberSaveable { mutableStateOf(originalGoal?.waterMl?.toString() ?: "") }
    var error by remember { mutableStateOf(false) }
    val ru = language == AppLanguage.RUSSIAN
    fun label(russian: String, english: String) = if (ru) russian else english
    fun number(text: String): Double? = text.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label("Профиль", "Profile"), style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { language = AppLanguage.RUSSIAN; onLanguageChange(language) }) { Text("Русский") }
            OutlinedButton(onClick = { language = AppLanguage.ENGLISH; onLanguageChange(language) }) { Text("English") }
        }
        Text(label("Тема", "Theme"))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            AppTheme.entries.forEach { item ->
                OutlinedButton(onClick = { theme = item; onThemeChange(item) }) {
                    Text(when (item) {
                        AppTheme.LIGHT -> label("Светлая", "Light")
                        AppTheme.DARK -> label("Тёмная", "Dark")
                        AppTheme.FOREST -> label("Лесная", "Forest")
                    })
                }
            }
        }
        Text(label("Пол для расчёта", "Sex for calculation"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { sex = Sex.MALE }) { Text(label("Мужской", "Male")) }
            OutlinedButton(onClick = { sex = Sex.FEMALE }) { Text(label("Женский", "Female")) }
        }
        Text(label("Выбрано: ", "Selected: ") + if (sex == Sex.MALE) label("мужской", "male") else label("женский", "female"))
        ProfileField(label("Возраст", "Age"), age) { age = it }
        ProfileField(label("Рост, см", "Height, cm"), height) { height = it }
        ProfileField(label("Вес, кг", "Weight, kg"), weight) { weight = it }
        Text(label("Уровень обычной активности", "Usual activity level"))
        listOf(
            Triple("1.2", "Малоподвижный", "Sedentary"),
            Triple("1.375", "Лёгкая активность", "Light activity"),
            Triple("1.55", "Умеренная активность", "Moderate activity"),
            Triple("1.725", "Высокая активность", "High activity"),
        ).forEach { (factor, russian, english) ->
            OutlinedButton(onClick = { activity = factor }) {
                Text((if (activity == factor) "✓ " else "") + label(russian, english))
            }
        }
        ProfileField(label("Коэффициент активности", "Activity factor"), activity) { activity = it }
        val estimated = number(weight)?.let { w ->
            number(height)?.let { h -> age.toIntOrNull()?.let { a -> estimateRestingEnergy(w, h, a, sex) } }
        }
        if (estimated != null && number(activity)?.let { it > 0 } == true) {
            val maintenance = estimated * number(activity)!!
            Text(label("Расчёт поддержания: ", "Estimated maintenance: ") + "%.0f".format(maintenance) + " kcal")
            OutlinedButton(onClick = { calories = "%.0f".format(java.util.Locale.US, maintenance) }) {
                Text(label("Использовать оценку", "Use estimate"))
            }
            OutlinedButton(onClick = { calories = "%.0f".format(java.util.Locale.US, maintenance * 0.9) }) {
                Text(label("Снижение веса: −10%", "Weight loss: −10%"))
            }
            OutlinedButton(onClick = { calories = "%.0f".format(java.util.Locale.US, maintenance * 1.1) }) {
                Text(label("Набор веса: +10%", "Weight gain: +10%"))
            }
            Text(label("Это приблизительные ориентиры, цель можно изменить.", "These are estimates. You can edit the goal."))
        } else if (age.toIntOrNull()?.let { it !in 19..78 } == true) {
            Text(label("Расчёт доступен для возраста 19–78 лет. Введите цель вручную.", "Estimates are available for ages 19–78. Enter your goal manually."))
        }
        ProfileField(label("Цель, ккал", "Goal, kcal"), calories) { calories = it }
        Text(label("БЖУ — граммы или проценты", "Macros — grams or percentages"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { if (mode != MacroMode.GRAMS) { protein = ""; fat = ""; carbs = "" }; mode = MacroMode.GRAMS }) { Text(label("Граммы", "Grams")) }
            OutlinedButton(onClick = { if (mode != MacroMode.PERCENT) { protein = ""; fat = ""; carbs = "" }; mode = MacroMode.PERCENT }) { Text("%") }
        }
        Text(label("Выбрано: ", "Selected: ") + if (mode == MacroMode.GRAMS) label("граммы", "grams") else "%")
        OutlinedButton(onClick = {
            mode = MacroMode.PERCENT
            protein = "20"
            fat = "30"
            carbs = "50"
        }) { Text(label("Предложить БЖУ: 20 / 30 / 50 %", "Suggested macros: 20 / 30 / 50 %")) }
        ProfileField(label("Белки", "Protein"), protein) { protein = it }
        ProfileField(label("Жиры", "Fat"), fat) { fat = it }
        ProfileField(label("Углеводы", "Carbohydrates"), carbs) { carbs = it }
        val pPreview = number(protein)
        val fPreview = number(fat)
        val cPreview = number(carbs)
        val kcalPreview = number(calories)
        if (mode == MacroMode.PERCENT && kcalPreview != null && kcalPreview > 0 &&
            pPreview != null && fPreview != null && cPreview != null &&
            pPreview >= 0 && fPreview >= 0 && cPreview >= 0 &&
            kotlin.math.abs(pPreview + fPreview + cPreview - 100.0) < 0.0001) {
            val preview = GoalCalculator.macros(kcalPreview, pPreview, fPreview, cPreview)
            Text(label("В граммах: ", "In grams: ") +
                "%.1f / %.1f / %.1f".format(preview.protein, preview.fat, preview.carbohydrate))
        }
        ProfileField(label("Цель жидкости, мл (необязательно)", "Water goal, ml (optional)"), water) { water = it }
        if (error) Text(label("Проверьте значения и сумму процентов.", "Check the values and percentage total."), color = MaterialTheme.colorScheme.error)
        Button(onClick = {
            val a = age.toIntOrNull()
            val h = number(height)
            val w = number(weight)
            val act = number(activity)
            val c = number(calories)
            val p = number(protein)
            val f = number(fat)
            val b = number(carbs)
            val waterValue = if (water.isBlank()) null else number(water)
            if (a == null || a <= 0 || h == null || h <= 0 || w == null || w <= 0 ||
                act == null || act <= 0 || c == null || c <= 0 || p == null || p < 0 ||
                f == null || f < 0 || b == null || b < 0 ||
                (water.isNotBlank() && (waterValue == null || waterValue <= 0)) ||
                (mode == MacroMode.PERCENT && kotlin.math.abs(p + f + b - 100.0) >= 0.0001)) {
                error = true
            } else {
                val macros = if (mode == MacroMode.PERCENT) GoalCalculator.macros(c, p, f, b) else com.niamniam.kbjy.MacroGoals(p, f, b)
                onSave(Profile(sex, a, h, w, act, language, theme), DailyGoal(c, macros.protein, macros.fat, macros.carbohydrate, waterValue, mode))
            }
        }, modifier = Modifier.fillMaxWidth()) { Text(label("Сохранить", "Save")) }
    }
}

@Composable
private fun ProfileField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth())
}
