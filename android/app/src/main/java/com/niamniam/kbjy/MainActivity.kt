package com.niamniam.kbjy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.room.withTransaction
import com.niamniam.kbjy.data.DiaryDatabase
import com.niamniam.kbjy.data.DiaryEntry
import com.niamniam.kbjy.data.Food
import com.niamniam.kbjy.data.StarterCatalog
import com.niamniam.kbjy.data.PackagedFood
import com.niamniam.kbjy.data.RegionalFood
import com.niamniam.kbjy.data.RegionalSource
import com.niamniam.kbjy.data.WATER_MEAL
import com.niamniam.kbjy.data.isWaterEntry
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
import com.niamniam.kbjy.profile.diaryColorScheme
import com.niamniam.kbjy.profile.ProfileScreen
import com.niamniam.kbjy.profile.ProfileStore
import com.niamniam.kbjy.profile.ProfileDataException
import com.niamniam.kbjy.profile.WeightDatabase
import com.niamniam.kbjy.profile.WeightScreen
import com.niamniam.kbjy.profile.WeightMeasurement
import com.niamniam.kbjy.profile.currentWeight
import com.niamniam.kbjy.profile.RecipeScreen
import com.niamniam.kbjy.profile.ExistingRecipe
import com.niamniam.kbjy.profile.goalForDate
import com.niamniam.kbjy.diary.HistoryScreen
import com.niamniam.kbjy.reports.ReportScreen
import com.niamniam.kbjy.update.UpdatesScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
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
                var packagedFoods by remember { mutableStateOf<List<PackagedFood>>(emptyList()) }
                var regionalFoods by remember { mutableStateOf<Map<RegionalSource, List<RegionalFood>>>(emptyMap()) }
                LaunchedEffect(Unit) {
                    starterFoods = withContext(Dispatchers.IO) { StarterCatalog.load(applicationContext) }
                    packagedFoods = withContext(Dispatchers.IO) { StarterCatalog.loadPackaged(applicationContext) }
                    regionalFoods = withContext(Dispatchers.IO) {
                        RegionalSource.entries.associateWith { StarterCatalog.loadRegional(applicationContext, it) }
                    }
                }
                val scope = rememberCoroutineScope()
                val manualFoodState = rememberSaveableStateHolder()
                val myFoodsState = rememberSaveableStateHolder()
                var screen by rememberSaveable { mutableStateOf(if (loadedProfile.isFailure) "profile_error" else if (profileState == null) "profile" else "today") }
                var weightReturnScreen by rememberSaveable { mutableStateOf("today") }
                var weightMeasurements by remember { mutableStateOf<List<WeightMeasurement>?>(null) }
                LaunchedEffect(weightDao) { weightDao.observeAll().collect { weightMeasurements = it } }
                var selected by rememberSaveable(stateSaver = selectedFoodSaver) { mutableStateOf<Food?>(null) }
                var meal by rememberSaveable { mutableStateOf("lunch") }
                var editingRecipe by remember { mutableStateOf<ExistingRecipe?>(null) }
                var editingRecipeId by rememberSaveable { mutableStateOf<Long?>(null) }
                var editingFood by rememberSaveable(stateSaver = selectedFoodSaver) { mutableStateOf<Food?>(null) }
                var savingToMyFoods by rememberSaveable { mutableStateOf(false) }
                fun finishFoodForm(diaryDestination: String): String {
                    if (!savingToMyFoods) return diaryDestination
                    savingToMyFoods = false
                    editingFood = null
                    return "my-foods"
                }
                var historyDateText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
                val historyDate = LocalDate.parse(historyDateText)
                var entryDate by rememberSaveable { mutableStateOf<String?>(null) }
                var scannedCode by rememberSaveable { mutableStateOf<String?>(null) }
                var scanError by remember { mutableStateOf(false) }
                var waterDate by rememberSaveable { mutableStateOf<String?>(null) }
                var recognizedLabel by remember { mutableStateOf<ParsedLabel?>(null) }
                var recognizing by remember { mutableStateOf(false) }
                var saving by remember { mutableStateOf(false) }
                var saveError by remember { mutableStateOf(false) }
                val saveDate = entryDate ?: date
                val historyFlow = remember(historyDate) { dao.observeDay(historyDate.toString()) }
                val historyEntries by key(historyDate) { historyFlow.collectAsState(initial = emptyList()) }
                val language = profileState?.profile?.language ?: AppLanguage.RUSSIAN
                val english = language == AppLanguage.ENGLISH
                LaunchedEffect(screen, editingRecipeId) {
                    val id = editingRecipeId
                    if (screen == "recipe" && id != null && editingRecipe == null) {
                        val food = dao.findFood(id)
                        val recipe = database.recipeDao().observeRecipe(id).first()
                        val ingredients = database.recipeDao().observeIngredients(id).first()
                        if (food != null && recipe != null) {
                            editingRecipe = ExistingRecipe(food, recipe.cookedWeight, ingredients.map { it.toIngredient() })
                        } else {
                            editingRecipeId = null
                            screen = finishFoodForm("today")
                        }
                    }
                }
                BackHandler(enabled = screen != "today" && screen != "profile_error" && profileState != null) {
                    screen = when (screen) {
                        "new" -> {
                            manualFoodState.removeState("manual-food")
                            scannedCode = null
                            recognizedLabel = null
                            finishFoodForm("foods")
                        }
                        "label" -> "new"
                        "scan" -> "foods"
                        "recipe" -> {
                            editingRecipeId = null
                            editingRecipe = null
                            finishFoodForm("foods")
                        }
                        "profile", "report", "updates" -> {
                            theme = profileState?.profile?.theme ?: AppTheme.LIGHT
                            "settings"
                        }
                        "licenses", "themes" -> "settings"
                        "weight" -> weightReturnScreen
                        else -> {
                            val destination = if (entryDate != null) "history" else "today"
                            entryDate = null
                            scannedCode = null
                            recognizedLabel = null
                            theme = profileState?.profile?.theme ?: AppTheme.LIGHT
                            destination
                        }
                    }
                }
                Scaffold(bottomBar = {
                    if (profileState != null) {
                        PrimaryNavigation(screen, english, enabled = !saving) { destination ->
                            manualFoodState.removeState("manual-food")
                            selected = null
                            editingFood = null
                            editingRecipe = null
                            editingRecipeId = null
                            savingToMyFoods = false
                            scannedCode = null
                            scanError = false
                            recognizedLabel = null
                            entryDate = null
                            screen = destination
                        }
                    }
                }) { insets ->
                  Surface(Modifier.fillMaxSize().padding(insets)) {
                    when (screen) {
                        "label" -> Column {
                            BackButton(english, onClick = { screen = "new" })
                            if (recognizing) Text(if (english) "Reading label…" else "Распознавание этикетки…")
                            else CaptureScreen(
                                english = english,
                                onManualEntry = { recognizedLabel = null; screen = "new" },
                                onImageCaptured = { bitmap ->
                                    recognizing = true
                                    scope.launch {
                                        val result = runCatching { withContext(Dispatchers.IO) { LabelRecognizer.recognize(applicationContext, bitmap) } }
                                        recognizing = false
                                        if (screen == "label") {
                                            recognizedLabel = result.getOrNull()
                                            scanError = result.isFailure
                                            screen = "new"
                                        }
                                    }
                                },
                            )
                        }
                        "scan" -> Column {
                            BackButton(english, onClick = { scannedCode = null; recognizedLabel = null; screen = "foods" })
                            if (scanError) Text(if (english) "Code not recognized. Try again." else "Код не распознан. Попробуйте ещё раз.")
                            CaptureScreen(
                                english = english,
                                forBarcode = true,
                                onManualEntry = { scannedCode = null; screen = "new" },
                                onImageCaptured = { bitmap ->
                                    val scanner = BarcodeScanning.getClient()
                                    scanner.process(InputImage.fromBitmap(bitmap, 0))
                                        .addOnSuccessListener { codes ->
                                            if (screen != "scan") return@addOnSuccessListener
                                            val code = codes.firstNotNullOfOrNull { it.rawValue }
                                            if (code == null) scanError = true
                                            else scope.launch {
                                                val food = dao.findByBarcode(code)
                                                if (screen != "scan") return@launch
                                                val starter = StarterCatalog.findPackagedByBarcode(packagedFoods, code)
                                                if (food == null && starter != null) { scannedCode = code; selected = starter.food; screen = "foods" }
                                                else if (food == null) { scannedCode = code; screen = "new" }
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
                            BackButton(english, onClick = { screen = "today" })
                            HistoryScreen(
                                date = historyDate,
                                entries = historyEntries,
                                onDateSelected = { historyDateText = it.toString() },
                                onDelete = { id -> dao.delete(id) },
                                onAdd = { selectedDate -> entryDate = selectedDate.toString(); screen = "choose-meal" },
                                onUpdate = { entry -> dao.update(entry) },
                                english = language == AppLanguage.ENGLISH,
                            )
                        }
                        "weight" -> WeightScreen(dao = weightDao, language = language, onBack = { screen = weightReturnScreen })
                        "recipe" -> if (editingRecipeId != null && editingRecipe == null) {
                            Text(if (english) "Loading recipe…" else "Загрузка рецепта…")
                        } else RecipeScreen(dao = dao, date = saveDate, meal = meal, language = language,
                            initialRecipe = editingRecipe,
                            allowDiaryEntry = !savingToMyFoods,
                            starterFoods = starterFoods + regionalFoods.values.flatten().map { it.food } + packagedFoods.map { it.food },
                            onSaved = {
                                editingRecipeId = null; editingRecipe = null
                                screen = finishFoodForm(if (entryDate == null) "today" else "history")
                                entryDate = null
                            },
                            onBack = { editingRecipeId = null; editingRecipe = null; screen = finishFoodForm("foods") },
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
                        "my-foods" -> myFoodsState.SaveableStateProvider("my-foods") {
                            val recipeIdsFlow = remember { database.recipeDao().observeRecipeFoodIds() }
                            val recipeIds by recipeIdsFlow.collectAsState(initial = null)
                            MyFoodsScreen(foods, english,
                                onBack = {
                                    myFoodsState.removeState("my-foods")
                                    screen = "today"
                                },
                                onNewFood = {
                                    scannedCode = null
                                    recognizedLabel = null
                                    scanError = false
                                    savingToMyFoods = true
                                    screen = "new"
                                },
                                onNewRecipe = {
                                    editingRecipe = null
                                    editingRecipeId = null
                                    savingToMyFoods = true
                                    screen = "recipe"
                                },
                                recipeIds = recipeIds?.toSet(),
                                onEditRecipe = { id ->
                                    editingRecipe = null
                                    editingRecipeId = id
                                    savingToMyFoods = true
                                    screen = "recipe"
                                },
                                onEditFood = { food ->
                                    scannedCode = null
                                    recognizedLabel = null
                                    scanError = false
                                    editingFood = food
                                    savingToMyFoods = true
                                    screen = "new"
                                },
                            )
                        }
                        "settings" -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            BackButton(english, onClick = { screen = "today" })
                            Text(if (english) "Settings" else "Настройки", style = MaterialTheme.typography.headlineLarge)
                            OutlinedButton(onClick = { screen = "profile" }, modifier = diaryActionSize) { Text(if (english) "Profile" else "Профиль") }
                            OutlinedButton(onClick = { weightReturnScreen = "settings"; screen = "weight" }, modifier = diaryActionSize) { Text(if (english) "Weight" else "Вес") }
                            OutlinedButton(onClick = { screen = "report" }, modifier = diaryActionSize) { Text(if (english) "Reports" else "Отчёт") }
                            OutlinedButton(onClick = { screen = "updates" }, modifier = diaryActionSize) { Text(if (english) "Updates" else "Обновления") }
                            OutlinedButton(onClick = { screen = "themes" }, modifier = diaryActionSize) { Text(if (english) "Theme" else "Тема") }
                            OutlinedButton(onClick = { screen = "licenses" }, modifier = diaryActionSize) {
                                Text(if (english) "Sources and licenses" else "Источники и лицензии")
                            }
                        }
                        "themes" -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            BackButton(english, onClick = { screen = "settings" })
                            Text(if (english) "Themes" else "Темы", style = MaterialTheme.typography.headlineMedium)
                            AppTheme.entries.forEach { option ->
                                val title = when (option) {
                                    AppTheme.LIGHT -> if (english) "Light" else "Светлая"
                                    AppTheme.DARK -> if (english) "Dark" else "Тёмная"
                                    AppTheme.FOREST -> if (english) "Forest" else "Лесная"
                                }
                                OutlinedButton(onClick = {
                                    profileState?.profile?.let { profile ->
                                        profileStore.save(profile.copy(theme = option), null)
                                        profileState = profileStore.load()
                                        theme = option
                                    }
                                }, modifier = diaryActionSize, enabled = theme != option,
                                    border = BorderStroke(1.dp, if (theme == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    ),
                                ) {
                                    val preview = diaryColorScheme(option)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        for (color in listOf(preview.background, preview.primary, preview.secondary)) {
                                            Surface(color = color, shape = MaterialTheme.shapes.extraSmall, modifier = Modifier.size(14.dp),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {}
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(if (theme == option) "✓ $title" else title)
                                }
                            }
                        }
                        "choose-meal" -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            BackButton(english, onClick = { entryDate = null; screen = "history" })
                            Text(if (english) "Choose a meal" else "Выберите приём пищи", style = MaterialTheme.typography.headlineSmall)
                            diaryMeals.forEach { item ->
                                Button(onClick = { meal = item; screen = "foods" }, modifier = diaryActionSize) { Text(mealName(item, english)) }
                            }
                        }
                        "report" -> ReportScreen(dao = dao, language = language, onBack = { screen = "settings" })
                        "updates" -> UpdatesScreen(english = english, onBack = { screen = "settings" })
                        "licenses" -> ThirdPartyNoticesScreen(english = english, onBack = { screen = "settings" })
                        "profile" -> if (profileState != null && weightMeasurements == null) {
                            Text(if (english) "Loading weight…" else "Загрузка веса…")
                        } else Column {
                            if (profileState != null) BackButton(english, onClick = {
                                theme = profileState?.profile?.theme ?: AppTheme.LIGHT
                                screen = "settings"
                            })
                            ProfileScreen(
                            initial = profileState,
                            currentWeight = profileState?.profile?.let { currentWeight(weightMeasurements.orEmpty(), it.weightKg, LocalDate.parse(date)) },
                            onManageWeight = { weightReturnScreen = "profile"; screen = "weight" },
                            onSave = { profile, goal ->
                                val originalWeight = profileState?.profile?.weightKg
                                val measured = profileState?.profile?.let { currentWeight(weightMeasurements.orEmpty(), it.weightKg, LocalDate.parse(date)).measurementDate != null } == true
                                val nextScreen = if (profileState == null) "today" else "settings"
                                profileStore.save(if (measured && originalWeight != null) profile.copy(weightKg = originalWeight) else profile, goal)
                                profileState = profileStore.load()
                                theme = profile.theme
                                screen = nextScreen
                            },
                            )
                        }
                        "new" -> manualFoodState.SaveableStateProvider("manual-food") { Column {
                            BackButton(english, onClick = {
                                manualFoodState.removeState("manual-food")
                                scannedCode = null
                                recognizedLabel = null
                                screen = finishFoodForm("foods")
                            })
                            TextButton(onClick = { screen = "label" }) { Text(if (english) "Read nutrition label" else "Считать этикетку") }
                            if (scanError) Text(if (english) "Recognition failed. Enter values manually." else "Распознать не удалось. Введите значения вручную.")
                            ManualFoodScreen(onSave = { input ->
                                if (saving) return@ManualFoodScreen
                                saving = true
                                val unit = if (input.unit == ManualFoodUnit.GRAMS) "г" else "мл"
                                val food = Food(id = editingFood?.id ?: 0, name = input.name, unit = unit, caloriesPer100 = input.caloriesPer100,
                                    proteinPer100 = input.proteinPer100, fatPer100 = input.fatPer100,
                                    carbohydratePer100 = input.carbsPer100, defaultPortion = input.portion, defaultIsDrink = input.isDrink)
                                scope.launch {
                                    try {
                                        if (food.id != 0L) {
                                            dao.updateFood(food)
                                        } else if (savingToMyFoods) {
                                            dao.insertFood(food)
                                        } else {
                                            dao.saveNewFoodAndEntry(food, food.entry(saveDate, input.portion, meal, if (input.isDrink) input.portion else 0.0), scannedCode)
                                        }
                                        manualFoodState.removeState("manual-food")
                                        scannedCode = null
                                        recognizedLabel = null
                                        scanError = false
                                        screen = finishFoodForm(if (entryDate == null) "today" else "history")
                                        entryDate = null
                                    } catch (e: CancellationException) { throw e }
                                    catch (_: Exception) { saveError = true }
                                    finally { saving = false }
                                }
                            }, english = english, recognized = recognizedLabel, catalogOnly = savingToMyFoods, initialFood = editingFood,
                                onRecognitionApplied = { recognizedLabel = null })
                        } }
                        "foods" -> Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            var query by rememberSaveable { mutableStateOf("") }
                            var browseDishes by rememberSaveable { mutableStateOf(false) }
                            fun selectPackaged(product: PackagedFood) {
                                scope.launch {
                                    val saved = dao.findByBarcode(product.code)
                                    scannedCode = product.code.takeIf { saved == null }
                                    selected = saved ?: product.food
                                }
                            }
                            BackButton(english, onClick = {
                                screen = if (entryDate == null) "today" else "history"
                                entryDate = null
                                scannedCode = null
                                recognizedLabel = null
                            })
                            Text(if (english) "Food search" else "Поиск продуктов", style = MaterialTheme.typography.headlineSmall)
                            Text(mealName(meal, english) + (entryDate?.let { " · $it" } ?: ""), style = MaterialTheme.typography.titleMedium)
                            OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), label = { Text(if (english) "Search" else "Поиск") })
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { scannedCode = null; recognizedLabel = null; scanError = false; screen = "new" }, modifier = Modifier.weight(1f)) { Text(if (english) "New food" else "Новый продукт") }
                                OutlinedButton(onClick = { scanError = false; scannedCode = null; screen = "scan" }, modifier = Modifier.weight(1f)) { Text(if (english) "Scan" else "Сканировать") }
                            }
                            OutlinedButton(onClick = { editingRecipe = null; editingRecipeId = null; screen = "recipe" }, modifier = Modifier.fillMaxWidth()) { Text(if (english) "Recipe" else "Рецепт") }
                            LazyColumn(Modifier.weight(1f)) {
                                if (query.isBlank()) item {
                                    FilterChip(selected = browseDishes, onClick = { browseDishes = !browseDishes },
                                        label = { Text(if (english) "Prepared dishes" else "Готовые блюда") })
                                }
                                items(foods.filter { (query.isNotBlank() || !browseDishes) && StarterCatalog.matches(it.name, query) }, key = { "saved-${it.id}" }) { food ->
                                    TextButton(onClick = { scannedCode = null; selected = food }) { Text(StarterCatalog.displayName(food.name, english)) }
                                }
                                if (query.isNotBlank()) {
                                    items(packagedFoods.filter { StarterCatalog.matches(it.food.name, query) }.take(100)) { product ->
                                        TextButton(onClick = { selectPackaged(product) }) { Text(StarterCatalog.displayName(product.food.name, english)) }
                                    }
                                }
                                val available = if (query.isBlank() && browseDishes) starterFoods.filter(StarterCatalog::isPreparedDish)
                                    else if (query.isBlank()) starterFoods.filterNot(StarterCatalog::isPreparedDish).take(36)
                                    else starterFoods.filter { StarterCatalog.matches(it.name, query) }.take(100)
                                if (query.isBlank() && !browseDishes && available.isNotEmpty()) item {
                                    Text(if (english) "Basic foods" else "Базовые продукты", style = MaterialTheme.typography.titleMedium)
                                }
                                items(available) { food ->
                                    TextButton(onClick = { scannedCode = null; selected = food }) { Text(StarterCatalog.displayName(food.name, english)) }
                                }
                                regionalFoods.forEach { (source, entries) ->
                                    val availableRegional = entries.filter { entry ->
                                        if (query.isBlank()) entry.prepared == browseDishes else StarterCatalog.matches(entry.food.name, query)
                                    }.take(100)
                                    items(availableRegional, key = { "${source.name}-${it.id}" }) { entry ->
                                        TextButton(onClick = { scannedCode = null; selected = entry.food }) {
                                            Text(StarterCatalog.displayName(entry.food.name, english))
                                        }
                                    }
                                }
                                if (query.isBlank() && browseDishes) {
                                    val storeDishes = packagedFoods.filter(StarterCatalog::isPreparedPackagedDish)
                                    if (storeDishes.isNotEmpty()) item {
                                        Text(if (english) "Packaged prepared dishes" else "Готовые блюда из магазина", style = MaterialTheme.typography.titleMedium)
                                    }
                                    items(storeDishes, key = { it.code }) { product ->
                                        TextButton(onClick = { selectPackaged(product) }) {
                                            Text(StarterCatalog.displayName(product.food.name, english))
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                          Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            val dateLocale = if (english) java.util.Locale.ENGLISH else java.util.Locale.forLanguageTag("ru")
                            val displayDate = LocalDate.parse(date).format(java.time.format.DateTimeFormatter.ofPattern(if (english) "EEEE, MMMM d" else "EEEE, d MMMM", dateLocale))
                            Surface(Modifier.align(Alignment.CenterHorizontally), color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.large) {
                                Text(displayDate, Modifier.padding(horizontal = 14.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge)
                            }
                            val goal = goalForDate(profileState?.goals.orEmpty(), LocalDate.parse(date))
                            TodaySummary(DiaryItem(entries.sumOf { it.calories }, entries.sumOf { it.protein },
                                entries.sumOf { it.fat }, entries.sumOf { it.carbohydrate }, entries.sumOf { it.drinkMl }), goal, english)
                            FluidRiver(entries.sumOf { it.drinkMl }, goal?.waterMl, english)
                            val (waterEntries, mealEntriesForDay) = entries.partition(DiaryEntry::isWaterEntry)
                            WaterSection(waterEntries, english, onAdd = { waterDate = date }, onDelete = { id ->
                                scope.launch {
                                    try { dao.delete(id) }
                                    catch (exception: CancellationException) { throw exception }
                                    catch (_: Exception) { saveError = true }
                                }
                            })
                            OutlinedButton(onClick = { screen = "my-foods" }, modifier = diaryActionSize) { Text(if (english) "My foods" else "Мои блюда") }
                            diaryMeals.forEach { mealType ->
                                val mealEntries = mealEntriesForDay.filter { it.meal == mealType || (mealType == "lunch" && it.meal == "Обед") }
                                Card(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(mealName(mealType, english), style = MaterialTheme.typography.titleMedium)
                                        mealEntries.forEach { entry ->
                                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                Column(Modifier.weight(1f)) {
                                                    Text(StarterCatalog.displayName(entry.name, english))
                                                    Text("${formatDecimal(entry.amount, english)} ${displayUnit(entry.unit, english)} · ${formatDecimal(entry.calories, english)} ${if (english) "kcal" else "ккал"}")
                                                }
                                                TextButton(onClick = { scope.launch { dao.delete(entry.id) } }) { Text(if (english) "Delete" else "Удалить") }
                                            }
                                        }
                                        Button(onClick = { meal = mealType; entryDate = null; screen = "foods" }, modifier = diaryActionSize) {
                                            Text(addToMealLabel(mealType, english))
                                        }
                                    }
                                }
                            }
                          }
                        }
                    }
                  }
                }
                if (saveError) {
                    AlertDialog(
                        onDismissRequest = { saveError = false },
                        title = { Text(if (english) "Could not save" else "Не удалось сохранить") },
                        text = { Text(if (english) "Check available storage and try again." else "Проверьте свободное место и попробуйте ещё раз.") },
                        confirmButton = { TextButton(onClick = { saveError = false }) { Text("OK") } },
                    )
                }
                waterDate?.let { targetDate ->
                    var waterAmount by rememberSaveable { mutableStateOf("") }
                    var savingWater by remember { mutableStateOf(false) }
                    var waterError by remember { mutableStateOf(false) }
                    val water = waterAmount.trim().replace(',', '.').toDoubleOrNull()
                        ?.takeIf { it.isFinite() && it > 0 }
                    AlertDialog(
                        onDismissRequest = { if (!savingWater) waterDate = null },
                        title = { Text(if (english) "Add water" else "Добавить воду") },
                        text = {
                            Column {
                                OutlinedTextField(waterAmount, { waterAmount = it }, enabled = !savingWater,
                                    label = { Text(if (english) "Amount, ml" else "Объём, мл") }, singleLine = true)
                                if (waterError) Text(if (english) "Could not save the water entry. Try again." else "Не удалось записать воду. Попробуйте ещё раз.",
                                    color = MaterialTheme.colorScheme.error)
                            }
                        },
                        confirmButton = {
                            TextButton(enabled = water != null && !savingWater, onClick = {
                                val volume = checkNotNull(water)
                                savingWater = true
                                waterError = false
                                scope.launch {
                                    try {
                                        dao.insert(DiaryEntry(date = targetDate, meal = WATER_MEAL,
                                            name = if (english) "Water" else "Вода", amount = volume, unit = "мл",
                                            calories = 0.0, protein = 0.0, fat = 0.0, carbohydrate = 0.0, drinkMl = volume))
                                        waterDate = null
                                    } catch (exception: CancellationException) { throw exception }
                                    catch (_: Exception) { waterError = true }
                                    finally { savingWater = false }
                                }
                            }) { Text(if (english) "Add" else "Добавить") }
                        },
                        dismissButton = { TextButton(enabled = !savingWater, onClick = { waterDate = null }) { Text(if (english) "Cancel" else "Отмена") } },
                    )
                }
                selected?.let { food ->
                    var proposing by rememberSaveable(food) { mutableStateOf(false) }
                    val defaultPortion = food.defaultPortion ?: 100.0
                    val displayedPortion = formatDecimal(defaultPortion, english)
                    var amount by rememberSaveable(food) { mutableStateOf(displayedPortion) }
                    var portions by rememberSaveable(food) { mutableStateOf("1") }
                    var saveDefault by rememberSaveable(food) { mutableStateOf(false) }
                    var countAsDrink by rememberSaveable(food) { mutableStateOf(food.defaultIsDrink) }
                    var drinkVolume by rememberSaveable(food) { mutableStateOf("") }
                    val unitWeight = if (amount == displayedPortion) defaultPortion else amount.replace(',', '.').toDoubleOrNull()
                    val count = portions.replace(',', '.').toDoubleOrNull()
                    val value = if (unitWeight != null && count != null) unitWeight * count else null
                    val volume = if (!countAsDrink) 0.0 else if (food.unit == "мл" || food.unit == "ml") value
                        else drinkVolume.replace(',', '.').toDoubleOrNull()
                    val recipe by database.recipeDao().observeRecipe(food.id).collectAsState(initial = null)
                    val ingredients by database.recipeDao().observeIngredients(food.id).collectAsState(initial = emptyList())
                    if (proposing) {
                        CatalogProposalDialog(food, recipe?.cookedWeight, ingredients, english, onDismiss = { proposing = false })
                    } else AlertDialog(
                        onDismissRequest = { selected = null; scannedCode = null },
                        title = { Text(StarterCatalog.displayName(food.name, english)) },
                        text = {
                            Column {
                                OutlinedTextField(portions, { portions = it }, label = { Text(if (english) "Number of portions" else "Количество порций") })
                                OutlinedTextField(amount, { amount = it }, label = { Text((if (english) "Size of one portion" else "Размер одной порции") + ", " + displayUnit(food.unit, english)) })
                                Row {
                                    Checkbox(checked = saveDefault, onCheckedChange = { saveDefault = it })
                                    Text(if (english) "Save as usual portion" else "Сохранить обычный вес порции")
                                }
                                Row {
                                    Checkbox(checked = countAsDrink, onCheckedChange = { countAsDrink = it })
                                    Text(if (english) "Count as a drink" else "Учесть как напиток")
                                }
                                if (countAsDrink && food.unit != "мл" && food.unit != "ml") {
                                    OutlinedTextField(drinkVolume, { drinkVolume = it },
                                        label = { Text(if (english) "Total volume, ml" else "Общий объём, мл") })
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(enabled = !saving && unitWeight != null && unitWeight.isFinite() && unitWeight > 0 &&
                                count != null && count.isFinite() && count > 0 && value != null && value.isFinite() && value > 0 &&
                                volume != null && volume.isFinite() && (if (countAsDrink) volume > 0 else volume == 0.0), onClick = {
                                saving = true
                                scope.launch {
                                    try {
                                        database.withTransaction {
                                            if (food.id == 0L) {
                                                val saved = food.copy(
                                                    defaultPortion = if (saveDefault) unitWeight else food.defaultPortion,
                                                    defaultIsDrink = countAsDrink,
                                                )
                                                dao.saveNewFoodAndEntry(saved, food.entry(saveDate, value!!, meal, volume!!), scannedCode)
                                            } else {
                                                if (saveDefault || food.defaultIsDrink != countAsDrink) {
                                                    dao.updateFood(food.copy(
                                                        defaultPortion = if (saveDefault) unitWeight else food.defaultPortion,
                                                        defaultIsDrink = countAsDrink,
                                                    ))
                                                }
                                                dao.insert(food.entry(saveDate, value!!, meal, volume!!))
                                            }
                                        }
                                        scannedCode = null
                                        selected = null
                                        screen = if (entryDate == null) "today" else "history"
                                        entryDate = null
                                    } catch (e: CancellationException) { throw e }
                                    catch (_: Exception) { saveError = true }
                                    finally { saving = false }
                                }
                            }) { Text(if (english) "Add" else "Добавить") }
                        },
                        dismissButton = {
                          Column(horizontalAlignment = Alignment.End) {
                            if (food.id > 0L && BuildConfig.CATALOG_INTAKE_URL.isNotBlank()) {
                                TextButton(onClick = { proposing = true }) {
                                    Text(if (english) "Suggest to catalog" else "Предложить в каталог")
                                }
                            }
                            Row {
                                if (recipe != null && ingredients.isNotEmpty()) {
                                    TextButton(onClick = {
                                        editingRecipe = ExistingRecipe(food, recipe!!.cookedWeight, ingredients.map { it.toIngredient() })
                                        editingRecipeId = food.id
                                        selected = null
                                        screen = "recipe"
                                    }) { Text(if (english) "Edit recipe" else "Изменить рецепт") }
                                }
                                TextButton(onClick = { selected = null; scannedCode = null }) { Text(if (english) "Cancel" else "Отмена") }
                            }
                          }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun WaterSection(entries: List<DiaryEntry>, english: Boolean, onAdd: () -> Unit, onDelete: (Long) -> Unit) {
    Card(Modifier.fillMaxWidth().testTag("today-water")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (english) "Water" else "Вода", style = MaterialTheme.typography.titleMedium)
                if (entries.isNotEmpty()) {
                    val total = "${formatDecimal(entries.sumOf { it.drinkMl }, english)} ${displayUnit("ml", english)}"
                    Text(if (english) "Total: $total" else "Всего: $total", style = MaterialTheme.typography.bodyMedium)
                }
            }
            for (entry in entries) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${formatDecimal(entry.drinkMl, english)} ${displayUnit("ml", english)}")
                    TextButton(onClick = { onDelete(entry.id) }) { Text(if (english) "Delete" else "Удалить") }
                }
            }
            OutlinedButton(onClick = onAdd, modifier = diaryActionSize) { Text(if (english) "Add water" else "Добавить воду") }
        }
    }
}

@Composable
private fun PrimaryNavigation(current: String, english: Boolean, enabled: Boolean, onNavigate: (String) -> Unit) {
    val destinations = listOf(
        "today" to if (english) "Home" else "Главная",
        "settings" to if (english) "Settings" else "Настройки",
        "history" to if (english) "History" else "История",
    )
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 3.dp) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((route, title) in destinations) {
                if (route != current) {
                    FilledTonalButton(onClick = { onNavigate(route) }, enabled = enabled,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("navigation-$route"),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)) { Text(title) }
                }
            }
        }
    }
}

