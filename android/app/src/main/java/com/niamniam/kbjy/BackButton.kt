package com.niamniam.kbjy

import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
internal fun BackButton(english: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, enabled = enabled) {
        Text(if (english) "Back" else "Назад")
    }
}
