package com.niamniam.kbjy.data

import android.content.Context
import org.json.JSONObject

data class PackagedFood(val code: String, val food: Food, val sourceLabel: String, val prepared: Boolean)
enum class RegionalSource(val asset: String, val label: String, val license: String) {
    SWEDISH_FOOD_AGENCY("swedish_food_agency.json", "Swedish Food Agency", "CC BY 4.0"),
    MAX_RUBNER_INSTITUTE("bls4_sample.json", "Max Rubner-Institut · BLS 4.0", "CC BY 4.0"),
    PUBLIC_HEALTH_ENGLAND("cofid_2021_sample.json", "Public Health England · CoFID 2021", "OGL 3.0"),
    NIAMNIAM_RECIPES("reference_recipes.json", "NiamNiam · расчёт / estimate", "USDA · CC0 / BLS · CC BY 4.0"),
}

data class RegionalFood(val id: String, val food: Food, val prepared: Boolean)

object StarterCatalog {
    private val cyrillic = Regex("\\p{IsCyrillic}")

    fun displayName(name: String, english: Boolean): String {
        val russian = name.substringBefore(" / ")
        val other = name.substringAfter(" / ", "")
        if (other.isBlank() || !cyrillic.containsMatchIn(russian) || cyrillic.containsMatchIn(other)) return name
        return if (english) other else russian.removeSuffix(" (рецептура USDA)")
    }

    private val essentials = setOf(2710258, 2707755, 2707600, 2705385, 2709770)
    fun matches(name: String, query: String): Boolean {
        fun normalize(value: String) = value.lowercase().replace('ё', 'е').trim()
        val words = normalize(query).split(Regex("\\s+")).filter { it.isNotEmpty() }
        val title = normalize(name)
        return words.all { word ->
            title.indices.any { index ->
                (index == 0 || !title[index - 1].isLetterOrDigit()) && title.startsWith(word, index)
            }
        }
    }
    private val russianNames = mapOf(
        2708408 to "Рис белый варёный без добавленного жира",
        2706029 to "Бедро куриное запечённое с кожей",
        2706030 to "Бедро куриное запечённое без кожи",
        2708362 to "Гречневая крупа",
        2707152 to "Яйцо сырое целое",
        2709388 to "Картофель варёный без кожуры и добавленного жира",
        2709660 to "Морковь сырая",
        2709784 to "Огурец сырой",
        2709215 to "Яблоко сырое",
        2705385 to "Молоко цельное",
        2710258 to "Сахар белый",
        2707755 to "Хлеб ржаной",
        2707600 to "Батон белый (хлеб пшеничный)",
        2705394 to "Кефир",
        2705956 to "Куриная грудка запечённая без кожи",
        2707154 to "Яйцо варёное или пашот",
        2708357 to "Макароны варёные",
        2708381 to "Овсяная каша на воде без добавленного жира",
        2708383 to "Овсяная каша на молоке без добавленного жира",
        2709171 to "Апельсин сырой",
        2709223 to "Авокадо сырое",
        2709224 to "Банан сырой",
        2709254 to "Груша сырая",
        2709275 to "Голубика сырая",
        2709283 to "Клубника сырая",
        2709614 to "Шпинат сырой",
        2709643 to "Брокколи сырая",
        2709719 to "Помидор сырой (томат)",
        2709770 to "Свёкла сырая",
        2709773 to "Капуста белокочанная зелёная сырая",
        2709786 to "Чеснок сырой",
        2709795 to "Лук репчатый сырой",
        2709168 to "Лимон сырой",
        2709237 to "Виноград сырой",
        2709777 to "Цветная капуста сырая",
        2709785 to "Баклажан сырой",
        2709800 to "Перец сладкий зелёный сырой",
        2709801 to "Перец сладкий красный сырой",
        2707349 to "Фасоль варёная из сухой без добавленного жира (сорт не уточнён)",
        2707425 to "Чечевица варёная из сухой без добавленного жира",
        2705730 to "Сыр твёрдый пармезан",
    )

