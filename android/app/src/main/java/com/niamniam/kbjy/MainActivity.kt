package com.niamniam.kbjy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.room.withTransaction
import com.niamniam.kbjy.data.DiaryDatabase
import com.niamniam.kbjy.data.DiaryEntry
import com.niamniam.kbjy.data.Food
import com.niamniam.kbjy.data.StarterCatalog
import com.niamniam.kbjy.scanner.ManualFoodScreen
import com.niamniam.kbjy.scanner.ManualFoodUnit
import com.niamniam.kbjy.scanner.CaptureScreen
import com.niamniam.kbjy.scanner.LabelRecognizer
import com.niamniam.kbjy.scanner.ParsedLabel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.niamniam.kbjy.profile.AppTheme
import com.niamniam.kbjy.profile.AppLanguage
import com.niamniam.kbjy.profile.NiamNiamTheme
import com.niamniam.kbjy.profile.ProfileScreen
import com.niamniam.kbjy.profile.ProfileStore
import com.niamniam.kbjy.profile.ProfileDataException
import com.niamniam.kbjy.profile.WeightDatabase
import com.niamniam.kbjy.profile.WeightScreen
import com.niamniam.kbjy.profile.RecipeScreen
import com.niamniam.kbjy.profile.ExistingRecipe
import com.niamniam.kbjy.profile.goalForDate
import com.niamniam.kbjy.diary.HistoryScreen
import com.niamniam.kbjy.reports.ReportScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = DiaryDatabase.get(this)
        val dao = database.diaryDao()
        val profileStore = ProfileStore(this)
        val weightDao = WeightDatabase.get(this).weightDao()
        val applicationContext = applicationContext
        setContent {
            val loadedProfile = remember { runCatching { profileStore.load() } }
            var profileState by remember { mutableStateOf(loadedProfile.getOrNull()) }
            var theme by remember { mutableStateOf(profileState?.profile?.theme ?: AppTheme.LIGHT) }
            NiamNiamTheme(theme) {
                var date by remember { mutableStateOf(LocalDate.now().toString()) }
                LaunchedEffect(Unit) {
                    while (true) {
                        date = LocalDate.now().toString()
                        delay(1_000)
                    }
                }
                val dayFlow = remember(date) { dao.observeDay(date) }
                val entries by key(date) { dayFlow.collectAsState(initial = emptyList()) }
                val foodsFlow = remember { dao.observeFoods() }
                val foods by foodsFlow.collectAsState(initial = emptyList())
                var starterFoods by remember { mutableStateOf<List<Food>>(emptyList()) }
                LaunchedEffect(Unit) {
                    starterFoods = withContext(Dispatchers.IO) { StarterCatalog.load(applicationContext) }
                }
                val scope = rememberCoroutineScope()
                var screen by remember { mutableStateOf(if (loadedProfile.isFailure) "profile_error" else if (profileState == null) "profile" else "today") }
                var selected by remember { mutableStateOf<Food?>(null) }
                var meal by remember { mutableStateOf("lunch") }
                var editingRecipe by remember { mutableStateOf<ExistingRecipe?>(null) }
                var historyDate by remember { mutableStateOf(LocalDate.now()) }
                var entryDate by remember { mutableStateOf<String?>(null) }
                var scannedCode by remember { mutableStateOf<String?>(null) }
                var scanError by remember { mutableStateOf(false) }
                var showWater by remember { mutableStateOf(false) }
                var recognizedLabel by remember { mutableStateOf<ParsedLabel?>(null) }
                var recognizing by remember { mutableStateOf(false) }
                var saving by remember { mutableStateOf(false) }
                val saveDate = entryDate ?: date
                val historyFlow = remember(historyDate) { dao.observeDay(historyDate.toString()) }
                val historyEntries by key(historyDate) { historyFlow.collectAsState(initial = emptyList()) }
                val language = profileState?.profile?.language ?: AppLanguage.RUSSIAN
                val english = language == AppLanguage.ENGLISH
                Surface(Modifier.fillMaxSize()) {
                    when (screen) {
                        "label" -> Column {
                            TextButton(onClick = { screen = "new" }) { Text(if (english) "Back" else "Назад") }
                            if (recognizing) Text(if (english) "Reading label…" else "Распознавание этикетки…")
                            else CaptureScreen(
                                english = english,
                                onManualEntry = { recognizedLabel = null; screen = "new" },
                                onImageCaptured = { bitmap ->
                                    recognizing = true
                                    scope.launch {
                                        val result = runCatching { withContext(Dispatchers.IO) { LabelRecognizer.recognize(applicationContext, bitmap) } }
                                        recognizedLabel = result.getOrNull()
                                        scanError = result.isFailure
                                        recognizing = false
                                        screen = "new"
                                    }
                                },
                            )
                        }
                        "scan" -> Column {
                            TextButton(onClick = { screen = "today" }) { Text(if (english) "Back" else "Назад") }
                            if (scanError) Text(if (english) "Code not recognized. Try again." else "Код не распознан. Попробуйте ещё раз.")
                            CaptureScreen(
                                english = english,
                                onManualEntry = { scannedCode = null; screen = "new" },
                                onImageCaptured = { bitmap ->
                                    val scanner = BarcodeScanning.getClient()
                                    scanner.process(InputImage.fromBitmap(bitmap, 0))
                                        .addOnSuccessListener { codes ->
                                            val code = codes.firstNotNullOfOrNull { it.rawValue }
                                            if (code == null) scanError = true
                                            else scope.launch {
                                                val food = dao.findByBarcode(code)
                                                if (food == null) { scannedCode = code; screen = "new" }
                                                else { scannedCode = null; selected = food; screen = "foods" }
                                            }
                                        }
                                        .addOnFailureListener { scanError = true }
                                        .addOnCompleteListener { scanner.close() }
                                },
                            )
                        }
                        "profile_error" -> Text("Не удалось прочитать сохранённый профиль. Данные сохранены на устройстве.", modifier = Modifier.padding(24.dp))
                        "history" -> Column {
                            TextButton(onClick = { screen = "today" }) { Text(if (english) "Back" else "Назад") }
                            HistoryScreen(
                                date = historyDate,
                                entries = historyEntries,
                                onDateSelected = { historyDate = it },
                                onDelete = { id -> scope.launch { dao.delete(id) } },
                                onAdd = { selectedDate -> entryDate = selectedDate.toString(); screen = "foods" },
                                onUpdate = { entry -> scope.launch { dao.update(entry) } },
                                english = language == AppLanguage.ENGLISH,
                            )
                        }
                        "weight" -> WeightScreen(dao = weightDao, language = language, onBack = { screen = "today" })
                        "recipe" -> RecipeScreen(dao = dao, date = saveDate, meal = meal, language = language,
                            initialRecipe = editingRecipe,
                            onSaved = { screen = "today" }, onBack = { screen = "today" },
                            saveRecipe = { food, weight, ingredients, entry ->
                                database.withTransaction {
                                    val id = if (food.id == 0L) dao.insertFood(food) else {
                                        dao.updateFood(food)
                                        food.id
                                    }
                                    database.recipeDao().replaceComposition(id, weight, ingredients)
                                    if (entry != null) dao.insert(entry)
                                }
                            })
                        "report" -> ReportScreen(dao = dao, language = language, onBack = { screen = "today" })
                        "profile" -> ProfileScreen(
                            initial = profileState,
                            onThemeChange = { theme = it },
                            onSave = { profile, goal ->
                                profileStore.save(profile, goal)
                                profileState = profileStore.load()
                                theme = profile.theme
                                screen = "today"
                            },
                        )
                        "new" -> Column {
                            TextButton(onClick = { screen = "today" }) { Text(if (english) "Back" else "Назад") }
                            TextButton(onClick = { screen = "label" }) { Text(if (english) "Read nutrition label" else "Считать этикетку") }
                            if (scanError) Text(if (english) "Recognition failed. Enter values manually." else "Распознать не удалось. Введите значения вручную.")
                            ManualFoodScreen(onSave = { input ->
                                if (saving) return@ManualFoodScreen
                                saving = true
                                val unit = if (input.unit == ManualFoodUnit.GRAMS) "г" else "мл"
                                val food = Food(name = input.name, unit = unit, caloriesPer100 = input.caloriesPer100,
                                    proteinPer100 = input.proteinPer100, fatPer100 = input.fatPer100,
                                    carbohydratePer100 = input.carbsPer100)
                                scope.launch {
                                    try {
                                        dao.saveNewFoodAndEntry(food, food.entry(saveDate, input.portion, meal), scannedCode)
                                        scannedCode = null
                                        recognizedLabel = null
                                        scanError = false
                                        screen = if (entryDate == null) "today" else "history"
                                        entryDate = null
                                    } finally { saving = false }
                                }
                            }, english = english, recognized = recognizedLabel)
                        }
                        "foods" -> Column(Modifier.padding(16.dp)) {
                            var query by remember { mutableStateOf("") }
                            TextButton(onClick = { screen = "today" }) { Text(if (english) "Back" else "Назад") }
                            Text(if (english) "Food search" else "Поиск продуктов", style = MaterialTheme.typography.headlineSmall)
                            Text(if (english) "USDA FoodData Central values are estimates per 100 g." else "Данные USDA FoodData Central ориентировочные, на 100 г.", style = MaterialTheme.typography.bodySmall)
                            OutlinedTextField(query, { query = it }, label = { Text(if (english) "Search" else "Поиск") })
                            TextButton(onClick = { screen = "new" }) { Text(if (english) "New food" else "Новый продукт") }
                            LazyColumn {
                                items(foods.filter { StarterCatalog.matches(it.name, query) }, key = { "saved-${it.id}" }) { food ->
                                    TextButton(onClick = { selected = food }) { Text(food.name) }
                                }
                                if (query.isNotBlank()) {
                                    items(starterFoods.filter { StarterCatalog.matches(it.name, query) }.take(100)) { food ->
                                        TextButton(onClick = { selected = food }) { Text(food.name + " · USDA") }
                                    }
                                }
                            }
                        }
                        else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                            Text(if (english) "Today" else "Сегодня", style = MaterialTheme.typography.headlineLarge)
                            Text(date)
                            profileState?.goals?.let { goals ->
                                goalForDate(goals, LocalDate.parse(date))?.let { goal ->
                                    TodaySummary(
                                        DiaryItem(entries.sumOf { it.calories }, entries.sumOf { it.protein },
                                            entries.sumOf { it.fat }, entries.sumOf { it.carbohydrate },
                                            entries.sumOf { it.drinkMl }),
                                        goal,
                                        profileState?.profile?.language == AppLanguage.ENGLISH,
                                    )
                                }
                            }
                            Text((if (english) "Fluid: %.0f ml" else "Жидкость: %.0f мл").format(entries.sumOf { it.drinkMl }))
                            profileState?.goals?.let { goals ->
                                goalForDate(goals, LocalDate.parse(date))?.waterMl?.let { target ->
                                    Text((if (english) "Fluid goal: %.0f ml" else "Цель жидкости: %.0f мл").format(target))
                                    LinearProgressIndicator(
                                        progress = { (entries.sumOf { it.drinkMl } / target).coerceIn(0.0, 1.0).toFloat() },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                            TextButton(onClick = { showWater = true }) { Text(if (english) "Add water" else "Добавить воду") }
                            Text(if (english) "Meal for new entries" else "Приём пищи для новых записей")
                            listOf(listOf("breakfast", "lunch"), listOf("dinner", "snack")).forEach { group ->
                              Row {
                                group.forEach { item ->
                                    TextButton(onClick = { meal = item }) {
                                        val label = when (item) {
                                            "breakfast" -> if (english) "Breakfast" else "Завтрак"
                                            "lunch" -> if (english) "Lunch" else "Обед"
                                            "dinner" -> if (english) "Dinner" else "Ужин"
                                            else -> if (english) "Snack" else "Перекус"
                                        }
                                        Text(if (meal == item) "✓ $label" else label)
                                    }
                                }
                              }
                            }
                            Row {
                                TextButton(onClick = { entryDate = null; screen = "foods" }) { Text(if (english) "Choose food" else "Выбрать продукт") }
                                TextButton(onClick = { entryDate = null; scannedCode = null; screen = "new" }) { Text(if (english) "New food" else "Новый продукт") }
                                TextButton(onClick = { screen = "profile" }) { Text(if (english) "Profile" else "Профиль") }
                            }
                            Row {
                                TextButton(onClick = { screen = "history" }) { Text(if (english) "History" else "История") }
                                TextButton(onClick = { screen = "weight" }) { Text(if (english) "Weight" else "Вес") }
                                TextButton(onClick = { screen = "report" }) { Text(if (english) "Reports" else "Отчёт") }
                            }
                            Row {
                                TextButton(onClick = { editingRecipe = null; screen = "recipe" }) { Text(if (english) "Recipe" else "Рецепт") }
                                TextButton(onClick = { scanError = false; scannedCode = null; entryDate = null; screen = "scan" }) { Text(if (english) "Scan" else "Сканировать") }
                            }
                            listOf("breakfast", "lunch", "dinner", "snack").forEach { mealType ->
                              val mealEntries = entries.filter { it.meal == mealType || (mealType == "lunch" && it.meal == "Обед") }
                              if (mealEntries.isNotEmpty()) {
                                val heading = when (mealType) {
                                    "breakfast" -> if (english) "Breakfast" else "Завтрак"
                                    "lunch" -> if (english) "Lunch" else "Обед"
                                    "dinner" -> if (english) "Dinner" else "Ужин"
                                    else -> if (english) "Snack" else "Перекус"
                                }
                                Text(heading, style = MaterialTheme.typography.titleMedium)
                                mealEntries.forEach { entry ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text(entry.name)
                                        Text((if (english) "%.0f %s · %.0f kcal" else "%.0f %s · %.0f ккал").format(entry.amount, displayUnit(entry.unit, english), entry.calories))
                                    }
                                    TextButton(onClick = { scope.launch { dao.delete(entry.id) } }) { Text(if (english) "Delete" else "Удалить") }
                                }
                                }
                              }
                            }
                        }
                    }
                }
                if (showWater) {
                    var waterAmount by remember { mutableStateOf("") }
                    val water = waterAmount.replace(',', '.').toDoubleOrNull()
                    AlertDialog(
                        onDismissRequest = { showWater = false },
                        title = { Text(if (english) "Add water" else "Добавить воду") },
                        text = { OutlinedTextField(waterAmount, { waterAmount = it }, label = { Text(if (english) "Amount, ml" else "Объём, мл") }) },
                        confirmButton = {
                            TextButton(enabled = water != null && water.isFinite() && water > 0, onClick = {
                                showWater = false
                                scope.launch {
                                    dao.insert(DiaryEntry(date = LocalDate.now().toString(), meal = meal,
                                        name = if (english) "Water" else "Вода", amount = water!!, unit = "мл",
                                        calories = 0.0, protein = 0.0, fat = 0.0, carbohydrate = 0.0, drinkMl = water))
                                }
                            }) { Text(if (english) "Add" else "Добавить") }
                        },
                        dismissButton = { TextButton(onClick = { showWater = false }) { Text(if (english) "Cancel" else "Отмена") } },
                    )
                }
                selected?.let { food ->
                    var amount by remember(food.id) { mutableStateOf("") }
                    val value = amount.replace(',', '.').toDoubleOrNull()
                    val recipe by database.recipeDao().observeRecipe(food.id).collectAsState(initial = null)
                    val ingredients by database.recipeDao().observeIngredients(food.id).collectAsState(initial = emptyList())
                    AlertDialog(
                        onDismissRequest = { selected = null },
                        title = { Text(food.name) },
                        text = { OutlinedTextField(amount, { amount = it }, label = { Text((if (english) "Portion" else "Порция") + ", " + displayUnit(food.unit, english)) }) },
                        confirmButton = {
                            TextButton(enabled = !saving && value != null && value.isFinite() && value > 0, onClick = {
                                saving = true
                                scope.launch {
                                    try {
                                        if (food.id == 0L) dao.saveNewFoodAndEntry(food, food.entry(saveDate, value!!, meal))
                                        else dao.insert(food.entry(saveDate, value!!, meal))
                                        selected = null
                                        screen = if (entryDate == null) "today" else "history"
                                        entryDate = null
                                    } finally { saving = false }
                                }
                            }) { Text(if (english) "Add" else "Добавить") }
                        },
                        dismissButton = {
                            Row {
                                if (recipe != null && ingredients.isNotEmpty()) {
                                    TextButton(onClick = {
                                        editingRecipe = ExistingRecipe(food, recipe!!.cookedWeight, ingredients.map { it.toIngredient() })
                                        selected = null
                                        screen = "recipe"
                                    }) { Text(if (english) "Edit recipe" else "Изменить рецепт") }
                                }
                                TextButton(onClick = { selected = null }) { Text(if (english) "Cancel" else "Отмена") }
                            }
                        },
                    )
                }
            }
        }
    }
}

private fun displayUnit(unit: String, english: Boolean): String = when (unit) {
    "г", "g" -> if (english) "g" else "г"
    "мл", "ml" -> if (english) "ml" else "мл"
    else -> unit
}

private fun Food.entry(date: String, amount: Double, meal: String): DiaryEntry = DiaryEntry(
    date = date, meal = meal, name = name, amount = amount, unit = unit,
    calories = PortionCalculator.scale(caloriesPer100, amount),
    protein = PortionCalculator.scale(proteinPer100, amount),
    fat = PortionCalculator.scale(fatPer100, amount),
    carbohydrate = PortionCalculator.scale(carbohydratePer100, amount),
    drinkMl = if (unit == "мл" || unit == "ml") amount else 0.0,
)