internal fun displayUnit(unit: String, english: Boolean): String = when (unit) {
    "г", "g" -> if (english) "g" else "г"
    "мл", "ml" -> if (english) "ml" else "мл"
    else -> unit
}

private val diaryMeals = listOf("breakfast", "lunch", "dinner", "snack")
private val diaryActionSize = Modifier.fillMaxWidth().heightIn(min = 52.dp)

private fun mealName(meal: String, english: Boolean): String = when (meal) {
    "breakfast" -> if (english) "Breakfast" else "Завтрак"
    "lunch" -> if (english) "Lunch" else "Обед"
    "dinner" -> if (english) "Dinner" else "Ужин"
    else -> if (english) "Snack" else "Перекус"
}

private fun addToMealLabel(meal: String, english: Boolean): String = if (english) {
    "Add to ${mealName(meal, true).lowercase()}"
} else when (meal) {
    "breakfast" -> "Добавить на завтрак"
    "lunch" -> "Добавить на обед"
    "dinner" -> "Добавить на ужин"
    else -> "Добавить перекус"
}

private val selectedFoodSaver = Saver<Food?, Bundle>(
    save = { food -> food?.let {
        Bundle().apply {
            putLong("id", it.id)
            putString("name", it.name)
            putString("unit", it.unit)
            putDouble("calories", it.caloriesPer100)
            putDouble("protein", it.proteinPer100)
            putDouble("fat", it.fatPer100)
            putDouble("carbs", it.carbohydratePer100)
            it.defaultPortion?.let { portion -> putDouble("portion", portion) }
            putBoolean("drink", it.defaultIsDrink)
        }
    } },
    restore = { saved ->
        Food(
            id = saved.getLong("id"),
            name = requireNotNull(saved.getString("name")),
            unit = requireNotNull(saved.getString("unit")),
            caloriesPer100 = saved.getDouble("calories"),
            proteinPer100 = saved.getDouble("protein"),
            fatPer100 = saved.getDouble("fat"),
            carbohydratePer100 = saved.getDouble("carbs"),
            defaultPortion = if (saved.containsKey("portion")) saved.getDouble("portion") else null,
            defaultIsDrink = saved.getBoolean("drink"),
        )
    },
)

private fun Food.entry(date: String, amount: Double, meal: String, volume: Double = if (unit == "мл" || unit == "ml") amount else 0.0): DiaryEntry = DiaryEntry(
    date = date, meal = meal, name = name, amount = amount, unit = unit,
    calories = PortionCalculator.scale(caloriesPer100, amount),
    protein = PortionCalculator.scale(proteinPer100, amount),
    fat = PortionCalculator.scale(fatPer100, amount),
    carbohydrate = PortionCalculator.scale(carbohydratePer100, amount),
    drinkMl = volume,
)
