package com.niamniam.kbjy.reports

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
    var period by remember { mutableStateOf(ReportPeriod.WEEK) }
    var anchor by remember { mutableStateOf(LocalDate.now()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var report by remember { mutableStateOf<DailyReport?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }
    LaunchedEffect(period, anchor, dao) {
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
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        if (uri != null) {
            val selectedReport = report
            busy = true
            message = null
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val output = context.contentResolver.openOutputStream(uri)
                            ?: error("Could not open selected file")
                        output.use { XlsxExporter.write(checkNotNull(selectedReport), it, english) }
                    }
                    message = if (english) "Report saved" else "Отчёт сохранён"
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    message = if (english) "Could not save the report" else "Не удалось сохранить отчёт"
                } finally {
                    busy = false
                }
            }
        }
    }
    val bounds = periodBounds(period, anchor)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        OutlinedButton(onClick = onBack, enabled = !busy) { Text(if (english) "Back" else "Назад") }
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
                    anchor = if (period == ReportPeriod.WEEK) anchor.minusWeeks(1) else anchor.minusMonths(1)
                }, enabled = !busy) { Text(if (english) "Previous" else "Назад") }
                OutlinedButton(onClick = {
                    anchor = if (period == ReportPeriod.WEEK) anchor.plusWeeks(1) else anchor.plusMonths(1)
                }, enabled = !busy) { Text(if (english) "Next" else "Вперёд") }
            }
        } else {
            Text(if (english) "All recorded dates" else "Все даты с записями")
            report?.let { Text("${it.from} — ${it.to}") }
        }
        when {
            loading -> Text(if (english) "Loading…" else "Загрузка…")
            loadError -> Text(if (english) "Could not load report" else "Не удалось загрузить отчёт")
            report != null -> ReportSummaryView(checkNotNull(report).summary(), english)
        }
        Button(
            onClick = { save.launch("NiamNiam-${LocalDate.now()}.xlsx") },
            enabled = !busy && !loading && report != null,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (english) "Save XLSX" else "Сохранить XLSX") }
        message?.let { Text(it) }
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
