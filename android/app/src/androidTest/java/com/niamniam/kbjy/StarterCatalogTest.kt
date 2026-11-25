package com.niamniam.kbjy

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.niamniam.kbjy.data.RegionalSource
import com.niamniam.kbjy.data.StarterCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StarterCatalogTest {
    @Test
    fun commonGroceriesAreVisibleAtTheStartInBothLanguages() {
        val foods = StarterCatalog.load(ApplicationProvider.getApplicationContext()).take(10)
        val common = listOf(
            "Соль" to "Salt", "Сахар" to "Sugar", "Хлеб" to "Bread",
            "Батон" to "Bread", "Молоко" to "Milk",
        )
        for ((russian, english) in common) {
            val item = foods.first { StarterCatalog.displayName(it.name, false).startsWith(russian) }
            assertTrue(StarterCatalog.displayName(item.name, true).startsWith(english))
        }
        assertEquals("Yogurt", StarterCatalog.displayName("Yogurt", false))
        assertEquals("Суп / салат", StarterCatalog.displayName("Суп / салат", true))
    }
    @Test
    fun bakedCabbagePieUsesNutritionPer100GramsOfTheFinishedPastry() {
        val recipes = StarterCatalog.loadRegional(
            ApplicationProvider.getApplicationContext(),
            RegionalSource.NIAMNIAM_RECIPES,
        )
        val pie = recipes.single { it.id == "baked-cabbage-pie" }
        assertTrue(pie.prepared)
        assertTrue(StarterCatalog.matches(pie.food.name, "пирожок печёный с капустой"))
        assertEquals("g", pie.food.unit)
        assertEquals(245.87, pie.food.caloriesPer100, 0.0)
        assertEquals(6.32, pie.food.proteinPer100, 0.0)
        assertEquals(7.02, pie.food.fatPer100, 0.0)
        assertEquals(39.38, pie.food.carbohydratePer100, 0.0)
    }

    @Test
    fun cherryVarenikiUsesNutritionPer100GramsAfterBoiling() {
        val recipes = StarterCatalog.loadRegional(
            ApplicationProvider.getApplicationContext(),
            RegionalSource.NIAMNIAM_RECIPES,
        )
        val vareniki = recipes.single { it.id == "cherry-vareniki" }
        assertTrue(vareniki.prepared)
        assertTrue(StarterCatalog.matches(vareniki.food.name, "вареники с вишней"))
        assertEquals("g", vareniki.food.unit)
        assertEquals(200.7, vareniki.food.caloriesPer100, 0.0)
        assertEquals(3.69, vareniki.food.proteinPer100, 0.0)
        assertEquals(4.73, vareniki.food.fatPer100, 0.0)
        assertEquals(36.3, vareniki.food.carbohydratePer100, 0.0)
    }

    @Test
    fun referenceRecipeNutritionUsesTheWeightOfTheAssembledDish() {
        val source = RegionalSource.entries.single { it.asset == "reference_recipes.json" }
        val recipes = StarterCatalog.loadRegional(ApplicationProvider.getApplicationContext(), source)
        val sandwich = recipes.single { it.id == "bread-with-butter" }
        assertTrue(sandwich.prepared)
        assertTrue(StarterCatalog.matches(sandwich.food.name, "бутерброд с маслом"))
        assertEquals("g", sandwich.food.unit)
        assertEquals(366.2, sandwich.food.caloriesPer100, 0.0)
        assertEquals(8.25, sandwich.food.proteinPer100, 0.0)
        assertEquals(19.19, sandwich.food.fatPer100, 0.0)
        assertEquals(40.25, sandwich.food.carbohydratePer100, 0.0)
    }

    @Test
    fun coldSoupNutritionIncludesTheWeightOfTheLiquidBase() {
        val source = RegionalSource.NIAMNIAM_RECIPES
        val recipes = StarterCatalog.loadRegional(ApplicationProvider.getApplicationContext(), source)
        val okroshka = recipes.single { it.id == "okroshka-kefir" }
        assertTrue(okroshka.prepared)
        assertTrue(StarterCatalog.matches(okroshka.food.name, "окрошка на кефире"))
        assertEquals("g", okroshka.food.unit)
        assertEquals(74.55, okroshka.food.caloriesPer100, 0.0)
        assertEquals(3.88, okroshka.food.proteinPer100, 0.0)
        assertEquals(3.28, okroshka.food.fatPer100, 0.0)
        assertEquals(7.59, okroshka.food.carbohydratePer100, 0.0)
    }

    @Test
    fun meatlessBorschtIsSearchableAsASeparatePreparedRecipe() {
        val source = RegionalSource.NIAMNIAM_RECIPES
        val recipes = StarterCatalog.loadRegional(ApplicationProvider.getApplicationContext(), source)
        val borscht = recipes.single { it.id == "lean-borscht" }
        assertTrue(borscht.prepared)
        assertTrue(StarterCatalog.matches(borscht.food.name, "борщ постный"))
        assertFalse(StarterCatalog.matches(borscht.food.name, "борщ с говядиной"))
        assertEquals("g", borscht.food.unit)
        assertEquals(44.47, borscht.food.caloriesPer100, 0.0)
        assertEquals(1.0, borscht.food.proteinPer100, 0.0)
        assertEquals(1.79, borscht.food.fatPer100, 0.0)
        assertEquals(6.56, borscht.food.carbohydratePer100, 0.0)
    }

    @Test
    fun mushroomRisottoUsesPreparedRiceAndCookedMushrooms() {
        val source = RegionalSource.NIAMNIAM_RECIPES
        val recipes = StarterCatalog.loadRegional(ApplicationProvider.getApplicationContext(), source)
        val risotto = recipes.single { it.id == "mushroom-risotto" }
        assertTrue(risotto.prepared)
        assertTrue(StarterCatalog.matches(risotto.food.name, "ризотто с грибами"))
        assertEquals(125.35, risotto.food.caloriesPer100, 0.0)
        assertEquals(3.69, risotto.food.proteinPer100, 0.0)
        assertEquals(3.15, risotto.food.fatPer100, 0.0)
        assertEquals(20.32, risotto.food.carbohydratePer100, 0.0)
    }

    @Test
    fun britishCatalogFindsCannedSpaghettiWithSourceValuesPer100Grams() {
        val source = RegionalSource.entries.single { it.asset == "cofid_2021_sample.json" }
        assertEquals("OGL 3.0", source.license)
        val dishes = StarterCatalog.loadRegional(ApplicationProvider.getApplicationContext(), source)
        assertEquals(9, dishes.map { it.id }.toSet().size)
        val spaghetti = dishes.single { it.id == "11-735" }
        assertTrue(spaghetti.prepared)
        assertTrue(StarterCatalog.matches(spaghetti.food.name, "спагетти с томатным соусом"))
        assertTrue(spaghetti.food.name.contains("консервированные"))
        assertEquals("g", spaghetti.food.unit)
        assertEquals(72.0, spaghetti.food.caloriesPer100, 0.0)
        assertEquals(2.1, spaghetti.food.proteinPer100, 0.0)
        assertEquals(0.3, spaghetti.food.fatPer100, 0.0)
        assertEquals(16.2, spaghetti.food.carbohydratePer100, 0.0)
        assertFalse(dishes.any { StarterCatalog.matches(it.food.name, "самса с мясом") })
    }

    @Test
    fun germanCatalogRetainsBlsIdentifiersAndNutrition() {
        val foods = StarterCatalog.loadRegional(ApplicationProvider.getApplicationContext(), RegionalSource.MAX_RUBNER_INSTITUTE)
        assertEquals(33, foods.size)
        assertEquals(foods.size, foods.map { it.id }.distinct().size)
        val porridge = foods.single { it.id == "X9A2010" }
        assertTrue(porridge.prepared)
        assertTrue(StarterCatalog.matches(porridge.food.name, "каша пшённая молочная"))
        assertEquals(134.0, porridge.food.caloriesPer100, 0.0)
        assertEquals(6.0, porridge.food.proteinPer100, 0.0)
        assertEquals(4.18, porridge.food.fatPer100, 0.0)
        assertEquals(17.82, porridge.food.carbohydratePer100, 0.0)
        val pollock = foods.single { it.id == "T213152" }
        assertTrue(pollock.prepared)
        assertTrue(StarterCatalog.matches(pollock.food.name, "минтай тушёный"))
        assertEquals("g", pollock.food.unit)
        assertEquals(51.0, pollock.food.caloriesPer100, 0.0)
        assertEquals(11.0, pollock.food.proteinPer100, 0.0)
        assertEquals(0.78, pollock.food.fatPer100, 0.0)
        assertEquals(0.0, pollock.food.carbohydratePer100, 0.0)
        val casserole = foods.single { it.id == "Y880853" }
        assertTrue(casserole.prepared)
        assertEquals(232.0, casserole.food.caloriesPer100, 0.0)
        assertEquals(10.5, casserole.food.proteinPer100, 0.0)
        assertEquals(11.1, casserole.food.fatPer100, 0.0)
        assertEquals(21.5, casserole.food.carbohydratePer100, 0.0)
    }
    @Test
    fun cookedBuckwheatRetainsTheSeparateCookedNutritionPer100Grams() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val cooked = StarterCatalog.load(context).single { StarterCatalog.matches(it.name, "гречка варёная") }
        val dry = StarterCatalog.loadRegional(context, RegionalSource.SWEDISH_FOOD_AGENCY).single { it.id == "825" }.food
        assertFalse(StarterCatalog.matches(dry.name, "гречка варёная"))
        assertEquals("g", cooked.unit)
        assertEquals(92.0, cooked.caloriesPer100, 0.0)
        assertEquals(3.38, cooked.proteinPer100, 0.0)
        assertEquals(0.62, cooked.fatPer100, 0.0)
        assertEquals(19.94, cooked.carbohydratePer100, 0.0)
    }
    @Test
    fun searchMatchesWordPrefixesWithoutMatchingTheMiddleOfAnotherWord() {
        assertFalse(StarterCatalog.matches("Гречка сухая", "уха"))
        assertTrue(StarterCatalog.matches("Уха из судака", "уха"))
        assertTrue(StarterCatalog.matches("Гречка сухая", "греч"))
        assertTrue(StarterCatalog.matches("Свёкла сырая", "свек сырая"))
        assertTrue(StarterCatalog.matches("Баклажан / Eggplant, raw", "egg raw"))
        assertTrue(StarterCatalog.matches("Хачапури по-аджарски", "по-адж"))
        assertFalse(StarterCatalog.matches("Сыр полутвёрдый", "сыр твёрдый"))
    }
    @Test
    fun swedishCatalogPreservesSeparateSourceIdsAndNutrition() {
        val entries = StarterCatalog.loadRegional(ApplicationProvider.getApplicationContext(), RegionalSource.SWEDISH_FOOD_AGENCY)
        assertEquals(36, entries.map { it.id }.distinct().size)
        val baked = entries.single { it.id == "6345" }
        assertTrue(baked.prepared)
        assertTrue(StarterCatalog.matches(baked.food.name, "запеканка картофельная с мясом"))
        assertEquals(144.0, baked.food.caloriesPer100, 0.0)
        assertFalse(StarterCatalog.matches(entries.single { it.id == "3590" }.food.name, "шарлотка с яблоками"))
        val soup = entries.single { it.id == "6451" }
        assertTrue(soup.prepared)
        assertTrue(StarterCatalog.matches(soup.food.name, "щи из свежей капусты"))
        assertEquals("g", soup.food.unit)
        assertEquals(48.0, soup.food.caloriesPer100, 0.0)
        assertEquals(0.9, soup.food.proteinPer100, 0.0)
        assertEquals(2.9, soup.food.fatPer100, 0.0)
        assertEquals(4.0, soup.food.carbohydratePer100, 0.0)
        assertEquals(13, entries.count { !it.prepared })
        val rice = entries.single { it.id == "2485" }
        assertFalse(rice.prepared)
        assertTrue(StarterCatalog.matches(rice.food.name, "рис белый сухой"))
        val porridge = entries.single { it.id == "655" }
        assertTrue(porridge.prepared)
        assertTrue(StarterCatalog.matches(porridge.food.name, "каша рисовая молочная"))
        val moussaka = entries.single { it.id == "515" }
        assertTrue(moussaka.prepared)
        assertTrue(StarterCatalog.matches(moussaka.food.name, "мусака с баклажанами"))
        assertEquals(117.0, moussaka.food.caloriesPer100, 0.0)
        assertEquals(6.8, moussaka.food.proteinPer100, 0.0)
        assertEquals(7.7, moussaka.food.fatPer100, 0.0)
        assertEquals(4.6, moussaka.food.carbohydratePer100, 0.0)
    }
    @Test
    fun preparedBrandedPierogiPreserveDistinctFillingsAndUsdaNutrition() {
        val products = StarterCatalog.loadPackaged(ApplicationProvider.getApplicationContext())
        val potato = products.single { it.code == "0797589001046" }
        val cheese = products.single { it.code == "0797589001022" }

        assertEquals("USDA FoodData Central", potato.sourceLabel)
        assertEquals(potato, StarterCatalog.findPackagedByBarcode(products, "797589001046"))
        assertEquals(potato, StarterCatalog.findPackagedByBarcode(products, "0797589001046"))
        assertTrue(StarterCatalog.isPreparedPackagedDish(potato))
        assertTrue(StarterCatalog.isPreparedPackagedDish(cheese))
        assertTrue(StarterCatalog.matches(potato.food.name, "вареники с картофелем"))
        assertFalse(StarterCatalog.matches(potato.food.name, "вареники с творогом"))
        assertTrue(StarterCatalog.matches(cheese.food.name, "вареники с творогом"))
        assertTrue(StarterCatalog.displayName(potato.food.name, true).startsWith("POTATO AND ONION PIEROGI"))
        assertTrue(StarterCatalog.displayName(potato.food.name, false).startsWith("Вареники с картофелем и луком"))
        assertEquals("g", cheese.food.unit)
        assertEquals(163.0, cheese.food.caloriesPer100, 0.0)
        assertEquals(7.32, cheese.food.proteinPer100, 0.0)
        assertEquals(2.85, cheese.food.fatPer100, 0.0)
        assertEquals(27.64, cheese.food.carbohydratePer100, 0.0)
    }

    @Test
    fun preparedStoreDishesRetainTheirOwnCodesAndNutrition() {
        val products = StarterCatalog.loadPackaged(ApplicationProvider.getApplicationContext())
        val dishes = products.filter(StarterCatalog::isPreparedPackagedDish)
        assertEquals(20, dishes.map { it.code }.distinct().size)
        val chebureki = dishes.single { it.code == "4607025784319" }.food
        assertTrue(StarterCatalog.matches(chebureki.name, "чебурек с мясом"))
        assertTrue(chebureki.name.contains("готовые, замороженные"))
        assertEquals(270.0, chebureki.caloriesPer100, 0.0)
        val crepes = dishes.single { it.code == "0001332185069" }.food
        assertTrue(StarterCatalog.matches(crepes.name, "блины с творогом"))
        assertEquals(188.0, crepes.caloriesPer100, 0.0)
        assertEquals(4, dishes.count { StarterCatalog.matches(it.food.name, "борщ с говядиной") })
        assertEquals(2, dishes.count { StarterCatalog.matches(it.food.name, "салат из морской капусты") })
        assertEquals(3, dishes.count { StarterCatalog.matches(it.food.name, "сырники") })
        assertEquals(3, dishes.count { StarterCatalog.matches(it.food.name, "винегрет") })
        val chickenCaesar = dishes.single { StarterCatalog.matches(it.food.name, "салат цезарь с курицей") }
        assertEquals("4607105666818", chickenCaesar.code)
        assertEquals(230.0, chickenCaesar.food.caloriesPer100, 0.0)
        val beefKharcho = dishes.single { it.code == "4650322719447" }.food
        assertTrue(StarterCatalog.matches(beefKharcho.name, "харчо с говядиной"))
        assertEquals(75.8, beefKharcho.caloriesPer100, 0.0)
        assertFalse(StarterCatalog.matches(dishes.single { it.code == "4606411012159" }.food.name, "харчо с говядиной"))
        val seaweed = dishes.single { it.code == "4680009030404" }.food
        assertEquals(122.0, seaweed.caloriesPer100, 0.0)
        assertFalse(seaweed.name.contains("&quot;"))
        assertEquals(69.2, dishes.single { it.code == "2100100337919" }.food.caloriesPer100, 0.00001)
        assertEquals(37.3, dishes.single { it.code == "4640107814207" }.food.caloriesPer100, 0.00001)
    }
    @Test
    fun readyDishesAreSearchableWithSourceNutritionAndWithoutInventedFillings() {
        val foods = StarterCatalog.load(ApplicationProvider.getApplicationContext())
        val dishes = foods.filter(StarterCatalog::isPreparedDish)
        assertEquals(85, dishes.size)
        val chickenSkewer = dishes.single { StarterCatalog.matches(it.name, "шашлык из курицы") }
        assertEquals(104.0, chickenSkewer.caloriesPer100, 0.0)
        val meatballs = dishes.filter { StarterCatalog.matches(it.name, "тефтели с рисом") }
        assertEquals(2, meatballs.size)
        val tomatoMeatballs = meatballs.single { it.name.contains("томатном соусе") }
        assertEquals(153.0, tomatoMeatballs.caloriesPer100, 0.0)
        assertEquals(13.4, tomatoMeatballs.carbohydratePer100, 0.0)
        val rolls = dishes.single { StarterCatalog.matches(it.name, "голубцы с говядиной и рисом") }
        assertEquals(114.0, rolls.caloriesPer100, 0.0)
        assertTrue(StarterCatalog.matches(rolls.name, "голубцы с мясом и рисом"))
        assertTrue(dishes.any { StarterCatalog.matches(it.name, "борщ") })
        assertTrue(dishes.none { StarterCatalog.matches(it.name, "борщ постный") })
        assertTrue(dishes.none { StarterCatalog.matches(it.name, "вареники с картофелем") })
        val cucumberSalad = dishes.single { StarterCatalog.matches(it.name, "салат из огурцов и помидоров") }
        assertTrue(cucumberSalad.name.contains("с маслом и уксусом"))
        assertTrue(dishes.none { StarterCatalog.matches(it.name, "оладьи") })
        val drumsticks = dishes.filter { StarterCatalog.matches(it.name, "куриная голень запеченная") }
        assertEquals(2, drumsticks.size)
        assertTrue(drumsticks.any { it.name.contains("с кожей") && it.name.contains("skin eaten") })
        assertTrue(drumsticks.any { it.name.contains("без кожи") && it.name.contains("skin not eaten") })
        val bakedChicken = dishes.filter { StarterCatalog.matches(it.name, "курица запеченная") }
        assertEquals(2, bakedChicken.size)
        assertTrue(bakedChicken.any { it.name.contains("без кожи") && it.name.contains("skin not eaten") })
    }
    @Test
    fun regionalDishesPreserveSourceNamesAndPortionBasis() {
        val dishes = StarterCatalog.load(ApplicationProvider.getApplicationContext()).filter(StarterCatalog::isPreparedDish)
        val expected = mapOf(
            "суп гаспачо" to "Soup, gazpacho",
            "суп минестроне" to "Soup, minestrone",
            "суп рыбный буйабес" to "Soup, bouillabaisse",
            "суп луковый французский" to "Soup, French onion",
            "гренки французские" to "French toast, plain",
            "картофельный салат по-немецки" to "Potato salad, German style",
            "пирог с яблоками" to "Pie, apple",
            "сэндвич с курицей" to "Chicken fillet sandwich, grilled, on white bun",
            "котлета куриная" to "Chicken patty, breaded",
            "свинина тушёная" to "Stewed pork, Puerto Rican style",
            "шашлык из свинины" to "Pork shish kabob with vegetables, excluding potatoes",
            "суп сырный" to "Soup, broccoli cheese",
            "бутерброд с колбасой" to "Bologna sandwich on white",
            "суп с вермишелью" to "Soup, sopa de fideo aguada",
        )
        for ((query, sourceName) in expected) {
            val food = dishes.single { it.name.endsWith(" / $sourceName") }
            assertTrue(StarterCatalog.matches(food.name, query))
            assertEquals("g", food.unit)
        }
    }
    @Test
    fun commonFoodsAreDiscoverableInRussianWithoutChangingPreparation() {
        val foods = StarterCatalog.load(ApplicationProvider.getApplicationContext())
        val oatmeal = foods.filter { StarterCatalog.matches(it.name, "овсяная каша") }
        assertEquals(2, oatmeal.size)
        assertTrue(oatmeal.any { it.name.contains("на воде") && it.name.contains("made with water") })
        assertTrue(oatmeal.any { it.name.contains("на молоке") && it.name.contains("made with milk") })
        assertTrue(foods.take(30).any { StarterCatalog.matches(it.name, "свекла сырая") })
    }
    @Test
    fun newRussianProduceNamesKeepTheOriginalFoodAndPreparation() {
        val foods = StarterCatalog.load(ApplicationProvider.getApplicationContext())
        val expected = mapOf(
            "лимон" to "Lemon, raw",
            "виноград" to "Grapes, raw",
            "капуста белокочанная" to "Cabbage, green, raw",
            "цветная капуста" to "Cauliflower, raw",
            "баклажан" to "Eggplant, raw",
            "перец сладкий зелёный" to "Peppers, sweet, green, raw",
            "перец сладкий красный" to "Peppers, sweet, red, raw",
            "фасоль варёная" to "Beans, from dried, NS as to type, no added fat",
            "чечевица варёная" to "Lentils, from dried, no added fat",
            "сыр твёрдый" to "Cheese, Parmesan, hard",
        )
        for ((query, sourceName) in expected) {
            val food = foods.single { it.name.endsWith(" / $sourceName") }
            assertTrue(StarterCatalog.matches(food.name, query))
            assertEquals("g", food.unit)
        }
    }
    @Test
    fun packagedProductHasBarcodeAndNutritionFromOfflineAsset() {
        val products = StarterCatalog.loadPackaged(ApplicationProvider.getApplicationContext())
        val product = products.single { it.code == "4810268047602" }
        assertTrue(product.food.name.contains("Сметана"))
        assertEquals(202.0, product.food.caloriesPer100, 0.0)
        assertEquals(20.0, product.food.fatPer100, 0.0)
    }
}
