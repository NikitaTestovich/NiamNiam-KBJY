package com.niamniam.kbjy.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.niamniam.kbjy.GoalCalculator
import com.niamniam.kbjy.formatDecimal
import java.time.LocalDate

@Composable
fun ProfileScreen(
    initial: ProfileState?,
    onLanguageChange: (AppLanguage) -> Unit = {},
    currentWeight: CurrentWeight? = null,
    onManageWeight: () -> Unit = {},
    onSave: (Profile, DailyGoal?) -> Unit,
) {
    val original = initial?.profile
    val originalGoal = initial?.goals?.let { goalForDate(it, LocalDate.now()) }
    var language by rememberSaveable { mutableStateOf(original?.language ?: AppLanguage.RUSSIAN) }
    val theme = original?.theme ?: AppTheme.LIGHT
    var sex by rememberSaveable { mutableStateOf(original?.sex ?: Sex.MALE) }
    var mode by rememberSaveable { mutableStateOf(originalGoal?.macroMode ?: MacroMode.GRAMS) }
    var age by rememberSaveable { mutableStateOf(original?.age?.toString() ?: "") }
    val originalWeight = currentWeight?.kilograms ?: original?.weightKg
    fun display(value: Double?) = value?.let { formatDecimal(it) }.orEmpty()
    var height by rememberSaveable { mutableStateOf(display(original?.heightCm)) }
    var weight by rememberSaveable { mutableStateOf(display(originalWeight)) }
    val originalActivity = original?.activityFactor ?: 1.2
    var exactActivity by rememberSaveable { mutableStateOf<Double?>(originalActivity) }
    var activity by rememberSaveable { mutableStateOf(display(originalActivity)) }
    var calories by rememberSaveable { mutableStateOf(display(originalGoal?.calories)) }
    fun initialMacro(value: Double?, kcalPerGram: Int): Double? {
        if (value == null || originalGoal == null) return null
        return if (originalGoal.macroMode == MacroMode.PERCENT) value * kcalPerGram * 100 / originalGoal.calories else value
    }
    val originalProtein = initialMacro(originalGoal?.protein, 4)
    val originalFat = initialMacro(originalGoal?.fat, 9)
    val originalCarbs = initialMacro(originalGoal?.carbohydrate, 4)
    var protein by rememberSaveable { mutableStateOf(display(originalProtein)) }
    var fat by rememberSaveable { mutableStateOf(display(originalFat)) }
    var carbs by rememberSaveable { mutableStateOf(display(originalCarbs)) }
    var water by rememberSaveable { mutableStateOf(display(originalGoal?.waterMl)) }
    var error by remember { mutableStateOf(false) }
    val ru = language == AppLanguage.RUSSIAN
    fun label(russian: String, english: String) = if (ru) russian else english
    fun number(text: String): Double? = text.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
    fun read(text: String, saved: Double?) = saved?.takeIf { text == display(it) } ?: number(text)
    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (initial == null) label("Первый запуск", "Get started") else label("Профиль", "Profile"), style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileChoice("Русский", language == AppLanguage.RUSSIAN) { language = AppLanguage.RUSSIAN; onLanguageChange(language) }
            ProfileChoice("English", language == AppLanguage.ENGLISH) { language = AppLanguage.ENGLISH; onLanguageChange(language) }
        }
        Text(label("Пол для расчёта", "Sex for calculation"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileChoice(label("Мужской", "Male"), sex == Sex.MALE) { sex = Sex.MALE }
            ProfileChoice(label("Женский", "Female"), sex == Sex.FEMALE) { sex = Sex.FEMALE }
        }
        ProfileField(label("Возраст", "Age"), age) { age = it }
        ProfileField(label("Рост, см", "Height, cm"), height) { height = it }
        if (currentWeight?.measurementDate == null) {
            ProfileField(label("Вес, кг", "Weight, kg"), weight) { weight = it }
        } else {
            OutlinedTextField(weight, {}, readOnly = true, label = { Text(label("Вес, кг", "Weight, kg")) }, modifier = Modifier.fillMaxWidth())
            Text(label("Последний замер: ", "Last measurement: ") + currentWeight.measurementDate)
            OutlinedButton(onClick = onManageWeight) { Text(label("Изменить вес", "Change weight")) }
        }
        if (initial == null) {
            if (error) Text(label("Проверьте возраст, рост и вес.", "Check your age, height and weight."), color = MaterialTheme.colorScheme.error)
            Button(onClick = {
                val a = age.toIntOrNull()?.takeIf { it > 0 }
                val h = number(height)?.takeIf { it > 0 }
                val w = number(weight)?.takeIf { it > 0 }
                if (a == null || h == null || w == null) {
                    error = true
                } else {
                    onSave(Profile(sex, a, h, w, 1.2, language, theme), null)
                }
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(label("Открыть дневник", "Open diary")) }
            return@Column
        }
        Text(label("Уровень обычной активности", "Usual activity level"))
        Text(label("Ориентиры для выбора, шаги и минуты приведены для примера.", "Use these examples as a guide. Steps and minutes are illustrative."),
            style = MaterialTheme.typography.bodySmall)
        listOf(
            Triple("1.2", label("Малоподвижный", "Sedentary"), label("Много сидите · около 3 000 шагов · 10–15 мин движения в день", "Mostly seated · about 3,000 steps · 10–15 min of movement a day")),
            Triple("1.375", label("Лёгкая активность", "Light activity"), label("Короткие прогулки · около 5 000 шагов · 20–30 мин в день", "Short walks · about 5,000 steps · 20–30 min a day")),
            Triple("1.55", label("Умеренная активность", "Moderate activity"), label("Регулярная ходьба или тренировки · около 8 000 шагов · 30–60 мин в день", "Regular walking or workouts · about 8,000 steps · 30–60 min a day")),
            Triple("1.725", label("Высокая активность", "High activity"), label("Подвижная работа или длительные тренировки · около 11 000 шагов · от 60 мин в день", "Active job or longer workouts · about 11,000 steps · 60+ min a day")),
        ).forEach { (factor, title, description) ->
            val preset = factor.toDouble()
            ProfileChoice(title, (exactActivity ?: number(activity)) == preset, Modifier.fillMaxWidth()) {
                exactActivity = preset
                activity = display(preset)
            }
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ProfileField(label("Коэффициент активности", "Activity factor"), activity) {
            activity = it
            exactActivity = null
        }
        val estimated = read(weight, originalWeight)?.let { w ->
            read(height, original?.heightCm)?.let { h -> age.toIntOrNull()?.let { a -> estimateRestingEnergy(w, h, a, sex) } }
        }
        val activityFactor = exactActivity ?: number(activity)
        if (estimated != null && activityFactor != null && activityFactor > 0) {
            val maintenance = estimated * activityFactor
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
            ProfileChoice(label("Граммы", "Grams"), mode == MacroMode.GRAMS) { if (mode != MacroMode.GRAMS) { protein = ""; fat = ""; carbs = "" }; mode = MacroMode.GRAMS }
            ProfileChoice("%", mode == MacroMode.PERCENT) { if (mode != MacroMode.PERCENT) { protein = ""; fat = ""; carbs = "" }; mode = MacroMode.PERCENT }
        }
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
                listOf(preview.protein, preview.fat, preview.carbohydrate).joinToString(" / ") { formatDecimal(it, !ru) })
        }
        ProfileField(label("Цель жидкости, мл (необязательно)", "Water goal, ml (optional)"), water) { water = it }
        if (error) Text(label("Проверьте значения и сумму процентов.", "Check the values and percentage total."), color = MaterialTheme.colorScheme.error)
        Button(onClick = {
            val a = age.toIntOrNull()
            val h = read(height, original?.heightCm)
            val w = read(weight, originalWeight)
            val act = activityFactor
            if (a == null || a <= 0 || h == null || h <= 0 || w == null || w <= 0 || act == null || act <= 0) {
                error = true
                return@Button
            }
            val profile = Profile(sex, a, h, w, act, language, theme)
            if (originalGoal == null && listOf(calories, protein, fat, carbs, water).all { it.isBlank() }) {
                onSave(profile, null)
                return@Button
            }
            val c = read(calories, originalGoal?.calories)
            val sameMode = mode == originalGoal?.macroMode
            val p = read(protein, originalProtein.takeIf { sameMode })
            val f = read(fat, originalFat.takeIf { sameMode })
            val b = read(carbs, originalCarbs.takeIf { sameMode })
            val waterValue = if (water.isBlank()) null else read(water, originalGoal?.waterMl)
            if (c == null || c <= 0 || p == null || p < 0 ||
                f == null || f < 0 || b == null || b < 0 ||
                (water.isNotBlank() && (waterValue == null || waterValue <= 0)) ||
                (mode == MacroMode.PERCENT && kotlin.math.abs(p + f + b - 100.0) >= 0.0001)) {
                error = true
            } else {
                val macros = if (mode == MacroMode.PERCENT) GoalCalculator.macros(c, p, f, b) else com.niamniam.kbjy.MacroGoals(p, f, b)
                onSave(profile, DailyGoal(c, macros.protein, macros.fat, macros.carbohydrate, waterValue, mode))
            }
        }, modifier = Modifier.fillMaxWidth()) { Text(label("Сохранить", "Save")) }
    }
}

@Composable
private fun ProfileField(label: String, value: String, onChange: (String) -> Unit) {
    val fill = if (value.isBlank()) Color.Transparent else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = fill, unfocusedContainerColor = fill))
}

@Composable
private fun ProfileChoice(label: String, chosen: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val marked = modifier.semantics { selected = chosen }
    if (chosen) Button(onClick = onClick, modifier = marked, colors = ButtonDefaults.buttonColors()) { Text(label) }
    else OutlinedButton(onClick = onClick, modifier = marked) { Text(label) }
}
