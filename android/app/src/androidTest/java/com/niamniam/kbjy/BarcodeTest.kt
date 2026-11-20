package com.niamniam.kbjy

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import org.junit.Assert.assertEquals
import org.junit.Test

class BarcodeTest {
    @Test
    fun bundledScannerReadsQrAndEan() {
        val scanner = BarcodeScanning.getClient()
        try {
            for ((format, value) in listOf(
                BarcodeFormat.QR_CODE to "NIAMNIAM-TEST",
                BarcodeFormat.EAN_13 to "4006381333931",
            )) {
                val matrix = MultiFormatWriter().encode(value, format, 800, 800)
                val pixels = IntArray(800 * 800) { index ->
                    if (matrix[index % 800, index / 800]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                }
                val bitmap = Bitmap.createBitmap(pixels, 800, 800, Bitmap.Config.ARGB_8888)
                val result = Tasks.await(scanner.process(InputImage.fromBitmap(bitmap, 0)))
                assertEquals(value, result.single().rawValue)
            }
        } finally {
            scanner.close()
        }
    }
}