    private val preparedDishNames = mapOf(
        2710105 to "Борщ (состав не уточнён)",
        2706382 to "Бефстроганов из говядины",
        2706620 to "Голубцы с говядиной и рисом (с мясом)",
        2709552 to "Драники (картофельные)",
        2708720 to "Вареники (начинка не уточнена)",
        2708342 to "Блины без начинки",
        2708719 to "Блины с мясом",
        2707459 to "Суп гороховый",
        2707458 to "Суп гороховый с мясом",
        2709149 to "Суп куриный с лапшой",
        2707453 to "Суп фасолевый",
        2707462 to "Суп чечевичный",
        2709718 to "Суп тыквенный",
        2709757 to "Суп томатный",
        2709556 to "Суп картофельный",
        2710113 to "Суп овощной",
        2710107 to "Суп-пюре из грибов",
        2709073 to "Перец фаршированный рисом и мясом",
        2709496 to "Картофельное пюре с молоком",
        2709982 to "Капуста квашеная",
        2706592 to "Говядина тушёная (рагу)",
        2707123 to "Суп с фрикадельками",
        2707143 to "Суп рыбный или с креветками (вариант не уточнён)",
        2707201 to "Омлет или яичница-болтунья на сливочном масле",
        2707229 to "Омлет или яичница-болтунья с помидорами, с жиром",
        2707230 to "Омлет или яичница-болтунья с помидорами, без добавленного жира",
        2707156 to "Яичница без добавленного жира",
        2707159 to "Яичница на сливочном масле",
        2706002 to "Куриная голень запечённая с кожей",
        2706003 to "Куриная голень запечённая без кожи",
        2709384 to "Картофель запечённый без кожуры",
        2709524 to "Картофель запечённый с кожурой",
        2709872 to "Свёкла отварная или приготовленная без добавленного жира",
        2706843 to "Салат с крабовыми палочками",
        2709220 to "Яблоко печёное",
        2705933 to "Курица запечённая с кожей",
        2705934 to "Курица запечённая без кожи",
        2706037 to "Куриное бедро тушёное с кожей",
        2706038 to "Куриное бедро тушёное без кожи",
        2705882 to "Свинина запечённая",
        2706467 to "Фрикадельки мясные с соусом (вид мяса не уточнён)",
        2706550 to "Котлеты рыбные (вид рыбы не уточнён)",
        2706552 to "Котлеты рыбные из лосося",
        2706286 to "Лосось запечённый или приготовленный под грилем",
        2709458 to "Картофель фри из свежего картофеля",
        2705797 to "Бутерброд с сыром чеддер на белом хлебе",
        2705798 to "Бутерброд с сыром чеддер на пшеничном хлебе",
        2708753 to "Лазанья с мясом домашняя",
        2710028 to "Рататуй",
        2706225 to "Рыба запечённая или приготовленная под грилем (вид не уточнён)",
        2706227 to "Рыба жареная (вид не уточнён)",
        2706241 to "Треска запечённая или приготовленная под грилем",
        2706244 to "Треска жареная",
        2706264 to "Скумбрия запечённая или приготовленная под грилем",
        2706303 to "Форель запечённая или приготовленная под грилем",
        2709756 to "Салат из огурцов и помидоров с маслом и уксусом",
        2709830 to "Греческий салат без заправки",
        2710084 to "Грибы маринованные",
        2709816 to "Салат из капусты (состав не уточнён)",
        2708831 to "Макароны с томатным соусом домашние",
        2708838 to "Макароны с томатным соусом и мясом домашние",
        2706492 to "Тефтели с рисом в томатном соусе",
        2706496 to "Тефтели с рисом в грибном соусе",
        2709659 to "Суп из брокколи с сыром (сырный)",
        2707132 to "Бульон (вид не уточнён)",
        2710106 to "Суп гаспачо",
        2710114 to "Суп минестроне",
        2706717 to "Суп рыбный буйабес",
        2710108 to "Суп луковый французский",
        2708335 to "Гренки французские без добавок",
        2709541 to "Картофельный салат по-немецки",
        2709532 to "Картофельный салат с яйцом и майонезом",
        2707182 to "Яичный салат с майонезом",
        2709758 to "Суп томатный кремовый",
        2709557 to "Суп картофельный с мясом",
        2707995 to "Пирог с яблоками",
        2707012 to "Сэндвич с курицей гриль на белой булочке",
        2707014 to "Сэндвич с курицей гриль на пшеничной булочке",
        2706088 to "Котлета куриная в панировке",
        2706412 to "Свинина тушёная по-пуэрторикански",
        2706779 to "Шашлык из свинины с овощами (без картофеля)",
        2706783 to "Шашлык из курицы с овощами (рецептура USDA)",
        2707040 to "Бутерброд с варёной колбасой на белом хлебе",
        2707046 to "Бутерброд с колбасой салями на белом хлебе",
        2709161 to "Суп с вермишелью по-мексикански (сопа-де-фидео)",
    )
    private val preparedDishTitles = preparedDishNames.values.toSet()

