package com.niamniam.kbjy

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.niamniam.kbjy.profile.DailyGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TodaySummaryTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun showsCalorieAndMacroOveragesInText() {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    Box(Modifier.width(320.dp).testTag("compact-summary")) {
                        TodaySummary(
                            totals = DiaryItem(2220.0, 100.0, 75.0, 200.0, 0.0),
                            goal = DailyGoal(2100.0, 120.0, 70.0, 240.0, null),
                            english = false,
                        )
                    }
                }
            }
        }
        compose.onNodeWithText("+120").assertIsDisplayed()
        val center = compose.onNodeWithTag("compact-summary").fetchSemanticsNode().boundsInRoot.center.x
        val calories = compose.onNodeWithText("2220 / 2100 ккал").fetchSemanticsNode().boundsInRoot.center.x
        assertEquals(center, calories, 1.5f)
        compose.onNodeWithText("ккал выше цели").assertIsDisplayed()
        compose.onNodeWithText("БЖУ относительно дневной цели").assertDoesNotExist()
        val plate = compose.onNodeWithTag("macro-plate").fetchSemanticsNode().boundsInRoot
        val legend = compose.onNodeWithTag("macro-legend").fetchSemanticsNode().boundsInRoot
        assertTrue("Legend is to the right of the plate", legend.left > plate.right)
        assertEquals("Plate and legend are centered vertically", plate.center.y, legend.center.y, 1.5f)
        for (name in listOf("Белки", "Жиры", "Углеводы")) {
            compose.onNodeWithText(name).assertIsDisplayed()
        }
        for (amount in listOf("100 / 120 г", "75 / 70 г", "200 / 240 г")) {
            compose.onNodeWithText(amount).assertIsDisplayed()
        }
        compose.onNodeWithText("На 5 г выше цели").assertIsDisplayed()
    }
}
