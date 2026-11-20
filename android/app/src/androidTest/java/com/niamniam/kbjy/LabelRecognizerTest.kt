package com.niamniam.kbjy

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.niamniam.kbjy.scanner.LabelBasis
import com.niamniam.kbjy.scanner.LabelRecognizer
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LabelRecognizerTest {
    @Test
    fun bundledModelsReadClearRussianNutritionLabel() {
        val image = Bitmap.createBitmap(1400, 500, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image)
        canvas.drawColor(Color.WHITE)
        val paint = Paint().apply { color = Color.BLACK; textSize = 64f; isAntiAlias = true }
        listOf("на 100 г", "Энергия 120 ккал", "Белки 5 г", "Жиры 3 г", "Углеводы 20 г")
            .forEachIndexed { index, line -> canvas.drawText(line, 30f, 80f + index * 80f, paint) }
        val result = LabelRecognizer.recognize(ApplicationProvider.getApplicationContext(), image)
        assertEquals(LabelBasis.PER_100_GRAMS, result.basis)
        assertEquals(120.0, result.calories!!, 0.0)
        assertEquals(5.0, result.protein!!, 0.0)
    }

    @Test
    fun bundledModelsReadClearEnglishNutritionLabel() {
        val image = Bitmap.createBitmap(1400, 500, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image)
        canvas.drawColor(Color.WHITE)
        val paint = Paint().apply { color = Color.BLACK; textSize = 64f; isAntiAlias = true }
        listOf("per 100 g", "Energy 120 kcal", "Protein 5 g", "Fat 3 g", "Carbohydrates 20 g")
            .forEachIndexed { index, line -> canvas.drawText(line, 30f, 80f + index * 80f, paint) }
        val result = LabelRecognizer.recognize(ApplicationProvider.getApplicationContext(), image)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val api = com.googlecode.tesseract.android.TessBaseAPI()
        api.init(java.io.File(context.filesDir, "tesseract").absolutePath, "eng+rus")
        api.setImage(image)
        val recognizedText = api.getUTF8Text()
        api.recycle()
        assertEquals("OCR text: $recognizedText", LabelBasis.PER_100_GRAMS, result.basis)
        assertEquals(120.0, result.calories!!, 0.0)
        assertEquals(5.0, result.protein!!, 0.0)
    }
}
