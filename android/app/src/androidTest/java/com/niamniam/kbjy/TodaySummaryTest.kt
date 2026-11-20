package com.niamniam.kbjy

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.niamniam.kbjy.profile.DailyGoal
import org.junit.Rule
import org.junit.Test

class TodaySummaryTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun showsCalorieAndMacroOveragesInText() {
        compose.setContent {
            MaterialTheme {
                TodaySummary(
                    totals = DiaryItem(2220.0, 100.0, 75.0, 200.0, 0.0),
                    goal = DailyGoal(2100.0, 120.0, 70.0, 240.0, null),
                    english = false,
                )
            }
        }
        compose.onNodeWithText("+120").assertIsDisplayed()
        compose.onNodeWithText("ккал выше цели").assertIsDisplayed()
        compose.onNodeWithText("На 5,0 г выше цели").assertIsDisplayed()
    }
}
