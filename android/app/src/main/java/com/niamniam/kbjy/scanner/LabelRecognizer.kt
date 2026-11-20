package com.niamniam.kbjy.scanner

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File

object LabelRecognizer {
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
        val api = TessBaseAPI()
        try {
            check(api.init(root.absolutePath, "eng+rus"))
            api.setImage(bitmap)
            return LabelParser.parse(api.getUTF8Text().orEmpty())
        } finally {
            api.recycle()
        }
    }
}
