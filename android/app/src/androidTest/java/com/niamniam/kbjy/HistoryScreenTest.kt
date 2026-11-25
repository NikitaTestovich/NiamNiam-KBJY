package com.niamniam.kbjy

import android.util.Log
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.niamniam.kbjy.data.DiaryEntry
import com.niamniam.kbjy.diary.HistoryScreen
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HistoryScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun dayNavigationMovesOneDayInEachDirection() {
        val day = LocalDate.of(2026, 11, 22)
        val visited = mutableListOf<LocalDate>()
        compose.setContent {
            HistoryScreen(day, emptyList(), { visited.add(it) }, onDelete = {}, onAdd = {}, onUpdate = {})
        }
        compose.onNodeWithText("← День назад").performClick()
        compose.onNodeWithText("День вперёд →").performClick()
        compose.runOnIdle { assertEquals(listOf(day.minusDays(1), day.plusDays(1)), visited) }
    }

    @Test
    fun editorRoundsDisplayedProteinWithoutChangingUneditedNutrition() {
        val updated = AtomicReference<DiaryEntry>()
        val entry = DiaryEntry(id = 2, date = "2026-11-20", meal = "lunch", name = "Йогурт",
            amount = 415.0, unit = "г", calories = 292.16, protein = 12.035, fat = 8.3,
            carbohydrate = 42.33, drinkMl = 0.0)
        compose.setContent {
            HistoryScreen(LocalDate.parse(entry.date), listOf(entry), {}, {}, {}, { updated.set(it) })
        }
        compose.onNodeWithTag("history-list").performScrollToIndex(1)
        compose.onNodeWithText("Изменить").performScrollTo().performClick()
        compose.onNodeWithText("2,9").assertExists()
        compose.onNodeWithText("Название").performTextReplacement("Йогурт вишнёвый")
        compose.onNodeWithText("Сохранить").performClick()
        compose.waitUntil(10_000) { updated.get() != null }
        assertEquals(entry.protein, checkNotNull(updated.get()).protein, 0.0)
    }

    @Test
    fun waterEditorChangesVolumeAndPreservesLegacyWeightRatio() {
        val updated = AtomicReference<DiaryEntry>()
        val entry = DiaryEntry(id = 3, date = "2026-11-20", meal = "breakfast", name = "Вода",
            amount = 125.0, unit = "г", calories = 0.0, protein = 0.0,
            fat = 0.0, carbohydrate = 0.0, drinkMl = 250.123)
        compose.setContent {
            HistoryScreen(LocalDate.parse(entry.date), listOf(entry), {}, {}, {}, { updated.set(it) })
        }
        compose.onNodeWithTag("history-list").performScrollToIndex(2)
        compose.onNodeWithText("Изменить").performScrollTo().performClick()
        compose.onNodeWithText("Объём воды, мл").assertExists()
        compose.onNodeWithText("Белки, г").assertDoesNotExist()
        compose.onNodeWithText("250,12").assertExists()
        compose.onNodeWithText("Сохранить").performClick()
        compose.waitUntil(10_000) { updated.get() != null }
        assertEquals(entry, updated.getAndSet(null))

        compose.onNodeWithText("Изменить").performScrollTo().performClick()
        compose.onNodeWithText("Объём воды, мл").performTextReplacement("0")
        compose.onNodeWithText("Сохранить").assertIsNotEnabled()
        compose.onNodeWithText("Объём воды, мл").performTextReplacement("500,246")
        compose.onNodeWithText("Сохранить").performClick()
        compose.waitUntil(10_000) { updated.get() != null }
        assertEquals(entry.copy(amount = 250.0, drinkMl = 500.246), updated.get())
    }

    @Test
    fun changingDrinkPortionRecalculatesNutritionAndVolume() {
        val updated = AtomicReference<DiaryEntry>()
        val entry = DiaryEntry(id = 1, date = "2026-11-20", meal = "lunch", name = "Сок",
            amount = 100.0, unit = "мл", calories = 40.0, protein = 0.0,
            fat = 0.0, carbohydrate = 10.0, drinkMl = 100.0)
        Log.i("HistoryScreenTest", "Mounting history screen")
        compose.setContent {
            HistoryScreen(
                date = LocalDate.parse(entry.date), entries = listOf(entry),
                onDateSelected = {}, onDelete = {}, onAdd = {}, onUpdate = { updated.set(it) },
            )
        }
        Log.i("HistoryScreenTest", "Opening edit dialog")
        compose.onNodeWithText("Калории").assertExists()
        compose.onNodeWithText("Жидкость").assertExists()
        compose.onNodeWithText("40,00 ккал").assertExists()
        compose.onNodeWithTag("history-list").performScrollToIndex(1)
        compose.onNodeWithText("Обед · 100,00 мл").assertExists()
        compose.onNodeWithText("Изменить").performScrollTo().performClick()
        Log.i("HistoryScreenTest", "Replacing portion")
        compose.onNodeWithText("Количество (мл)").performTextReplacement("250")
        Log.i("HistoryScreenTest", "Saving edited entry")
        compose.onNodeWithText("Сохранить").performClick()
        compose.waitUntil(10_000) { updated.get() != null }
        val saved = checkNotNull(updated.get())
        assertEquals(100.0, saved.calories, 0.001)
        assertEquals(250.0, saved.drinkMl, 0.001)
        assertEquals(25.0, saved.carbohydrate, 0.001)
    }
}
