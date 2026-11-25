package com.niamniam.kbjy

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class NoticeDocument(val file: String, val title: String) {
    OVERVIEW("README.txt", "About the sources and components"),
    ODBL("ODbL-1.0.txt", "Open Database License 1.0"),
    DBCL("DbCL-1.0.txt", "Database Contents License 1.0"),
    APACHE("Apache-2.0.txt", "Apache License 2.0"),
    LEPTONICA("Leptonica-1.85.0.txt", "Leptonica 1.85.0"),
    JPEG("libjpeg-9f-README.txt", "libjpeg 9f"),
    PNG("libpng-1.6.48.txt", "libpng 1.6.48"),
    GOOGLE_SDK("Google-SDK-notices.txt", "Google SDK open-source notices");

    fun label(english: Boolean) = when {
        english -> title
        this == OVERVIEW -> "Об источниках и компонентах"
        this == GOOGLE_SDK -> "Лицензии компонентов Google SDK"
        else -> title
    }
}

private val noticeParagraphBreak = Regex("\\r?\\n[ \\t]*\\r?\\n")

@Composable
internal fun ThirdPartyNoticesScreen(english: Boolean, onBack: () -> Unit) {
    var opened by rememberSaveable { mutableStateOf<NoticeDocument?>(null) }
    val document = opened
    BackHandler(enabled = document != null) { opened = null }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        BackButton(english, onClick = { if (document == null) onBack() else opened = null })
        val title = document?.label(english) ?: if (english) "Sources and licenses" else "Источники и лицензии"
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(vertical = 12.dp))

        if (document == null) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(NoticeDocument.entries) { item ->
                    OutlinedButton(onClick = { opened = item }, modifier = Modifier.fillMaxWidth()) {
                        Text(item.label(english))
                    }
                }
            }
        } else {
            NoticeContents(document, english)
        }
    }
}

@Composable
private fun NoticeContents(document: NoticeDocument, english: Boolean) {
    val assets = LocalContext.current.assets
    val contents by produceState<Result<List<String>>?>(null, document, assets) {
        value = withContext(Dispatchers.IO) {
            try {
                val paragraphs = assets.open("licenses/${document.file}").bufferedReader().use { reader ->
                    noticeParagraphBreak.splitToSequence(reader.readText())
                        .filter(String::isNotBlank)
                        .flatMap { paragraph ->
                            paragraph.lineSequence().chunked(32) { lines -> lines.joinToString("\n") }
                        }
                        .toList()
                }
                if (paragraphs.isEmpty()) throw IOException("Empty notice")
                Result.success(paragraphs)
            } catch (error: IOException) {
                Result.failure(error)
            }
        }
    }
    val result = contents
    val paragraphs = result?.getOrNull()
    when {
        result == null -> Text(if (english) "Loading…" else "Загрузка…")
        paragraphs == null -> Text(
            if (english) "Could not open the notice." else "Не удалось открыть текст.",
            color = MaterialTheme.colorScheme.error,
        )
        else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(paragraphs) { paragraph -> Text(paragraph, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}
