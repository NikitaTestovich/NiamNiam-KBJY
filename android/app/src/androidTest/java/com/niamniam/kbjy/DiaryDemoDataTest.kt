package com.niamniam.kbjy

import androidx.room.withTransaction
import androidx.test.platform.app.InstrumentationRegistry
import com.niamniam.kbjy.data.DiaryDatabase
import com.niamniam.kbjy.data.DiaryEntry
import com.niamniam.kbjy.profile.AppLanguage
import com.niamniam.kbjy.profile.AppTheme
import com.niamniam.kbjy.profile.DailyGoal
import com.niamniam.kbjy.profile.Profile
import com.niamniam.kbjy.profile.ProfileStore
import com.niamniam.kbjy.profile.Sex
import com.niamniam.kbjy.profile.WeightDatabase
import com.niamniam.kbjy.profile.WeightMeasurement
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

private val firstDemoDay = LocalDate.of(2026, 9, 23)
private val lastDemoDay = LocalDate.of(2026, 11, 22)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class DemoDataCommand

private data class FoodPortion(
    val name: String,
    val amount: Double,
    val kcal: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val drink: Boolean = false,
)

private val breakfasts = listOf(
    FoodPortion("Овсянка с бананом и орехами", 320.0, 435.0, 13.0, 15.0, 62.0),
    FoodPortion("Омлет с тостами и помидорами", 280.0, 420.0, 23.0, 22.0, 32.0),
    FoodPortion("Сырники со сметаной", 245.0, 480.0, 25.0, 22.0, 46.0),
    FoodPortion("Гречневая каша с молоком и яблоком", 340.0, 409.0, 16.0, 9.0, 66.0),
    FoodPortion("Творог с ягодами и гранолой", 285.0, 453.0, 31.0, 17.0, 44.0),
)
private val lunches = listOf(
    FoodPortion("Запечённая курица с рисом и овощами", 460.0, 650.0, 47.0, 22.0, 66.0),
    FoodPortion("Борщ с говядиной, сметаной и хлебом", 515.0, 620.0, 28.0, 27.0, 66.0),
    FoodPortion("Паста с индейкой и томатами", 420.0, 680.0, 37.0, 22.0, 84.0),
    FoodPortion("Гречка с котлетой и овощным салатом", 450.0, 640.0, 36.0, 25.0, 68.0),
    FoodPortion("Лобио с лавашом и зеленью", 410.0, 625.0, 26.0, 21.0, 83.0),
    FoodPortion("Картофельное пюре с рыбой и салатом", 435.0, 610.0, 35.0, 24.0, 63.0),
)
private val dinners = listOf(
    FoodPortion("Лосось с картофелем и брокколи", 390.0, 580.0, 37.0, 28.0, 45.0),
    FoodPortion("Индейка с булгуром и овощами", 405.0, 565.0, 44.0, 17.0, 59.0),
    FoodPortion("Голубцы с рисом и сметаной", 400.0, 570.0, 29.0, 26.0, 55.0),
    FoodPortion("Запечённые овощи с нутом и фетой", 380.0, 553.0, 22.0, 25.0, 60.0),
    FoodPortion("Вареники с картофелем и луком", 365.0, 555.0, 15.0, 15.0, 90.0),
)
private val snacks = listOf(
    FoodPortion("Йогурт с яблоком", 280.0, 205.0, 12.0, 5.0, 28.0),
    FoodPortion("Банан и миндаль", 150.0, 240.0, 6.0, 12.0, 27.0),
    FoodPortion("Тост с сыром", 105.0, 265.0, 14.0, 13.0, 23.0),
    FoodPortion("Кефир с ягодами", 250.0, 180.0, 9.0, 6.0, 23.0, drink = true),
    FoodPortion("Груша и творог", 220.0, 208.0, 17.0, 6.0, 22.0),
)

private fun demoEntries(): List<DiaryEntry> = List(61) { firstDemoDay.plusDays(it.toLong()) }
    .flatMapIndexed { day, date ->
        val meals = listOf(
            "breakfast" to breakfasts[(day + day / 7) % breakfasts.size],
            "lunch" to lunches[(day * 5 + day / 7) % lunches.size],
            "snack" to snacks[(day * 3 + day / 6) % snacks.size],
            "dinner" to dinners[(day * 2 + day / 7) % dinners.size],
        )
        meals.mapIndexed { slot, (meal, food) ->
            val scale = 0.9 + (day * (slot + 1) % 7) * 0.05
            DiaryEntry(
                // Stable negative IDs are separate from Room's positive generated IDs.
                id = -((day * 4 + slot + 1).toLong()),
                date = date.toString(),
                meal = meal,
                name = food.name,
                amount = food.amount * scale,
                unit = if (food.drink) "мл" else "г",
                calories = food.kcal * scale,
                protein = food.protein * scale,
                fat = food.fat * scale,
                carbohydrate = food.carbs * scale,
                drinkMl = if (food.drink) food.amount * scale else 0.0,
            )
        }
    }

