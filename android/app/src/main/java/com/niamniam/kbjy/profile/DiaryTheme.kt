package com.niamniam.kbjy.profile

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal fun diaryColorScheme(theme: AppTheme) = when (theme) {
    AppTheme.LIGHT -> lightColorScheme(
        primary = Color(0xFF234C96), onPrimary = Color.White,
        primaryContainer = Color(0xFFDCE7FF), onPrimaryContainer = Color(0xFF102B60),
        secondary = Color(0xFF4B5977), secondaryContainer = Color(0xFFDEE5F5), onSecondaryContainer = Color(0xFF263653),
        background = Color(0xFFF7F8FC), surface = Color(0xFFF7F8FC), onSurface = Color(0xFF191F2B),
        surfaceContainerHigh = Color(0xFFE7ECF7), surfaceContainerHighest = Color(0xFFDFE6F4),
        onSurfaceVariant = Color(0xFF454E61), outline = Color(0xFF727D94),
        tertiaryContainer = Color(0xFFC9DBD5), surfaceVariant = Color(0xFFE3E9F4),
        error = Color(0xFFAA2234),
    )
    AppTheme.DARK -> darkColorScheme(
        primary = Color(0xFFCCC0FF), onPrimary = Color(0xFF261456),
        primaryContainer = Color(0xFF443479), onPrimaryContainer = Color(0xFFF0EAFF),
        secondary = Color(0xFFC5C1E2), secondaryContainer = Color(0xFF3B3653), onSecondaryContainer = Color(0xFFECE5FF),
        background = Color(0xFF151421), surface = Color(0xFF151421), onSurface = Color(0xFFF3EFFA),
        surfaceContainerHigh = Color(0xFF26243A), surfaceContainerHighest = Color(0xFF322F48),
        onSurfaceVariant = Color(0xFFCAC4D9), outline = Color(0xFF9C94B1),
        tertiaryContainer = Color(0xFF384F48), surfaceVariant = Color(0xFF38354B),
        error = Color(0xFFFFA9AE),
    )
    AppTheme.FOREST -> lightColorScheme(
        primary = Color(0xFF15543C), onPrimary = Color.White,
        primaryContainer = Color(0xFFBEDDBE), onPrimaryContainer = Color(0xFF103824),
        secondary = Color(0xFF5C5829), secondaryContainer = Color(0xFFE3E3AD), onSecondaryContainer = Color(0xFF373B13),
        background = Color(0xFFE3EDE1), surface = Color(0xFFE3EDE1), onSurface = Color(0xFF15291F),
        surfaceContainerHigh = Color(0xFFCFDFCA), surfaceContainerHighest = Color(0xFFC3D7BE),
        onSurfaceVariant = Color(0xFF3B5143), outline = Color(0xFF5C7462),
        tertiaryContainer = Color(0xFFC3D7AF), surfaceVariant = Color(0xFFDCE5CF),
        error = Color(0xFFA02332),
    )
}

@Composable
fun NiamNiamTheme(theme: AppTheme, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = diaryColorScheme(theme), content = content)
}
