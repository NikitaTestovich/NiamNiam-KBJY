package com.niamniam.kbjy.data

import android.content.Context
import org.json.JSONObject

data class PackagedFood(val code: String, val food: Food)

object StarterCatalog {
    fun matches(name: String, query: String): Boolean {
        fun normalize(value: String) = value.lowercase().replace('ё', 'е').trim()
        val words = normalize(query).split(Regex("\\s+")).filter { it.isNotEmpty() }
        val title = normalize(name)
        return words.all { title.contains(it) }
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
    )

    fun load(context: Context): List<Food> {
        val text = context.assets.open("fndds_2021_2023.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val foods = JSONObject(text).getJSONArray("foods")
        return List(foods.length()) { index ->
            val item = foods.getJSONObject(index)
            val englishName = item.getString("nameEn")
            val russianName = russianNames[item.getInt("fdcId")]
            Food(
                name = if (russianName == null) englishName else "$russianName / $englishName",
                unit = "g",
                caloriesPer100 = item.getDouble("calories"),
                proteinPer100 = item.getDouble("protein"),
                fatPer100 = item.getDouble("fat"),
                carbohydratePer100 = item.getDouble("carbohydrate"),
            )
        }
    }

    fun loadPackaged(context: Context): List<PackagedFood> {
        val text = context.assets.open("off_regional_sample.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val products = JSONObject(text).getJSONArray("products")
        return List(products.length()) { index ->
            val item = products.getJSONObject(index)
            val brand = item.optString("brand")
            PackagedFood(
                item.getString("code"),
                Food(
                    name = listOf(item.getString("name"), brand).filter { it.isNotBlank() }.joinToString(" · "),
                    unit = "g",
                    caloriesPer100 = item.getDouble("calories"),
                    proteinPer100 = item.getDouble("protein"),
                    fatPer100 = item.getDouble("fat"),
                    carbohydratePer100 = item.getDouble("carbohydrate"),
                ),
            )
        }
    }
}
