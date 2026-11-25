package com.niamniam.kbjy.scanner

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import kotlin.math.roundToInt

object LabelRecognizer {
    private const val RUSSIAN_RETRY_SCALE = 0.45

    fun recognize(context: Context, bitmap: Bitmap): ParsedLabel {
        val root = File(context.filesDir, "tesseract")
        val directory = File(root, "tessdata")
        check(directory.mkdirs() || directory.isDirectory)
        for (language in listOf("rus", "eng")) {
            val target = File(directory, "$language.traineddata")
            if (!target.exists()) {
                val temporary = File(directory, "$language.tmp")
                context.assets.open("tessdata/$language.traineddata").use { input ->
                    temporary.outputStream().use { output -> input.copyTo(output) }
                }
                check(temporary.renameTo(target))
            }
        }
        val primary = checkNotNull(readText(root, bitmap, "eng+rus")) { "Could not initialize label OCR" }
        return parseLabelWithRetry(
            primary,
            readEnglish = { readText(root, bitmap, "eng").orEmpty() },
            readSmallerRussian = { readSmallerImage(root, bitmap) },
        )
    }

    private fun readSmallerImage(root: File, bitmap: Bitmap): String {
        val width = (bitmap.width * RUSSIAN_RETRY_SCALE).roundToInt()
        val height = (bitmap.height * RUSSIAN_RETRY_SCALE).roundToInt()
        if (width < 1 || height < 1) return ""
        val smaller = Bitmap.createScaledBitmap(bitmap, width, height, true)
        return try {
            readText(root, smaller, "eng+rus").orEmpty()
        } finally {
            if (smaller !== bitmap) smaller.recycle()
        }
    }

    private fun readText(root: File, bitmap: Bitmap, language: String): String? {
        val api = TessBaseAPI()
        try {
            if (!api.init(root.absolutePath, language)) return null
            api.setImage(bitmap)
            return api.getUTF8Text().orEmpty()
        } finally {
            api.recycle()
        }
    }
}
