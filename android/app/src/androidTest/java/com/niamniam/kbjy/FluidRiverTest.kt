package com.niamniam.kbjy

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class FluidRiverTest {
    @get:Rule val compose = createComposeRule()

    @Test fun riverUsesDefaultAndCustomTargetsAndKeepsActualTotalVisible() {
        val consumed = mutableStateOf(500.0)
        val target = mutableStateOf<Double?>(null)
        compose.setContent { FluidRiver(consumed.value, target.value, english = false) }
        compose.onNodeWithText("Жидкость: 500 / 2000 мл").assertExists()
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo,
            ProgressBarRangeInfo(0.25f, 0f..1f))).assertExists()
        compose.runOnIdle { target.value = 800.0; consumed.value = 1000.0 }
        compose.onNodeWithText("Жидкость: 1000 / 800 мл").assertExists()
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo,
            ProgressBarRangeInfo(1f, 0f..1f))).assertExists()
    }
}
