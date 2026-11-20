package com.niamniam.kbjy.reports

import java.io.OutputStream
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Writes a self-contained workbook to the stream selected by the user. */
object XlsxExporter {
    fun write(report: DailyReport, output: OutputStream, english: Boolean = false) {
        val names = if (english) listOf("Days", "Diary", "Weight") else listOf("По дням", "Дневник", "Вес")
        val dailyHeaders = if (english) {
            listOf("Date", "Calories, kcal", "Calories goal, kcal", "Protein, g", "Protein goal, g", "Fat, g", "Fat goal, g", "Carbohydrate, g", "Carbohydrate goal, g", "Drinks, ml", "Drinks goal, ml", "Weight, kg")
        } else {
            listOf("Дата", "Калории, ккал", "Цель калорий, ккал", "Белки, г", "Цель белков, г", "Жиры, г", "Цель жиров, г", "Углеводы, г", "Цель углеводов, г", "Напитки, мл", "Цель жидкости, мл", "Вес, кг")
        }
        val entryHeaders = if (english) {
            listOf("Date", "Meal", "Name", "Amount", "Unit", "Calories, kcal", "Protein, g", "Fat, g", "Carbohydrate, g", "Drinks, ml")
        } else {
            listOf("Дата", "Приём пищи", "Название", "Количество", "Единица", "Калории, ккал", "Белки, г", "Жиры, г", "Углеводы, г", "Напитки, мл")
        }
        val dailyRows = listOf(dailyHeaders) + report.days.map {
            listOf(it.date, it.calories, it.goal?.calories, it.protein, it.goal?.protein, it.fat, it.goal?.fat, it.carbohydrate, it.goal?.carbohydrate, it.drinkMl, it.goal?.waterMl, it.weightKg)
        }
        val entryRows = listOf(entryHeaders) + report.entries.map {
            listOf(LocalDate.parse(it.date), it.meal, it.name, it.amount, it.unit, it.calories, it.protein, it.fat, it.carbohydrate, it.drinkMl.takeIf { value -> value > 0 })
        }
        val weightHeaders = if (english) listOf("Date", "Weight, kg") else listOf("Дата", "Вес, кг")
        val weightRows = listOf(weightHeaders) + report.weights.map { listOf(LocalDate.parse(it.date), it.kilograms) }
        ZipOutputStream(output).use { zip ->
            zip.file("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>""")
            zip.file("_rels/.rels", """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""")
            zip.file("xl/workbook.xml", """<?xml version="1.0" encoding="UTF-8"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="${names[0]}" sheetId="1" r:id="rId1"/><sheet name="${names[1]}" sheetId="2" r:id="rId2"/><sheet name="${names[2]}" sheetId="3" r:id="rId4"/></sheets></workbook>""")
            zip.file("xl/_rels/workbook.xml.rels", """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/><Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/><Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/></Relationships>""")
            zip.file("xl/styles.xml", """<?xml version="1.0" encoding="UTF-8"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts><fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills><borders count="1"><border/></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="14" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/></cellXfs></styleSheet>""")
            zip.file("xl/worksheets/sheet1.xml", sheet(dailyRows))
            zip.file("xl/worksheets/sheet2.xml", sheet(entryRows))
            zip.file("xl/worksheets/sheet3.xml", sheet(weightRows))
        }
    }

    private fun ZipOutputStream.file(path: String, content: String) {
        putNextEntry(ZipEntry(path))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun sheet(rows: List<List<Any?>>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
        rows.forEachIndexed { rowIndex, row ->
            append("<row r=\"${rowIndex + 1}\">")
            row.forEachIndexed { column, value ->
                if (value != null) {
                    val ref = "${('A'.code + column).toChar()}${rowIndex + 1}"
                    when (value) {
                        is LocalDate -> append("<c r=\"$ref\" s=\"1\"><v>${value.toEpochDay() + 25569}</v></c>")
                        is Number -> {
                            require(value.toDouble().isFinite()) { "Non-finite number" }
                            append("<c r=\"$ref\"><v>$value</v></c>")
                        }
                        else -> append("<c r=\"$ref\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${escape(value.toString())}</t></is></c>")
                    }
                }
            }
            append("</row>")
        }
        append("</sheetData></worksheet>")
    }

    private fun escape(value: String): String = buildString {
        value.forEach { char ->
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                else -> if (char == '\n' || char == '\r' || char == '\t' || char >= ' ') append(char)
            }
        }
    }
}
