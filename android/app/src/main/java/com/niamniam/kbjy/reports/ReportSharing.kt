package com.niamniam.kbjy.reports

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit

internal const val XLSX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

internal fun reportShareIntent(context: Context, report: DailyReport, english: Boolean): Intent {
    val cache = File(context.cacheDir, "shared-reports")
    check(cache.isDirectory || cache.mkdirs()) { "Could not create report cache" }
    val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
    cache.listFiles()?.forEach { previous ->
        if (previous.name.startsWith("share-") && previous.lastModified() < cutoff) previous.deleteRecursively()
    }
    val directory = Files.createTempDirectory(cache.toPath(), "share-").toFile()
    val file = File(directory, "NiamNiam-${report.from}--${report.to}.xlsx")
    return try {
        file.outputStream().use { XlsxExporter.write(report, it, english) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = XLSX_MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        Intent.createChooser(send, null)
    } catch (exception: Exception) {
        file.delete()
        directory.delete()
        throw exception
    }
}
