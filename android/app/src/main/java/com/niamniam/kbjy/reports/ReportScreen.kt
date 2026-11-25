package com.niamniam.kbjy.reports

import com.niamniam.kbjy.BackButton

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.niamniam.kbjy.data.DiaryDao
import com.niamniam.kbjy.profile.AppLanguage
import com.niamniam.kbjy.profile.ProfileStore
import com.niamniam.kbjy.profile.WeightDatabase
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private enum class ReportPeriod { WEEK, MONTH, ALL }

@Composable
fun ReportScreen(dao: DiaryDao, language: AppLanguage, onBack: () -> Unit) {
    val english = language == AppLanguage.ENGLISH
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var period by rememberSaveable { mutableStateOf(ReportPeriod.WEEK) }
    var anchorText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val anchor = LocalDate.parse(anchorText)
    var pendingPeriod by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingAnchor by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var exportFailed by rememberSaveable { mutableStateOf(false) }
    var shareFailed by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf<DailyReport?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }
    var loadAttempt by remember { mutableStateOf(0) }
    LaunchedEffect(period, anchor, dao, loadAttempt) {
        loading = true
        loadError = false
        report = null
        try {
            report = withContext(Dispatchers.IO) { loadReport(dao, context, period, anchor) }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            loadError = true
        } finally {
            loading = false
        }
    }
    val save = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(XLSX_MIME_TYPE)
    ) { uri ->
        if (uri != null) {
            val selectedPeriod = pendingPeriod?.let { runCatching { ReportPeriod.valueOf(it) }.getOrNull() }
            val selectedAnchor = pendingAnchor?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            pendingPeriod = null
            pendingAnchor = null
            busy = true
            message = null
            shareFailed = false
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val selectedReport = loadReport(dao, context, checkNotNull(selectedPeriod), checkNotNull(selectedAnchor))
                        val output = context.contentResolver.openOutputStream(uri)
                            ?: error("Could not open selected file")
                        output.use { XlsxExporter.write(selectedReport, it, english) }
                    }
                    message = if (english) "Report saved" else "Отчёт сохранён"
                    exportFailed = false
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    exportFailed = true
                    message = if (english) "Could not save the XLSX file. Check free space and choose a writable folder."
                        else "Не удалось сохранить XLSX. Проверьте свободное место и выберите папку, в которую можно сохранить файл."
                } finally {
                    busy = false
                }
            }
        } else { pendingPeriod = null; pendingAnchor = null }
    }
    val bounds = periodBounds(period, anchor)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        BackButton(english, enabled = !busy, onClick = onBack)
        Text(if (english) "Reports" else "Отчёты")
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            ReportPeriod.entries.forEach { option ->
                val label = when (option) {
                    ReportPeriod.WEEK -> if (english) "Week" else "Неделя"
                    ReportPeriod.MONTH -> if (english) "Month" else "Месяц"
                    ReportPeriod.ALL -> if (english) "All time" else "Всё время"
                }
                OutlinedButton(onClick = { period = option }, enabled = !busy && period != option) {
                    Text(label)
                }
            }
        }
        if (period != ReportPeriod.ALL) {
            Text("${bounds.first} — ${bounds.second}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    anchorText = (if (period == ReportPeriod.WEEK) anchor.minusWeeks(1) else anchor.minusMonths(1)).toString()
                }, enabled = !busy) { Text(if (english) "Previous period" else "Предыдущий период") }
                OutlinedButton(onClick = {
                    anchorText = (if (period == ReportPeriod.WEEK) anchor.plusWeeks(1) else anchor.plusMonths(1)).toString()
                }, enabled = !busy) { Text(if (english) "Next period" else "Следующий период") }
            }
        } else {
            Text(if (english) "All recorded dates" else "Все даты с записями")
            report?.let { Text("${it.from} — ${it.to}") }
        }
        when {
            loading -> Text(if (english) "Loading…" else "Загрузка…")
            loadError -> {
                Text(if (english) "Could not read the diary." else "Не удалось прочитать дневник.", color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { loadAttempt++ }) { Text(if (english) "Retry loading" else "Повторить загрузку") }
            }
            report != null -> ReportSummaryView(checkNotNull(report).summary(), english)
        }
        Button(
            onClick = {
                pendingPeriod = period.name
                pendingAnchor = anchor.toString()
                save.launch("NiamNiam-${LocalDate.now()}.xlsx")
            },
            enabled = !busy && !loading && report != null,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (exportFailed) {
            if (english) "Retry saving" else "Повторить сохранение"
        } else if (english) "Save XLSX" else "Сохранить XLSX") }
        OutlinedButton(
            onClick = {
                val selectedPeriod = period
                val selectedAnchor = anchor
                busy = true
                message = null
                shareFailed = false
                scope.launch {
                    try {
                        val chooser = withContext(Dispatchers.IO) {
                            val selectedReport = loadReport(dao, context, selectedPeriod, selectedAnchor)
                            reportShareIntent(context, selectedReport, english)
                        }
                        context.startActivity(chooser)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        shareFailed = true
                        message = if (english) "Could not share the XLSX file. Try again."
                        else "Не удалось поделиться XLSX. Попробуйте ещё раз."
                    } finally {
                        busy = false
                    }
                }
            },
            enabled = !busy && !loading && report != null,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (english) "Share XLSX" else "Поделиться XLSX") }
        message?.let { Text(it, color = if (exportFailed || shareFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface) }
    }
}

