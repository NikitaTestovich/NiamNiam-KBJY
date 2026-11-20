package com.niamniam.kbjy.reports

import com.niamniam.kbjy.data.DiaryEntry
import com.niamniam.kbjy.profile.DailyGoal
import com.niamniam.kbjy.profile.DatedGoal
import com.niamniam.kbjy.profile.WeightMeasurement
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.zip.ZipInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportTest {
    private val first = LocalDate.parse("2026-11-01")
    private fun entry(date: String, name: String, calories: Double, drinkMl: Double = 0.0) =
        DiaryEntry(date = date, meal = "Обед", name = name, amount = 250.0, unit = "мл",
            calories = calories, protein = 0.0, fat = 0.0, carbohydrate = 0.0, drinkMl = drinkMl)

    @Test fun aggregatesOnlyPresentObservations() {
        val report = DailyReports.create(first, first.plusDays(2), listOf(
            entry("2026-11-01", "Вода", 0.0, 250.0),
            entry("2026-11-02", "Сок", 100.0, 250.0),
            entry("2026-11-02", "Еда", 300.0),
            entry("2026-11-04", "Другая дата", 1000.0),
        ))
        assertNull(report.days[0].calories)
        assertEquals(250.0, report.days[0].drinkMl!!, 0.0)
        assertEquals(400.0, report.days[1].calories!!, 0.0)
        assertNull(report.days[2].calories)
        assertNull(report.days[2].drinkMl)
        assertEquals(3, report.entries.size)
    }

    @Test fun writesTypedDatesAndNumbersAndEscapesText() {
        val report = DailyReports.create(first, first, listOf(entry("2026-11-01", "A & <B>", 100.0)))
        val bytes = ByteArrayOutputStream()
        XlsxExporter.write(report, bytes)
        val files = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes.toByteArray())).use { zip ->
            while (true) {
                val next = zip.nextEntry ?: break
                files[next.name] = zip.readBytes().toString(Charsets.UTF_8)
            }
        }
        assertTrue(files.getValue("xl/worksheets/sheet1.xml").contains("r=\"A2\" s=\"1\""))
        assertTrue(files.getValue("xl/worksheets/sheet1.xml").contains("<v>100.0</v>"))
        assertTrue(files.getValue("xl/worksheets/sheet2.xml").contains("A &amp; &lt;B&gt;"))
        assertFalse(files.getValue("xl/worksheets/sheet2.xml").contains("<f>"))
        assertTrue(files.containsKey("xl/worksheets/sheet3.xml"))
    }

    @Test fun preservesDatedGoalsAndAllWeightMeasurements() {
        val oldGoal = DailyGoal(2000.0, 100.0, 70.0, 240.0, null)
        val newGoal = DailyGoal(1800.0, 110.0, 60.0, 210.0, 2000.0)
        val report = DailyReports.create(
            first, first.plusDays(2), emptyList(),
            listOf(DatedGoal(first, oldGoal), DatedGoal(first.plusDays(1), newGoal)),
            listOf(
                WeightMeasurement(id = 1, date = "2026-11-02", kilograms = 82.0),
                WeightMeasurement(id = 2, date = "2026-11-02", kilograms = 81.4),
                WeightMeasurement(id = 3, date = "2026-11-04", kilograms = 80.0),
            ),
        )
        assertEquals(2000.0, report.days[0].goal!!.calories, 0.0)
        assertEquals(1800.0, report.days[1].goal!!.calories, 0.0)
        assertEquals(81.4, report.days[1].weightKg!!, 0.0)
        assertEquals(2, report.weights.size)
        assertEquals(-0.6, report.summary().weightChangeKg!!, 0.0001)
        val output = ByteArrayOutputStream()
        XlsxExporter.write(report, output)
        val files = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            while (true) {
                val next = zip.nextEntry ?: break
                files[next.name] = zip.readBytes().toString(Charsets.UTF_8)
            }
        }
        assertTrue(files.getValue("xl/worksheets/sheet1.xml").contains("<v>1800.0</v>"))
        assertTrue(files.getValue("xl/worksheets/sheet3.xml").contains("<v>82.0</v>"))
        assertTrue(files.getValue("xl/worksheets/sheet3.xml").contains("<v>81.4</v>"))
        assertFalse(files.getValue("xl/worksheets/sheet3.xml").contains("<v>80.0</v>"))
    }

    @Test fun averagesUseObservedDaysAndReportGoalCoverage() {
        val goal = DailyGoal(2000.0, 100.0, 70.0, 240.0, 1500.0)
        val report = DailyReports.create(
            first, first.plusDays(3),
            listOf(
                entry("2026-11-01", "Вода", 0.0, 250.0),
                entry("2026-11-02", "Сок", 100.0, 250.0),
                entry("2026-11-03", "Еда", 300.0),
            ),
            listOf(DatedGoal(first.plusDays(2), goal)),
        )
        val summary = report.summary()
        assertEquals(2, summary.calories.count)
        assertEquals(200.0, summary.calories.actual!!, 0.0)
        assertEquals(1, summary.calories.goalDays)
        assertEquals(2000.0, summary.calories.goal!!, 0.0)
        assertEquals(2, summary.drinkMl.count)
        assertEquals(250.0, summary.drinkMl.actual!!, 0.0)
        assertNull(summary.drinkMl.goal)
    }
}
