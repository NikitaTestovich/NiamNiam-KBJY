package com.niamniam.kbjy

import android.content.Intent
import android.os.SystemClock
import androidx.core.content.FileProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.niamniam.kbjy.data.DiaryDatabase
import com.niamniam.kbjy.data.DiaryEntry
import com.niamniam.kbjy.data.Food
import com.niamniam.kbjy.profile.AppLanguage
import com.niamniam.kbjy.profile.AppTheme
import com.niamniam.kbjy.profile.DailyGoal
import com.niamniam.kbjy.profile.DatedGoal
import com.niamniam.kbjy.profile.Profile
import com.niamniam.kbjy.profile.ProfileState
import com.niamniam.kbjy.profile.ProfileStore
import com.niamniam.kbjy.profile.Sex
import com.niamniam.kbjy.profile.WeightDatabase
import com.niamniam.kbjy.profile.WeightMeasurement
import com.niamniam.kbjy.reports.DailyReports
import com.niamniam.kbjy.reports.XlsxExporter
import com.niamniam.kbjy.update.ApkIdentityReader
import com.niamniam.kbjy.update.RELEASE_REPOSITORY
import com.niamniam.kbjy.update.ReleaseAsset
import com.niamniam.kbjy.update.UpdateDownloader
import com.niamniam.kbjy.update.UpdateInstaller
import com.niamniam.kbjy.update.chooseReleaseAsset
import com.niamniam.kbjy.update.isAcceptableUpdate
import java.io.File
import java.net.HttpURLConnection
import java.security.MessageDigest
import java.time.LocalDate
import java.util.zip.ZipFile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class UpdatePersistenceTest {
    @Test
    fun cameraAndUpdateUrisRemainReadableInTheSameProcess() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val providers = mapOf("files" to "camera", "updates" to "updates")
        for ((authority, directory) in providers) {
            val cache = File(context.cacheDir, directory)
            check(cache.isDirectory || cache.mkdirs())
            val fixture = File.createTempFile("provider-test-", ".tmp", cache)
            try {
                fixture.writeText(authority)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.$authority", fixture)
                val payload = checkNotNull(context.contentResolver.openInputStream(uri)).bufferedReader().use { it.readText() }
                assertEquals(authority, payload)
            } finally {
                fixture.delete()
            }
        }
    }

    @Test
    @DemoDataCommand
    fun openSystemInstallerForTheVerifiedCandidate(): Unit = runBlocking {
        val arguments = InstrumentationRegistry.getArguments()
        assumeTrue(arguments.getString("upgradePhase") == "installer")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        check(context.packageName == "com.niamniam.kbjy.upgradetest.installer")
        check(context.packageManager.canRequestPackageInstalls())
        val version = checkNotNull(arguments.getString("expectedCode")).toLong()
        val candidate = File(context.cacheDir, "upgrade-candidate.apk")
        val name = "niamniam-kbjy-vc${version + 1}.apk"
        val releaseAsset = ReleaseAsset(name, "https://github.com/$RELEASE_REPOSITORY/releases/download/test/$name",
            "sha256:${checkNotNull(arguments.getString("candidateSha"))}")
        val asset = checkNotNull(chooseReleaseAsset(listOf(releaseAsset), version))
        val downloader = UpdateDownloader { url ->
            object : HttpURLConnection(url) {
                override fun connect() = Unit
                override fun disconnect() = Unit
                override fun usingProxy() = false
                override fun getResponseCode() = HTTP_OK
                override fun getInputStream() = candidate.inputStream()
            }
        }
        val intent = UpdateInstaller(context, downloader).prepare(asset)
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("application/vnd.android.package-archive", intent.type)
        assertEquals("${context.packageName}.updates", intent.data?.authority)
        assertTrue((intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
        val updateProvider = checkNotNull(context.packageManager.resolveContentProvider("${context.packageName}.updates", 0))
        val sharingProvider = checkNotNull(context.packageManager.resolveContentProvider("${context.packageName}.files", 0))
        assertTrue("The update and sharing authorities must use distinct provider classes", updateProvider.name != sharingProvider.name)
        context.contentResolver.openAssetFileDescriptor(checkNotNull(intent.data), "r").use { descriptor ->
            assertEquals(candidate.length(), checkNotNull(descriptor).length)
        }

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { it.startActivity(intent) }
            val automation = instrumentation.uiAutomation
            val deadline = SystemClock.uptimeMillis() + 10_000
            while (SystemClock.uptimeMillis() < deadline) {
                val confirmation = automation.rootInActiveWindow?.findAccessibilityNodeInfosByText("Update")
                    ?.any { it.isClickable && it.packageName.toString().contains("packageinstaller") } == true
                if (confirmation) return@runBlocking
                SystemClock.sleep(200)
            }
            error("The Android package installer did not offer to update the test app")
        }
    }

    @Test
    @DemoDataCommand
    fun seedOrVerifyAcrossPackageReplacement() {
        val arguments = InstrumentationRegistry.getArguments()
        val phase = arguments.getString("upgradePhase")
        assumeTrue(phase == "before" || phase == "after")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName in setOf("com.niamniam.kbjy.upgradetest", "com.niamniam.kbjy.upgradetest.installer"))
        val identity = ApkIdentityReader(context)
        val installed = identity.installed()
        val expectedCode = checkNotNull(arguments.getString("expectedCode")).toLong()
        assertEquals(expectedCode, installed.versionCode)

        val date = LocalDate.of(2026, 1, 12)
        val food = Food(501, "Рис тест обновления", "г", 120.0, 2.0, 1.0, 26.0, 150.0)
        val meal = DiaryEntry(501, date.toString(), "lunch", food.name, 150.0, "г", 180.0, 3.0, 1.5, 39.0)
        val drink = DiaryEntry(502, date.toString(), "lunch", "Вода тест обновления", 250.0, "мл", 0.0, 0.0, 0.0, 0.0, 250.0)
        val weight = WeightMeasurement(501, date.toString(), 71.5)
        val profile = Profile(Sex.FEMALE, 33, 174.0, 73.0, 1.375, AppLanguage.RUSSIAN, AppTheme.LIGHT)
        val goal = DailyGoal(1950.0, 110.0, 67.0, 227.0, 1800.0)
        val barcode = "0123456789012"
        val savedReport = File(context.filesDir, "upgrade-example.xlsx")
        val diary = DiaryDatabase.get(context).diaryDao()
        val weights = WeightDatabase.get(context).weightDao()
        val profiles = ProfileStore(context)

        runBlocking {
            if (phase == "before") {
                val apk = File(context.cacheDir, "upgrade-candidate.apk")
                val digest = MessageDigest.getInstance("SHA-256")
                apk.inputStream().use { input ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        digest.update(buffer, 0, count)
                    }
                }
                val candidateHash = digest.digest().joinToString("") { "%02x".format(it) }
                val candidate = identity.archive(apk)
                assertTrue(isAcceptableUpdate(installed, candidate, checkNotNull(arguments.getString("candidateSha")), candidateHash))
                assertEquals(expectedCode + 1, candidate.versionCode)
                diary.saveNewFoodAndEntry(food, meal, barcode)
                diary.insert(drink)
                weights.insert(weight)
                profiles.save(profile, goal, date)
                val report = DailyReports.create(date, date, listOf(meal, drink), listOf(DatedGoal(date, goal)), listOf(weight))
                savedReport.outputStream().use { XlsxExporter.write(report, it) }
            }

            assertEquals(listOf(meal, drink), diary.entriesBetween(date.toString(), date.toString()))
            assertEquals(food, diary.findByBarcode(barcode))
            assertEquals(listOf(weight), weights.observeAll().first())
            assertEquals(ProfileState(profile, listOf(DatedGoal(date, goal))), profiles.load())
            ZipFile(savedReport).use { workbook ->
                val diarySheet = workbook.getInputStream(workbook.getEntry("xl/worksheets/sheet2.xml"))
                    .bufferedReader().use { it.readText() }
                assertTrue(diarySheet.contains(food.name))
                assertTrue(diarySheet.contains(drink.name))
            }
        }
        println("Upgrade persistence phase ${phase} verified for versionCode ${installed.versionCode}")
    }
}
