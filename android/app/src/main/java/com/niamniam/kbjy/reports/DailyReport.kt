package com.niamniam.kbjy.reports

import com.niamniam.kbjy.data.DiaryEntry
import com.niamniam.kbjy.profile.DatedGoal
import com.niamniam.kbjy.profile.DailyGoal
import com.niamniam.kbjy.profile.WeightMeasurement
import com.niamniam.kbjy.profile.goalForDate
import java.time.LocalDate

data class DailyReportRow(
    val date: LocalDate,
    val calories: Double?,
    val protein: Double?,
    val fat: Double?,
    val carbohydrate: Double?,
    val drinkMl: Double?,
    val goal: DailyGoal? = null,
    val weightKg: Double? = null,
)

data class DailyReport(
    val from: LocalDate,
    val to: LocalDate,
    val days: List<DailyReportRow>,
    val entries: List<DiaryEntry>,
    val weights: List<WeightMeasurement> = emptyList(),
)

data class ReportAverage(val count: Int, val actual: Double?, val goal: Double?, val goalDays: Int)

data class ReportSummary(
    val calories: ReportAverage,
    val protein: ReportAverage,
    val fat: ReportAverage,
    val carbohydrate: ReportAverage,
    val drinkMl: ReportAverage,
    val weightChangeKg: Double?,
)

fun DailyReport.summary(): ReportSummary {
    fun average(actual: (DailyReportRow) -> Double?, goal: (DailyReportRow) -> Double?): ReportAverage {
        val observed = days.mapNotNull { row -> actual(row)?.let { row to it } }
        val goals = observed.mapNotNull { goal(it.first) }
        return ReportAverage(
            observed.size,
            observed.map { it.second }.average().takeIf { observed.isNotEmpty() },
            goals.average().takeIf { goals.isNotEmpty() },
            goals.size,
        )
    }
    return ReportSummary(
        average({ it.calories }, { it.goal?.calories }),
        average({ it.protein }, { it.goal?.protein }),
        average({ it.fat }, { it.goal?.fat }),
        average({ it.carbohydrate }, { it.goal?.carbohydrate }),
        average({ it.drinkMl }, { it.goal?.waterMl }),
        if (weights.size >= 2) weights.last().kilograms - weights.first().kilograms else null,
    )
}

object DailyReports {
    fun create(
        from: LocalDate,
        to: LocalDate,
        entries: List<DiaryEntry>,
        goals: List<DatedGoal> = emptyList(),
        weights: List<WeightMeasurement> = emptyList(),
    ): DailyReport {
        require(!to.isBefore(from)) { "End date precedes start date" }
        val selected = entries.map { LocalDate.parse(it.date) to it }
            .filter { (date, _) -> date in from..to }
            .sortedWith(compareBy({ it.first }, { it.second.id }))
        val byDate = selected.groupBy({ it.first }, { it.second })
        val selectedWeights = weights.filter { LocalDate.parse(it.date) in from..to }
            .sortedWith(compareBy({ it.date }, { it.id }))
        val weightsByDate = selectedWeights.groupBy { LocalDate.parse(it.date) }
        val days = ArrayList<DailyReportRow>()
        var date = from
        while (!date.isAfter(to)) {
            val dayEntries = byDate[date].orEmpty()
            val hasFood = dayEntries.any {
                it.drinkMl <= 0.0 || it.calories != 0.0 || it.protein != 0.0 ||
                    it.fat != 0.0 || it.carbohydrate != 0.0
            }
            val hasDrink = dayEntries.any { it.drinkMl > 0.0 }
            days += DailyReportRow(
                date,
                dayEntries.sumOf { it.calories }.takeIf { hasFood },
                dayEntries.sumOf { it.protein }.takeIf { hasFood },
                dayEntries.sumOf { it.fat }.takeIf { hasFood },
                dayEntries.sumOf { it.carbohydrate }.takeIf { hasFood },
                dayEntries.sumOf { it.drinkMl }.takeIf { hasDrink },
                goalForDate(goals, date),
                weightsByDate[date]?.lastOrNull()?.kilograms,
            )
            date = date.plusDays(1)
        }
        return DailyReport(from, to, days, selected.map { it.second }, selectedWeights)
    }
}
