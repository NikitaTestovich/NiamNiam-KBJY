package com.niamniam.kbjy

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.googlecode.tesseract.android.TessBaseAPI
import com.niamniam.kbjy.scanner.LabelBasis
import com.niamniam.kbjy.scanner.LabelRecognizer
import com.niamniam.kbjy.scanner.ParsedLabel
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LabelRecognizerTest {
    @Test
    fun bundledModelsReadClearRussianNutritionLabel() = verifyLabel(
        "nutrition-ru.png", ParsedLabel(LabelBasis.PER_100_GRAMS, 120.0, 5.0, 3.0, 20.0),
    )

    @Test
    fun bundledModelsReadClearEnglishNutritionLabel() = verifyLabel(
        "nutrition-en.png", ParsedLabel(LabelBasis.PER_100_GRAMS, 120.0, 5.0, 3.0, 20.0),
    )

    @Test
    fun bundledModelsReadRussianDrinkLabelWithDecimalCommas() = verifyLabel(
        "nutrition-drink-ru.png", ParsedLabel(LabelBasis.PER_100_MILLILITERS, 45.0, 0.5, 0.1, 10.2),
    )

    @Test
    fun bundledModelsReadThePer100GramsColumnOfANutritionTable() = verifyLabel(
        "nutrition-two-columns.png", ParsedLabel(LabelBasis.PER_100_GRAMS, 200.0, 10.0, 8.0, 22.0),
    )

    private fun verifyLabel(filename: String, expected: ParsedLabel) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val image = instrumentation.context.assets.open("ocr/$filename").use { stream ->
            checkNotNull(BitmapFactory.decodeStream(stream)) { "Could not decode $filename" }
        }
        try {
            val result = LabelRecognizer.recognize(context, image)
            println("$filename: $result")
            val message = if (result == expected) filename else {
                val diagnostic = listOf("eng+rus", "eng").joinToString("\n") { language ->
                    "$language: ${readDiagnosticText(context, image, language)}"
                }
                "$filename OCR text:\n$diagnostic"
            }
            assertEquals(message, expected, result)
        } finally {
            image.recycle()
        }
    }

    private fun readDiagnosticText(context: Context, image: Bitmap, language: String): String? {
        val api = TessBaseAPI()
        try {
            val data = File(context.filesDir, "tesseract")
            if (!api.init(data.absolutePath, language)) return "Could not initialize diagnostic OCR"
            api.setImage(image)
            return api.getUTF8Text()
        } finally {
            api.recycle()
        }
    }
}
