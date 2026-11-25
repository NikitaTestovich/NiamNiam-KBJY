package com.niamniam.kbjy

import com.niamniam.kbjy.data.StarterCatalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.niamniam.kbjy.data.Food
import com.niamniam.kbjy.profile.RecipeIngredientRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.util.Locale
import java.util.UUID
import javax.net.ssl.HttpsURLConnection

private fun proposalUnit(unit: String) = if (unit == "мл" || unit == "ml") "ml" else "g"

internal fun catalogProposal(id: String, food: Food, cookedGrams: Double?, ingredients: List<RecipeIngredientRecord>): JSONObject =
    JSONObject().put("id", id).put("name", food.name)
        .put("unit", proposalUnit(food.unit)).put("kcal", food.caloriesPer100)
        .put("protein", food.proteinPer100).put("fat", food.fatPer100).put("carbs", food.carbohydratePer100)
        .put("source", if (cookedGrams == null) "manual" else "recipe").apply {
            if (cookedGrams != null) {
                put("cookedGrams", cookedGrams)
                put("ingredients", JSONArray(ingredients.map {
                    JSONObject().put("name", it.name).put("amount", it.amount).put("unit", proposalUnit(it.unit))
                }))
            }
        }

private fun sendCatalogProposal(body: JSONObject): JSONObject {
    fun connect(url: URL) = (url.openConnection() as HttpsURLConnection).apply {
        connectTimeout = 12_000
        readTimeout = 15_000
        instanceFollowRedirects = false
    }
    fun read(connection: HttpsURLConnection): JSONObject {
        check(connection.responseCode == 200)
        return connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
            val buffer = CharArray(4097)
            val text = StringBuilder()
            while (true) {
                val count = reader.read(buffer)
                if (count < 0) break
                check(text.length + count <= 4096)
                text.append(buffer, 0, count)
            }
            JSONObject(text.toString())
        }
    }
    val post = connect(URL(BuildConfig.CATALOG_INTAKE_URL))
    val redirect: URL
    try {
        post.requestMethod = "POST"
        post.doOutput = true
        post.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        post.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        if (post.responseCode !in listOf(302, 303)) return read(post)
        redirect = URL(checkNotNull(post.getHeaderField("Location")))
    } finally { post.disconnect() }
    // Apps Script ContentService returns the response at this host. The POST body stays with the intake.
    check(redirect.protocol == "https" && redirect.host == "script.googleusercontent.com" &&
        redirect.userInfo == null && redirect.port in listOf(-1, 443))
    val get = connect(redirect)
    try { return read(get) } finally { get.disconnect() }
}

@Composable
internal fun CatalogProposalDialog(food: Food, cookedGrams: Double?, ingredients: List<RecipeIngredientRecord>,
                                   english: Boolean, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val id = rememberSaveable { UUID.randomUUID().toString() }
    var busy by remember { mutableStateOf(false) }
    var result by rememberSaveable { mutableStateOf<String?>(null) }
    fun tr(ru: String, en: String) = if (english) en else ru
    val formatted = "%.2f / %.2f / %.2f / %.2f".format(if (english) Locale.ENGLISH else Locale.forLanguageTag("ru"),
        food.caloriesPer100, food.proteinPer100, food.fatPer100, food.carbohydratePer100)
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text(tr("Предложить в каталог", "Suggest to catalog")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(StarterCatalog.displayName(food.name, english))
                val unit = displayUnit(food.unit, english)
                Text(tr("Ккал / белки / жиры / углеводы на 100 $unit", "Calories / protein / fat / carbs per 100 $unit"))
                Text(formatted)
                if (cookedGrams != null) {
                    val weight = formatDecimal(cookedGrams, english)
                    Text(tr("Вес готового блюда: $weight г", "Cooked weight: $weight g"))
                    ingredients.forEach { ingredient ->
                        Text("${StarterCatalog.displayName(ingredient.name, english)} · ${formatDecimal(ingredient.amount, english)} ${displayUnit(ingredient.unit, english)}")
                    }
                }
                val message = when (result) {
                    "sent" -> tr("Заявка отправлена на проверку.", "Suggestion submitted for review.")
                    "invalid_submission" -> tr("Проверьте название, КБЖУ и состав блюда.", "Check the name, nutrition and recipe details.")
                    "unavailable" -> tr("Не удалось отправить заявку. Проверьте интернет и повторите попытку.", "Couldn't submit. Check your connection and try again.")
                    else -> null
                }
                message?.let { Text(it, color = if (result == "sent") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            if (result == "sent") Button(onClick = onDismiss) { Text(tr("Готово", "Done")) }
            else Button(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    try {
                        val response = withContext(Dispatchers.IO) { sendCatalogProposal(catalogProposal(id, food, cookedGrams, ingredients)) }
                        result = when {
                            response.optBoolean("ok") && response.optString("id") == id -> "sent"
                            response.optString("error") == "invalid_submission" -> "invalid_submission"
                            else -> "unavailable"
                        }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        result = "unavailable"
                    } finally { busy = false }
                }
            }) { Text(if (busy) tr("Отправка…", "Sending…") else tr("Отправить", "Submit")) }
        },
        dismissButton = { if (result != "sent") BackButton(english, enabled = !busy, onClick = onDismiss) },
    )
}
