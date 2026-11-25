package com.niamniam.kbjy

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.core.content.IntentCompat
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.niamniam.kbjy.data.DiaryDatabase
import com.niamniam.kbjy.data.DiaryEntry
import com.niamniam.kbjy.profile.AppLanguage
import com.niamniam.kbjy.reports.ReportScreen
import java.io.File
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.ZipInputStream
import java.util.zip.ZipFile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReportScreenRestorationTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun failedExportCanBeRetriedForSameMonth() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val directory = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File.createTempFile("report-test-", ".xlsx", directory)
        val goodUri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val attempts = AtomicInteger()
        val monitor = object : Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
                if (intent.action != Intent.ACTION_CREATE_DOCUMENT) return null
                val uri = if (attempts.getAndIncrement() == 0) Uri.parse("content://invalid.test/report.xlsx") else goodUri
                return Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(uri))
            }
        }
        val database = Room.inMemoryDatabaseBuilder(context, DiaryDatabase::class.java).build()
        val dao = database.diaryDao()
        val firstOfMonth = LocalDate.now().minusMonths(1).withDayOfMonth(1)
        val nextMonth = firstOfMonth.plusMonths(1)
        val expectedPeriod = "$firstOfMonth — ${nextMonth.minusDays(1)}"
        val included = DiaryEntry(date = firstOfMonth.toString(), meal = "lunch", name = "Запись выбранного месяца",
            amount = 100.0, unit = "г", calories = 75.0, protein = 2.0, fat = 1.0, carbohydrate = 14.0)
        val excluded = included.copy(date = nextMonth.toString(), name = "Запись текущего месяца")
        instrumentation.addMonitor(monitor)
        try {
            runBlocking {
                dao.insert(included)
                dao.insert(excluded)
            }
            compose.setContent {
                var showReport by remember { mutableStateOf(true) }
                if (showReport) ReportScreen(dao, AppLanguage.RUSSIAN, onBack = { showReport = false })
                else Text("Отчёт закрыт")
            }
            compose.onNodeWithText("Месяц").performClick()
            compose.onNodeWithText("Предыдущий период").performClick()
            compose.onNodeWithText(expectedPeriod).assertExists()
            fun waitForText(text: String) = compose.waitUntil(10_000) {
                compose.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
            }
            waitForText("Среднее за день")
            compose.onNodeWithText("Сохранить XLSX").performScrollTo().performClick()
            waitForText("Не удалось сохранить XLSX")
            compose.onNodeWithText("Месяц").assertIsNotEnabled()
            compose.onNodeWithText("Повторить сохранение").performScrollTo().performClick()
            waitForText("Отчёт сохранён")
            compose.onNodeWithText("Месяц").assertIsNotEnabled()
            compose.onNodeWithText(expectedPeriod).assertExists()
            assertEquals(2, attempts.get())
            ZipFile(file).use { zip ->
                val diary = zip.getInputStream(zip.getEntry("xl/worksheets/sheet2.xml")).bufferedReader().use { it.readText() }
                assertTrue(diary.contains(included.name))
                assertFalse(diary.contains(excluded.name))
            }
            compose.onNodeWithText("Назад").performScrollTo().performClick()
            compose.onNodeWithText("Отчёт закрыт").assertExists()
        } finally {
            instrumentation.removeMonitor(monitor)
            database.close()
            file.delete()
        }
    }

    @Test
    fun sharesOnlyTheSelectedMonthAsAReadableXlsxAttachment() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val chooser = AtomicReference<Intent>()
        val monitor = object : Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
                if (intent.action != Intent.ACTION_CHOOSER) return null
                chooser.set(intent)
                return Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null)
            }
        }
        val database = Room.inMemoryDatabaseBuilder(context, DiaryDatabase::class.java).build()
        val dao = database.diaryDao()
        val first = LocalDate.now().minusMonths(1).withDayOfMonth(1)
        val nextMonth = first.plusMonths(1)
        val included = DiaryEntry(date = first.toString(), meal = "lunch", name = "Передать запись месяца",
            amount = 120.0, unit = "г", calories = 75.0, protein = 2.0, fat = 1.0, carbohydrate = 14.0)
        instrumentation.addMonitor(monitor)
        try {
            runBlocking {
                dao.insert(included)
                dao.insert(included.copy(date = nextMonth.toString(), name = "Запись вне периода"))
            }
            compose.setContent { ReportScreen(dao, AppLanguage.RUSSIAN, onBack = {}) }
            compose.onNodeWithText("Месяц").performClick()
            compose.onNodeWithText("Предыдущий период").performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodes(hasText("Среднее за день")).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Поделиться XLSX").performScrollTo().performClick()
            compose.waitUntil(10_000) { chooser.get() != null }
            val send = checkNotNull(IntentCompat.getParcelableExtra(chooser.get(), Intent.EXTRA_INTENT, Intent::class.java))
            assertEquals(Intent.ACTION_SEND, send.action)
            assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", send.type)
            assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
            assertFalse(send.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION != 0)
            val uri = checkNotNull(IntentCompat.getParcelableExtra(send, Intent.EXTRA_STREAM, Uri::class.java))
            assertEquals("content", uri.scheme)
            assertEquals(uri, send.clipData?.getItemAt(0)?.uri)
            assertEquals("NiamNiam-$first--${nextMonth.minusDays(1)}.xlsx", uri.lastPathSegment)
            ZipInputStream(checkNotNull(context.contentResolver.openInputStream(uri))).use { zip ->
                val sheet = generateSequence { zip.nextEntry }.first { it.name == "xl/worksheets/sheet2.xml" }
                assertEquals("xl/worksheets/sheet2.xml", sheet.name)
                val diary = zip.bufferedReader().readText()
                assertTrue(diary.contains(included.name))
                assertFalse(diary.contains("Запись вне периода"))
            }
        } finally {
            instrumentation.removeMonitor(monitor)
            database.close()
        }
    }

    @Test
    fun selectedReportPeriodSurvivesActivityRecreation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, DiaryDatabase::class.java).build()
        try {
            val restoration = StateRestorationTester(compose)
            restoration.setContent { ReportScreen(database.diaryDao(), AppLanguage.RUSSIAN, onBack = {}) }
            compose.onNodeWithText("Месяц").performClick()
            compose.onNodeWithText("Месяц").assertIsNotEnabled()
            restoration.emulateSavedInstanceStateRestore()
            compose.onNodeWithText("Месяц").assertIsNotEnabled()
        } finally {
            database.close()
        }
    }
}
