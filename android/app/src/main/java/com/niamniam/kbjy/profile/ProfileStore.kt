package com.niamniam.kbjy.profile

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class Profile(
    val sex: Sex,
    val age: Int,
    val heightCm: Double,
    val weightKg: Double,
    val activityFactor: Double,
    val language: AppLanguage,
    val theme: AppTheme,
)

data class ProfileState(val profile: Profile, val goals: List<DatedGoal>)
class ProfileDataException(cause: Throwable) : IllegalStateException("Unable to read saved profile", cause)

class ProfileStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("profile", Context.MODE_PRIVATE)

    fun load(): ProfileState? {
        val text = preferences.getString("state", null) ?: return null
        return try {
            val root = JSONObject(text)
            val profile = Profile(
                Sex.valueOf(root.getString("sex")),
                root.getInt("age"),
                root.getDouble("height"),
                root.getDouble("weight"),
                root.getDouble("activity"),
                AppLanguage.valueOf(root.getString("language")),
                AppTheme.valueOf(root.getString("theme")),
            )
            val array = root.getJSONArray("goals")
            val goals = (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                DatedGoal(
                    LocalDate.parse(item.getString("date")),
                    DailyGoal(
                        item.getDouble("calories"), item.getDouble("protein"),
                        item.getDouble("fat"), item.getDouble("carbohydrate"),
                        if (item.isNull("water")) null else item.getDouble("water"),
                        MacroMode.valueOf(item.getString("mode")),
                    ),
                )
            }
            ProfileState(profile, goals)
        } catch (exception: IllegalArgumentException) {
            throw ProfileDataException(exception)
        } catch (exception: org.json.JSONException) {
            throw ProfileDataException(exception)
        }
    }

    fun save(profile: Profile, goal: DailyGoal?, date: LocalDate = LocalDate.now()) {
        val previous = load()?.goals.orEmpty()
        val history = if (goal == null) previous else previous.filterNot { it.date == date } + DatedGoal(date, goal)
        val goals = JSONArray()
        history.sortedBy { it.date }.forEach { dated ->
            val item = JSONObject()
                .put("date", dated.date.toString())
                .put("calories", dated.goal.calories)
                .put("protein", dated.goal.protein)
                .put("fat", dated.goal.fat)
                .put("carbohydrate", dated.goal.carbohydrate)
                .put("water", dated.goal.waterMl ?: JSONObject.NULL)
                .put("mode", dated.goal.macroMode.name)
            goals.put(item)
        }
        val root = JSONObject()
            .put("sex", profile.sex.name)
            .put("age", profile.age)
            .put("height", profile.heightCm)
            .put("weight", profile.weightKg)
            .put("activity", profile.activityFactor)
            .put("language", profile.language.name)
            .put("theme", profile.theme.name)
            .put("goals", goals)
        check(preferences.edit().putString("state", root.toString()).commit()) { "Could not save profile" }
    }
}
