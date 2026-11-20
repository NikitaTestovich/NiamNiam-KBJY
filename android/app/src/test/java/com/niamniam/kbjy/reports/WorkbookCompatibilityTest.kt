package com.niamniam.kbjy.reports

import com.niamniam.kbjy.data.DiaryEntry
import java.io.File
import java.time.LocalDate
import org.junit.Test

class WorkbookCompatibilityTest {
    @Test
    fun writesSyntheticWorkbookForIndependentReader() {
        val date = LocalDate.of(2026, 11, 20)
        val entry = DiaryEntry(date = date.toString(), meal = "lunch", name = "Тестовый сок",
            amount = 250.0, unit = "ml", calories = 100.0, protein = 0.0, fat = 0.0,
            carbohydrate = 25.0, drinkMl = 250.0)
        val report = DailyReports.create(date, date, listOf(entry))
        val output = File("build/reports/synthetic-workbook.xlsx")
        requireNotNull(output.parentFile).mkdirs()
        output.outputStream().use { XlsxExporter.write(report, it) }
    }
}
