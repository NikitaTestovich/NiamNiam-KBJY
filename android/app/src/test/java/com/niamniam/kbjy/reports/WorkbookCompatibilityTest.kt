package com.niamniam.kbjy.reports

import com.niamniam.kbjy.data.DiaryEntry
import com.niamniam.kbjy.profile.DailyGoal
import com.niamniam.kbjy.profile.DatedGoal
import com.niamniam.kbjy.profile.WeightMeasurement
import java.io.File
import java.time.LocalDate
import org.junit.Test

class WorkbookCompatibilityTest {
    @Test
    fun writesSyntheticWorkbookForIndependentReader() {
        val date = LocalDate.of(2026, 11, 20)
        val juice = DiaryEntry(id = 1, date = date.toString(), meal = "lunch", name = "Тестовый сок",
            amount = 250.0, unit = "ml", calories = 100.0, protein = 0.0, fat = 0.0,
            carbohydrate = 25.0, drinkMl = 250.0)
        val meal = DiaryEntry(id = 2, date = date.toString(), meal = "dinner",
            name = "=1+1 & <рис> 🍚 ＋\r\n\tбез соли\u0000\uD800\uFFFE\uFFFF",
            amount = 125.0, unit = "g", calories = 182.5, protein = 4.0, fat = 1.25, carbohydrate = 38.5)
        val water = juice.copy(
            id = 3, date = date.plusDays(1).toString(), name = "Вода", calories = 0.0, carbohydrate = 0.0,
        )
        val report = DailyReports.create(
            date, date.plusDays(2), listOf(juice, meal, water),
            goals = listOf(
                DatedGoal(date, DailyGoal(2000.0, 100.0, 70.0, 240.0, null)),
                DatedGoal(date.plusDays(1), DailyGoal(1800.0, 110.0, 60.0, 210.0, 2000.0)),
            ),
            weights = listOf(
                WeightMeasurement(1, date.toString(), 82.0),
                WeightMeasurement(2, date.toString(), 81.4),
            ),
        )
        val directory = File("build/reports")
        directory.mkdirs()
        for ((filename, english) in listOf("synthetic-workbook.xlsx" to false, "synthetic-workbook-en.xlsx" to true)) {
            File(directory, filename).outputStream().use { XlsxExporter.write(report, it, english) }
        }
    }
}
