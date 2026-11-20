package com.niamniam.kbjy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.niamniam.kbjy.profile.DailyGoal
import kotlin.math.abs
import java.util.Locale

@Composable
fun TodaySummary(totals: DiaryItem, goal: DailyGoal, english: Boolean) {
    val difference = goal.calories - totals.calories
    val over = difference < 0
    val accent = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val locale = if (english) Locale.ENGLISH else Locale.forLanguageTag("ru")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(180.dp).align(Alignment.CenterHorizontally), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val width = 14.dp.toPx()
                drawArc(Color.LightGray, -90f, 360f, false, style = Stroke(width))
                drawArc(accent, -90f, (totals.calories / goal.calories).coerceIn(0.0, 1.0).toFloat() * 360f,
                    false, style = Stroke(width, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (over) "+%.0f".format(locale, abs(difference)) else "%.0f".format(locale, difference),
                    style = MaterialTheme.typography.headlineMedium, color = accent)
                Text(when {
                    over -> if (english) "kcal over goal" else "ккал выше цели"
                    difference == 0.0 -> if (english) "Goal reached" else "Цель достигнута"
                    english -> "kcal left"
                    else -> "ккал осталось"
                })
            }
        }
        Text((if (english) "%.0f / %.0f kcal" else "%.0f / %.0f ккал").format(locale, totals.calories, goal.calories))
        MacroProgress(if (english) "Protein" else "Белки", totals.protein, goal.protein, english)
        MacroProgress(if (english) "Fat" else "Жиры", totals.fat, goal.fat, english)
        MacroProgress(if (english) "Carbohydrates" else "Углеводы", totals.carbohydrate, goal.carbohydrate, english)
    }
}

@Composable
private fun MacroProgress(name: String, value: Double, goal: Double, english: Boolean) {
    val difference = goal - value
    val locale = if (english) Locale.ENGLISH else Locale.forLanguageTag("ru")
    Text((if (english) "$name: %.1f / %.1f g" else "$name: %.1f / %.1f г").format(locale, value, goal))
    androidx.compose.material3.LinearProgressIndicator(
        progress = { if (goal > 0) (value / goal).coerceIn(0.0, 1.0).toFloat() else 0f },
        modifier = Modifier.fillMaxWidth(),
        color = if (difference < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
    )
    Text(when {
        difference < 0 -> if (english) "%.1f g above goal".format(locale, -difference) else "На %.1f г выше цели".format(locale, -difference)
        difference == 0.0 -> if (english) "Goal reached" else "Цель достигнута"
        english -> "%.1f g left".format(locale, difference)
        else -> "Осталось %.1f г".format(locale, difference)
    }, style = MaterialTheme.typography.bodySmall)
}