    fun isPreparedDish(food: Food): Boolean = food.name.substringBefore(" / ") in preparedDishTitles

    // Individual products reviewed against the bundled Open Food Facts snapshot.
    private val preparedPackagedCodes = setOf(
        "2100100337919", "4640107814207", "2090006044519", "4660301759562",
        "4680009030404", "4607010730727", "2090006041051", "2100100391232", "4660019678735",
        "2100100580148", "2090006003707", "4620747668668", "4650322719447", "4606411012159",
        "4607105666818", "2100100667238",
        "4607025784319", "0001332185069",
    )

    fun isPreparedPackagedDish(product: PackagedFood): Boolean =
        product.prepared || product.code in preparedPackagedCodes

    fun findPackagedByBarcode(products: List<PackagedFood>, code: String): PackagedFood? {
        products.firstOrNull { it.code == code }?.let { return it }
        val alternate = when {
            code.length == 12 && code.all { it in '0'..'9' } -> "0$code"
            code.length == 13 && code.startsWith('0') && code.all { it in '0'..'9' } -> code.drop(1)
            else -> return null
        }
        return products.firstOrNull { it.code == alternate }
    }

    fun load(context: Context): List<Food> {
        val text = context.assets.open("fndds_2021_2023.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val foods = JSONObject(text).getJSONArray("foods")
        val fndds = List(foods.length()) { foods.getJSONObject(it) }
            .sortedBy { item ->
                val id = item.getInt("fdcId")
                when (id) {
                    in essentials -> 0
                    in russianNames -> 1
                    in preparedDishNames -> 2
                    else -> 3
                }
            }
            .map { item ->
                val englishName = item.getString("nameEn")
                val id = item.getInt("fdcId")
                val russianName = russianNames[id] ?: preparedDishNames[id]
                foodPer100g(item, if (russianName == null) englishName else "$russianName / $englishName")
            }
        val srText = context.assets.open("usda_sr28_sample.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val referenceFoods = JSONObject(srText).getJSONArray("foods")
        val sr28 = List(referenceFoods.length()) { index ->
            val item = referenceFoods.getJSONObject(index)
            foodPer100g(item, item.getString("nameRu") + " / " + item.getString("nameEn"))
        }
        val saltPrefix = "Соль поваренная / "
        return sr28.filter { it.name.startsWith(saltPrefix) } + fndds + sr28.filterNot { it.name.startsWith(saltPrefix) }
    }

    private fun foodPer100g(item: JSONObject, name: String) = Food(
        name = name,
        unit = "g",
        caloriesPer100 = item.getDouble("calories"),
        proteinPer100 = item.getDouble("protein"),
        fatPer100 = item.getDouble("fat"),
        carbohydratePer100 = item.getDouble("carbohydrate"),
    )

    fun loadPackaged(context: Context): List<PackagedFood> =
        (loadPackagedAsset(context, "off_regional_sample.json", "Open Food Facts") +
            loadPackagedAsset(context, "usda_branded_sample.json", "USDA FoodData Central")).distinctBy { it.code }

    private fun loadPackagedAsset(context: Context, asset: String, sourceLabel: String): List<PackagedFood> {
        val text = context.assets.open(asset).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val products = JSONObject(text).getJSONArray("products")
        return List(products.length()) { index ->
            val item = products.getJSONObject(index)
            val brand = item.optString("brand")
            fun withBrand(title: String) = listOf(title, brand).filter { it.isNotBlank() }.joinToString(" · ")
            val title = item.getString("name")
            val translation = item.optString("nameEn")
            val name = if (translation.isBlank()) withBrand(title) else "${withBrand(title)} / ${withBrand(translation)}"
            PackagedFood(item.getString("code"), foodPer100g(item, name), sourceLabel, item.optBoolean("prepared"))
        }
    }

    fun loadRegional(context: Context, source: RegionalSource): List<RegionalFood> {
        val text = context.assets.open(source.asset).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val foods = JSONObject(text).getJSONArray("foods")
        return List(foods.length()) { index ->
            val item = foods.getJSONObject(index)
            RegionalFood(
                item.get("id").toString(),
                foodPer100g(item, item.getString("nameRu") + " / " + item.getString("nameEn")),
                item.getBoolean("prepared"),
            )
        }
    }
}