@Composable
private fun ReportSummaryView(summary: ReportSummary, english: Boolean) {
    fun format(value: Double) = String.format(if (english) Locale.US else Locale.forLanguageTag("ru"), "%.1f", value)
    fun line(label: String, value: ReportAverage, unit: String): String {
        val actual = value.actual ?: return "$label: —"
        val goal = value.goal?.let {
            if (english) ", average goal ${format(it)} $unit (${value.goalDays} days)"
            else ", средняя цель ${format(it)} $unit (${value.goalDays} дн.)"
        }.orEmpty()
        return if (english) "$label: ${format(actual)} $unit (${value.count} days)$goal"
        else "$label: ${format(actual)} $unit (${value.count} дн.)$goal"
    }
    Text(if (english) "Daily averages" else "Среднее за день")
    Text(line(if (english) "Calories" else "Калории", summary.calories, if (english) "kcal" else "ккал"))
    Text(line(if (english) "Protein" else "Белки", summary.protein, if (english) "g" else "г"))
    Text(line(if (english) "Fat" else "Жиры", summary.fat, if (english) "g" else "г"))
    Text(line(if (english) "Carbohydrate" else "Углеводы", summary.carbohydrate, if (english) "g" else "г"))
    Text(line(if (english) "Drinks" else "Жидкость", summary.drinkMl, if (english) "ml" else "мл"))
    val change = summary.weightChangeKg?.let { format(it) } ?: "—"
    Text(if (english) "Weight change: $change kg" else "Изменение веса: $change кг")
}

private suspend fun loadReport(
    dao: DiaryDao,
    context: android.content.Context,
    period: ReportPeriod,
    anchor: LocalDate,
): DailyReport {
    val bounds = periodBounds(period, anchor)
    val entries = dao.entriesBetween(bounds.first.toString(), bounds.second.toString())
    val weights = WeightDatabase.get(context).weightDao().observeAll().first()
    val goals = ProfileStore(context).load()?.goals.orEmpty()
    val dates = entries.map { LocalDate.parse(it.date) } + weights.map { LocalDate.parse(it.date) }
    val actualBounds = if (period == ReportPeriod.ALL && dates.isNotEmpty()) {
        dates.minOrNull()!! to dates.maxOrNull()!!
    } else if (period == ReportPeriod.ALL) {
        anchor to anchor
    } else bounds
    return DailyReports.create(actualBounds.first, actualBounds.second, entries, goals, weights)
}

private fun periodBounds(period: ReportPeriod, anchor: LocalDate): Pair<LocalDate, LocalDate> =
    when (period) {
        ReportPeriod.WEEK -> anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).let {
            it to it.plusDays(6)
        }
        ReportPeriod.MONTH -> anchor.withDayOfMonth(1).let {
            it to it.with(TemporalAdjusters.lastDayOfMonth())
        }
        ReportPeriod.ALL -> LocalDate.of(1, 1, 1) to LocalDate.of(9999, 12, 31)
    }
