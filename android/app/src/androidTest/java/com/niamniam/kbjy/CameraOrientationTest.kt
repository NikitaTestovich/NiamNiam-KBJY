package com.niamniam.kbjy

import android.graphics.Bitmap
import android.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import com.niamniam.kbjy.scanner.decodePhoto
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class CameraOrientationTest {
    @Test
    fun rotatesPortraitPhotoAccordingToExif() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val file = File(context.cacheDir, "camera-orientation-test.jpg")
        try {
            file.outputStream().use {
                Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 90, it)
            }
            ExifInterface(file.absolutePath).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
                saveAttributes()
            }
            val result = requireNotNull(decodePhoto(file))
            assertEquals(80, result.width)
            assertEquals(120, result.height)
        } finally {
            file.delete()
        }
    }
}
