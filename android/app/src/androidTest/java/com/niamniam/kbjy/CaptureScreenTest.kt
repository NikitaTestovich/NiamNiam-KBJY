package com.niamniam.kbjy

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.provider.MediaStore
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.niamniam.kbjy.scanner.CaptureScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CaptureScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun cameraAccessFailureCanBeRetriedOrFollowedByManualEntry() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        var attempts = 0
        var manualEntry = false
        val monitor = object : Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
                if (intent.action != MediaStore.ACTION_IMAGE_CAPTURE) return null
                if (++attempts == 1) throw SecurityException("Test camera access denied")
                return Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null)
            }
        }
        instrumentation.addMonitor(monitor)
        try {
            compose.setContent {
                CaptureScreen(
                    english = true,
                    onImageCaptured = { error("The cancelled camera should not return an image") },
                    onManualEntry = { manualEntry = true },
                )
            }
            compose.onNodeWithText("Open camera").performClick()
            val denied = "Could not access the camera. Enter the details manually."
            compose.onNodeWithText(denied).assertExists()
            compose.onNodeWithText("Open camera").performClick()
            val cancelled = "No photo was captured. Try again or enter the details manually."
            compose.waitUntil(5_000) { compose.onAllNodesWithText(cancelled).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText(denied).assertDoesNotExist()
            compose.onNodeWithText("Enter manually").performClick()
            compose.runOnIdle { assertTrue(manualEntry) }
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }
}
