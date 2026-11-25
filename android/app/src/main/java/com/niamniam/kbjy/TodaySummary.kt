package com.niamniam.kbjy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.niamniam.kbjy.profile.DailyGoal
import kotlin.math.abs
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

@Composable
internal fun macroColors(): List<Color> = if (MaterialTheme.colorScheme.surface.luminance() < 0.4f) {
    listOf(Color(0xFFAFCCFF), Color(0xFFFFBE80), Color(0xFFB3DAA0))
} else {
    listOf(Color(0xFF245BB2), Color(0xFF9A4D0C), Color(0xFF376C31))
}

@Composable
fun TodaySummary(totals: DiaryItem, goal: DailyGoal?, english: Boolean) {
    val colors = macroColors()
    val labels = if (english) listOf("Protein", "Fat", "Carbohydrates") else listOf("Белки", "Жиры", "Углеводы")
    val values = listOf(totals.protein, totals.fat, totals.carbohydrate)
    val grams = if (english) "g" else "г"
    val kcal = if (english) "kcal" else "ккал"
    fun number(value: Double) = formatDecimal(value, english)
    if (goal == null) {
        Text("${number(totals.calories)} $kcal")
        values.forEachIndexed { index, value -> Text("${labels[index]} · ${number(value)} $grams", color = colors[index]) }
        return
    }
    val difference = goal.calories - totals.calories
    val over = difference < 0
    val accent = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val trackOutline = MaterialTheme.colorScheme.outline
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(180.dp).align(Alignment.CenterHorizontally), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val width = 14.dp.toPx()
                val inset = width / 2 + 1.dp.toPx()
                val bounds = Size(size.width - 2 * inset, size.height - 2 * inset)
                val start = Offset(inset, inset)
                drawArc(track, -90f, 360f, false, start, bounds, style = Stroke(width))
                drawCircle(trackOutline, radius = size.minDimension / 2 - 1.dp.toPx(), style = Stroke(1.dp.toPx()))
                drawArc(accent, -90f, (totals.calories / goal.calories).coerceIn(0.0, 1.0).toFloat() * 360f,
                    false, start, bounds, style = Stroke(width, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (over) "+${number(abs(difference))}" else number(difference),
                    style = MaterialTheme.typography.headlineMedium, color = accent)
                Text(when {
                    over -> if (english) "kcal over goal" else "ккал выше цели"
                    difference == 0.0 -> if (english) "Goal reached" else "Цель достигнута"
                    english -> "kcal left"
                    else -> "ккал осталось"
                })
            }
        }
        Text("${number(totals.calories)} / ${number(goal.calories)} $kcal",
            Modifier.align(Alignment.CenterHorizontally), style = MaterialTheme.typography.titleLarge)
        val targets = listOf(goal.protein, goal.fat, goal.carbohydrate)
        Column(Modifier.fillMaxWidth()) {
            HorizontalDivider()
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(0.34f), contentAlignment = Alignment.CenterStart) {
                    MacroPlate(values, targets, colors,
                        Modifier.widthIn(max = 128.dp).fillMaxWidth().aspectRatio(1f).testTag("macro-plate"))
                }
                Column(Modifier.weight(0.66f).testTag("macro-legend")) {
                    labels.forEachIndexed { index, label ->
                        if (index > 0) HorizontalDivider()
                        MacroLegendRow(label, values[index], targets[index], english, colors[index])
                    }
                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun MacroPlate(values: List<Double>, targets: List<Double>, colors: List<Color>, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Canvas(modifier) {
        drawCircle(scheme.surfaceContainerHigh)
        drawCircle(scheme.outline, radius = size.minDimension / 2 - 1.dp.toPx(), style = Stroke(1.dp.toPx()))
        val inset = size.minDimension * 0.065f
        val start = Offset(inset, inset)
        val plate = Size(size.width - 2 * inset, size.height - 2 * inset)
        val warningInset = size.minDimension * 0.025f
        val warningBounds = Size(size.width - 2 * warningInset, size.height - 2 * warningInset)
        for (index in values.indices) {
            val angle = -88f + 120f * index
            val progress = if (targets[index] > 0) (values[index] / targets[index]).coerceIn(0.0, 1.0).toFloat() else 0f
            drawArc(colors[index].copy(alpha = 0.18f), angle, 116f, true, start, plate)
            if (progress > 0f) drawArc(colors[index], angle, 116f * progress, true, start, plate)
            if (values[index] > targets[index]) {
                drawArc(scheme.error, angle, 116f, false, Offset(warningInset, warningInset), warningBounds, style = Stroke(2.dp.toPx()))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun MacroLegendRow(name: String, value: Double, goal: Double, english: Boolean, color: Color) {
    val difference = goal - value
    val grams = if (english) "g" else "г"
    val amount = formatDecimal(abs(difference), english)
    val facts = buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(formatDecimal(value, english)) }
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
            append(" / ${formatDecimal(goal, english)} $grams")
        }
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(7.dp).background(color, CircleShape))
                Text(name, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(3.dp))
            }
            Text(facts, style = MaterialTheme.typography.bodyMedium)
        }
        Text(when {
            difference < 0 -> if (english) "$amount g above goal" else "На $amount г выше цели"
            difference == 0.0 -> if (english) "Goal reached" else "Цель достигнута"
            english -> "$amount g left"
            else -> "Осталось $amount г"
        }, Modifier.padding(start = 13.dp), style = MaterialTheme.typography.bodySmall,
            color = if (difference < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
