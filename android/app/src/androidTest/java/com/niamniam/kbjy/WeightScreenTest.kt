package com.niamniam.kbjy

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.niamniam.kbjy.profile.AppLanguage
import com.niamniam.kbjy.profile.WeightDao
import com.niamniam.kbjy.profile.WeightMeasurement
import com.niamniam.kbjy.profile.WeightScreen
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WeightScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun opensTheWeightFormOnlyWhenRequestedAndRecordsTheMeasurement() {
        val dao = TestWeightDao()
        compose.setContent { WeightScreen(dao, AppLanguage.RUSSIAN, onBack = {}) }

        compose.onNodeWithText("Дата ГГГГ-ММ-ДД").assertDoesNotExist()
        compose.onNodeWithText("Вес, кг").assertDoesNotExist()
        compose.onNodeWithText("Записать вес").performScrollTo().performClick()
        compose.onNodeWithText("Дата ГГГГ-ММ-ДД").assertIsDisplayed()
        compose.onNodeWithText("Вес, кг").performTextReplacement("72,56")
        compose.onNodeWithText("Сохранить").performClick()

        compose.waitUntil(5_000) { dao.measurements.value.size == 1 }
        compose.onNodeWithText("72,56 кг").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Вес, кг").assertDoesNotExist()
        assertEquals(LocalDate.now().toString(), dao.measurements.value.single().date)
    }

    @Test fun editsAndDeletesFromTheHistoryRowWithoutRoundingAnUnchangedWeight() {
        val date = LocalDate.now().toString()
        val initial = WeightMeasurement(1, date, 74.1234)
        val dao = TestWeightDao(initial)
        compose.setContent { WeightScreen(dao, AppLanguage.RUSSIAN, onBack = {}) }

        compose.onNodeWithText(date).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Изменить вес").performScrollTo().performClick()
        compose.onNodeWithText("74,12").assertIsDisplayed()
        compose.onNodeWithText("Сохранить").performClick()
        compose.waitUntil(5_000) { dao.updated.value != null }
        assertEquals(initial, dao.updated.value)

        compose.onNodeWithContentDescription("Удалить замер веса").performScrollTo().performClick()
        compose.onNodeWithText("Удалить").performClick()
        compose.waitUntil(5_000) { dao.measurements.value.isEmpty() }
        compose.onNodeWithText("74,12 кг").assertDoesNotExist()
    }

    private class TestWeightDao(vararg initial: WeightMeasurement) : WeightDao {
        val measurements = MutableStateFlow(initial.toList())
        val updated = MutableStateFlow<WeightMeasurement?>(null)

        override fun observeAll() = measurements

        override suspend fun insert(measurement: WeightMeasurement) {
            measurements.update { it + measurement.copy(id = 1) }
        }

        override suspend fun update(measurement: WeightMeasurement) {
            measurements.update { rows -> rows.map { if (it.id == measurement.id) measurement else it } }
            updated.value = measurement
        }

        override suspend fun delete(id: Long) {
            measurements.update { rows -> rows.filterNot { it.id == id } }
        }
    }
}
