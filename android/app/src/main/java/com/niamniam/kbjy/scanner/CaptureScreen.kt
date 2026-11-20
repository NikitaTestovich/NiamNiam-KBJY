package com.niamniam.kbjy.scanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Captures a camera preview image through the installed camera app.
 * The caller owns recognition and must handle its result.
 */
@Composable
fun CaptureScreen(
    onImageCaptured: (Bitmap) -> Unit,
    onManualEntry: () -> Unit,
    modifier: Modifier = Modifier,
    english: Boolean = false,
) {
    var cancelled by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val photo = File(context.cacheDir, "camera/capture.jpg")
    val uri = FileProvider.getUriForFile(context, context.packageName + ".files", photo)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val bitmap = if (success) BitmapFactory.decodeFile(photo.absolutePath) else null
        photo.delete()
        if (bitmap == null) {
            cancelled = true
        } else {
            cancelled = false
            onImageCaptured(bitmap)
        }
    }
    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            if (english) "Photograph the label" else "Сфотографировать этикетку",
            style = MaterialTheme.typography.headlineSmall,
        )
        if (cancelled) {
            Text(if (english) "No photo was captured." else "Снимок не получен.")
        }
        Button(onClick = {
            try {
                photo.parentFile?.mkdirs()
                launcher.launch(uri)
            } catch (_: android.content.ActivityNotFoundException) {
                cancelled = true
            }
        }) {
            Text(if (english) "Open camera" else "Открыть камеру")
        }
        Button(onClick = onManualEntry) {
            Text(if (english) "Enter manually" else "Ввести вручную")
        }
    }
}
