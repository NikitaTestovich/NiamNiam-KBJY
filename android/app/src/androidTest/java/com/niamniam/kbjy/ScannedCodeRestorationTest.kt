package com.niamniam.kbjy

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.niamniam.kbjy.data.DiaryDatabase
import com.niamniam.kbjy.data.Food
import com.niamniam.kbjy.data.FoodBarcode
import com.niamniam.kbjy.profile.*
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ScannedCodeRestorationTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test
    fun catalogProductReopensSavedCardForSameBarcode() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("profile", 0)
        val original = prefs.getString("state", null)
        val database = DiaryDatabase.get(context)
        val dao = database.diaryDao()
        val code = "2100100337919"
        val name = "Сохранённый товар для проверки каталога"
        val id = runBlocking {
            check(dao.findByBarcode(code) == null)
            dao.insertFood(Food(name = name, unit = "г", caloriesPer100 = 70.0, proteinPer100 = 4.0,
                fatPer100 = 3.0, carbohydratePer100 = 5.0, defaultPortion = 250.0)).also {
                dao.insertBarcode(FoodBarcode(code, it))
            }
        }
        try {
            ProfileStore(context).save(Profile(Sex.MALE, 30, 175.0, 75.0, 1.2, AppLanguage.RUSSIAN, AppTheme.LIGHT), null)
            ActivityScenario.launch(MainActivity::class.java).use {
                compose.onNodeWithText("Добавить на обед").performScrollTo().performClick()
                compose.onNodeWithText("Поиск").performTextInput("Борщ с говядиной Вкусвил")
                val catalogTitle = "Борщ с говядиной · Вкусвил"
                compose.waitUntil(10_000) { compose.onAllNodesWithText(catalogTitle).fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText(catalogTitle).performClick()
                compose.waitUntil(10_000) { compose.onAllNodesWithText("250").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText(name).assertExists()
            }
        } finally {
            prefs.edit().putString("state", original).commit()
            val sql = database.openHelper.writableDatabase
            sql.execSQL("DELETE FROM food_barcodes WHERE code = ? AND foodId = ?", arrayOf<Any>(code, id))
            sql.execSQL("DELETE FROM foods WHERE id = ?", arrayOf(id))
        }
    }

    @Test
    fun scannedUpcOpensThePreparedUsdaProduct() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val prefs = context.getSharedPreferences("profile", 0)
        val original = prefs.getString("state", null)
        val title = "Вареники с картофелем и луком (готовые) · Delicious Fresh Pierogi"
        val monitor = barcodeCaptureMonitor(instrumentation, "797589001046", BarcodeFormat.UPC_A)
        instrumentation.addMonitor(monitor)
        try {
            ProfileStore(context).save(
                Profile(Sex.MALE, 30, 175.0, 75.0, 1.2, AppLanguage.RUSSIAN, AppTheme.LIGHT),
                DailyGoal(2000.0, 100.0, 70.0, 200.0, null),
            )
            ActivityScenario.launch(MainActivity::class.java).use {
                compose.onNodeWithText("Добавить на обед").performScrollTo().performClick()
                compose.onNodeWithText("Поиск").performTextInput("вареники с картофелем")
                compose.waitUntil(10_000) {
                    compose.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
                }
                compose.onNodeWithText("Сканировать").performClick()
                compose.onNodeWithText("Сканировать штрихкод или QR-код").assertExists()
                compose.onNodeWithText("Открыть камеру").performClick()
                compose.waitUntil(20_000) { compose.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText(title).assertExists()
            }
        } finally {
            instrumentation.removeMonitor(monitor)
            prefs.edit().putString("state", original).commit()
        }
    }

    @Test
    fun unknownQrReopensTheProductSavedFromACorrectedLabel() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val prefs = context.getSharedPreferences("profile", 0)
        val original = prefs.getString("state", null)
        val code = "NIAMNIAM-RESTORE-${System.nanoTime()}"
        val name = "Проверка QR и этикетки ${System.nanoTime()}"
        val dao = DiaryDatabase.get(context).diaryDao()
        ProfileStore(context).save(
            Profile(Sex.MALE, 30, 175.0, 75.0, 1.2, AppLanguage.RUSSIAN, AppTheme.LIGHT),
            DailyGoal(2000.0, 100.0, 70.0, 200.0, null),
        )
        val monitor = barcodeCaptureMonitor(instrumentation, code, BarcodeFormat.QR_CODE, labelFixture = "nutrition-ru.png")
        instrumentation.addMonitor(monitor)
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                compose.onNodeWithText("Добавить на обед").performScrollTo().performClick()
                compose.onNodeWithText("Сканировать").performClick()
                compose.onNodeWithText("Открыть камеру").performClick()
                compose.waitUntil(20_000) {
                    compose.onAllNodes(hasText("Название")).fetchSemanticsNodes().isNotEmpty()
                }
                scenario.recreate()
                compose.onNodeWithText("Название").performTextInput(name)
                compose.onNodeWithText("Порция, г").performScrollTo().performTextInput("50")
                compose.onNodeWithText("Считать этикетку").performClick()
                compose.onNodeWithText("Открыть камеру").performClick()
                compose.waitUntil(20_000) {
                    compose.onAllNodesWithText("Проверьте все распознанные значения по этикетке.")
                        .fetchSemanticsNodes().isNotEmpty()
                }
                compose.onNodeWithText(name).assertExists()
                compose.onNodeWithText("Жиры, г").performScrollTo().performTextReplacement("3,5")
                compose.onNodeWithText("Сохранить").performScrollTo().performClick()
                compose.waitUntil(10_000) { runBlocking { dao.findByBarcode(code) } != null }
                val saved = checkNotNull(runBlocking { dao.findByBarcode(code) })
                assertEquals(name, saved.name)
                assertEquals(120.0, saved.caloriesPer100, 0.0)
                assertEquals(5.0, saved.proteinPer100, 0.0)
                assertEquals(3.5, saved.fatPer100, 0.0)
                assertEquals(20.0, saved.carbohydratePer100, 0.0)
                assertEquals(50.0, saved.defaultPortion)
                val today = LocalDate.now().toString()
                val lunch = runBlocking { dao.entriesBetween(today, today) }.single { it.name == name }
                assertEquals("lunch", lunch.meal)
                assertEquals(60.0, lunch.calories, 0.0)
                assertEquals(1.75, lunch.fat, 0.0)

                compose.waitUntil(10_000) { compose.onAllNodesWithText("Добавить на обед").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText("Добавить на обед").performScrollTo().performClick()
                compose.onNodeWithText("Сканировать").performClick()
                compose.onNodeWithText("Открыть камеру").performClick()
                compose.waitUntil(20_000) {
                    compose.onAllNodesWithText("Размер одной порции, г").fetchSemanticsNodes().isNotEmpty()
                }
                val inPortionDialog = hasAnyAncestor(isDialog())
                compose.onNode(hasText(name) and inPortionDialog).assertExists()
                compose.onNode(hasText("50") and inPortionDialog).assertExists()
                compose.onNode(hasText("Отмена") and inPortionDialog).performClick()
                compose.onNodeWithText("Поиск").performTextInput("Проверка QR и этикетки")
                compose.waitUntil(10_000) { compose.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText(name).assertExists()
            }
        } finally {
            instrumentation.removeMonitor(monitor)
            prefs.edit().apply { if (original == null) remove("state") else putString("state", original) }.commit()
            val sql = DiaryDatabase.get(context).openHelper.writableDatabase
            sql.execSQL("DELETE FROM food_barcodes WHERE code = ?", arrayOf(code))
            sql.execSQL("DELETE FROM foods WHERE name = ?", arrayOf(name))
            sql.execSQL("DELETE FROM diary_entries WHERE name = ?", arrayOf(name))
        }
    }

    private fun barcodeCaptureMonitor(
        instrumentation: Instrumentation,
        code: String,
        format: BarcodeFormat,
        labelFixture: String? = null,
    ) = object : Instrumentation.ActivityMonitor() {
        private var captures = 0

        override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
            if (intent.action != MediaStore.ACTION_IMAGE_CAPTURE) return null
            @Suppress("DEPRECATION")
            val output = intent.getParcelableExtra<Uri>(MediaStore.EXTRA_OUTPUT)!!
            captures++
            if (labelFixture != null && captures == 2) {
                instrumentation.context.assets.open("ocr/$labelFixture").use { label ->
                    checkNotNull(instrumentation.targetContext.contentResolver.openOutputStream(output)).use { label.copyTo(it) }
                }
                return Instrumentation.ActivityResult(Activity.RESULT_OK, null)
            }
            val matrix = MultiFormatWriter().encode(code, format, 800, 800)
            val pixels = IntArray(matrix.width * matrix.height) { index ->
                if (matrix[index % matrix.width, index / matrix.width]) android.graphics.Color.BLACK
                else android.graphics.Color.WHITE
            }
            val bitmap = Bitmap.createBitmap(pixels, matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
            try {
                instrumentation.targetContext.contentResolver.openOutputStream(output)!!.use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            } finally {
                bitmap.recycle()
            }
            return Instrumentation.ActivityResult(Activity.RESULT_OK, null)
        }
    }
}