private fun demoDrinks(): List<DiaryEntry> = List(61) { day ->
    val date = firstDemoDay.plusDays(day.toLong()).toString()
    listOf("breakfast" to (250 + day % 4 * 50), "lunch" to (550 + day % 5 * 75),
        "dinner" to (500 + day % 6 * 60)).mapIndexed { slot, (meal, volume) ->
        DiaryEntry(id = -1000L - day * 3 - slot, date = date, meal = meal, name = "Вода",
            amount = volume.toDouble(), unit = "мл", calories = 0.0, protein = 0.0,
            fat = 0.0, carbohydrate = 0.0, drinkMl = volume.toDouble())
    }
}.flatten()

private fun demoWeights(): List<WeightMeasurement> = List(21) { index ->
    val fluctuations = listOf(0.0, 0.6, -0.2, 0.8, -0.4, 0.3, -0.6)
    WeightMeasurement(-2000L - index, firstDemoDay.plusDays(index * 3L).toString(),
        (7300 - index * 7 + (fluctuations[index % fluctuations.size] * 100).toInt()) / 100.0)
}

class DiaryDemoDataTest {
    @Test
    fun fixtureIncludesDailyDrinksAndWeightChangesInBothDirections() {
        val drinks = demoDrinks().groupBy { it.date }
        assertEquals(61, drinks.size)
        assertTrue(drinks.values.all { rows -> rows.size == 3 && rows.sumOf { it.drinkMl } in 1300.0..2050.0 })
        val changes = demoWeights().zipWithNext { a, b -> b.kilograms - a.kilograms }
        assertTrue(changes.any { it > 0 } && changes.any { it < 0 })
    }
    @Test
    fun fixtureCoversEveryDayWithFourDistinctMealsAndVariedPortions() {
        val fixture = demoEntries()
        assertEquals(244, fixture.size)
        assertEquals(244, fixture.map { it.id }.distinct().size)
        assertTrue(fixture.all { it.id < 0 && it.amount > 0 && it.calories > 0 })
        val byDay = fixture.groupBy { it.date }
        assertEquals(61, byDay.size)
        assertEquals(firstDemoDay.toString(), byDay.keys.min())
        assertEquals(lastDemoDay.toString(), byDay.keys.max())
        for ((_, meals) in byDay) {
            assertEquals(setOf("breakfast", "lunch", "dinner", "snack"), meals.map { it.meal }.toSet())
            assertTrue(meals.sumOf { it.calories } in 1450.0..2600.0)
        }
        assertTrue(fixture.map { it.name }.distinct().size >= 20)
        assertTrue(fixture.groupBy { it.name }.values.any { rows -> rows.map { it.amount }.distinct().size > 1 })
    }

    @Test
    @DemoDataCommand
    fun seedOnRequestedTestInstallation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assumeTrue("Explicit demo data request required", InstrumentationRegistry.getArguments().getString("seedDemoDiary") == "true")
        val context = instrumentation.targetContext
        check(context.packageName == "com.niamniam.kbjy.devicetest")
        runBlocking {
            val database = DiaryDatabase.get(context)
            database.withTransaction {
                val dao = database.diaryDao()
                val first = firstDemoDay.toString()
                val last = lastDemoDay.toString()
                val existing = dao.entriesBetween(first, last).associateBy { it.id }
                val fixture = demoEntries() + demoDrinks()
                fixture.forEach { row ->
                    val previous = existing[row.id]
                    if (previous == null) dao.insert(row)
                    else check(previous == row) { "Demo row has been edited: ${row.id}" }
                }
                val stored = dao.entriesBetween(first, last).associateBy { it.id }
                assertTrue(fixture.all { it == stored[it.id] })
            }
            val weights = WeightDatabase.get(context)
            weights.withTransaction {
                val dao = weights.weightDao()
                val existing = dao.observeAll().first().associateBy { it.id }
                val fixture = demoWeights()
                fixture.forEach { row ->
                    val previous = existing[row.id]
                    if (previous == null) dao.insert(row)
                    else check(previous == row) { "Demo measurement has been edited: ${row.id}" }
                }
                val stored = dao.observeAll().first().associateBy { it.id }
                assertTrue(fixture.all { it == stored[it.id] })
            }
        }
        val profileStore = ProfileStore(context)
        if (profileStore.load() == null) {
            profileStore.save(
                Profile(Sex.FEMALE, 33, 174.0, 73.0, 1.375, AppLanguage.RUSSIAN, AppTheme.LIGHT),
                DailyGoal(1950.0, 110.0, 67.0, 227.0, 1800.0),
                firstDemoDay,
            )
        }
        checkNotNull(profileStore.load())
        println("Seed verified: 244 demo meals, 183 drinks and 21 weight measurements")
    }
}
