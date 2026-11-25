package com.niamniam.kbjy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

@Composable
fun FluidRiver(consumedMl: Double, goalMl: Double?, english: Boolean) {
    val target = goalMl?.takeIf { it.isFinite() && it > 0 } ?: 2000.0
    val progress = (consumedMl / target).coerceIn(0.0, 1.0).toFloat()
    val format = NumberFormat.getNumberInstance(if (english) Locale.ENGLISH else Locale.forLanguageTag("ru")).apply {
        isGroupingUsed = false
        maximumFractionDigits = 2
    }
    val bankColor = MaterialTheme.colorScheme.tertiaryContainer
    val bedColor = MaterialTheme.colorScheme.surfaceVariant
    Column {
        val amount = "${format.format(consumedMl)} / ${format.format(target)}"
        Text(if (english) "Fluid: $amount ml" else "Жидкость: $amount мл")
        Canvas(Modifier.fillMaxWidth().height(66.dp).semantics {
            progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
        }) {
            val w = size.width
            val h = size.height
            val river = Path().apply {
                moveTo(0f, h * .56f)
                cubicTo(w * .14f, h * .14f, w * .22f, h * .37f, w * .36f, h * .56f)
                cubicTo(w * .52f, h * .78f, w * .62f, h * .84f, w * .75f, h * .60f)
                cubicTo(w * .86f, h * .40f, w * .92f, h * .41f, w, h * .49f)
            }
            val tributary = Path().apply {
                moveTo(w * .31f, h * .13f)
                cubicTo(w * .40f, h * .10f, w * .43f, h * .30f, w * .50f, h * .47f)
                cubicTo(w * .55f, h * .61f, w * .59f, h * .72f, w * .65f, h * .71f)
                moveTo(w * .08f, h * .86f)
                cubicTo(w * .14f, h * .92f, w * .17f, h * .71f, w * .21f, h * .63f)
                cubicTo(w * .25f, h * .55f, w * .28f, h * .49f, w * .31f, h * .50f)
                moveTo(w * .71f, h * .15f)
                cubicTo(w * .77f, h * .12f, w * .78f, h * .25f, w * .81f, h * .31f)
                cubicTo(w * .84f, h * .37f, w * .87f, h * .42f, w * .92f, h * .44f)
            }
            val mainWidth = h * .32f
            val branchWidth = h * .14f
            val bankMargin = h * .12f
            fun channel(width: Float) = Stroke(width, cap = StrokeCap.Round)
            drawPath(river, bankColor, style = channel(mainWidth + bankMargin))
            drawPath(tributary, bankColor, style = channel(branchWidth + bankMargin))
            drawPath(river, bedColor, style = channel(mainWidth))
            drawPath(tributary, bedColor, style = channel(branchWidth))
            clipRect(right = w * progress) {
                val water = Brush.horizontalGradient(listOf(Color(0xFF1474AC), Color(0xFF249BA9), Color(0xFF176C9F)))
                drawPath(tributary, water, style = channel(branchWidth))
                drawPath(river, water, style = channel(mainWidth))
                val current = Path().apply {
                    moveTo(w * .08f, h * .42f)
                    quadraticTo(w * .15f, h * .34f, w * .22f, h * .42f)
                    moveTo(w * .39f, h * .55f)
                    quadraticTo(w * .48f, h * .70f, w * .54f, h * .67f)
                    moveTo(w * .78f, h * .52f)
                    quadraticTo(w * .85f, h * .40f, w * .92f, h * .43f)
                }
                drawPath(current, Color(0x99C6F4F1), style = channel(h * .03f))
            }
        }
    }
}
