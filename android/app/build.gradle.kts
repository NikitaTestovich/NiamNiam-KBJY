plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.niamniam.kbjy"
    compileSdk = 37
    defaultConfig {
        applicationId = providers.gradleProperty("deviceTestApplicationId").getOrElse("com.niamniam.kbjy")
        minSdk = 26
        targetSdk = 37
        versionCode = providers.gradleProperty("appVersionCode").get().toInt()
        versionName = providers.gradleProperty("appVersionName").get()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["notAnnotation"] = "com.niamniam.kbjy.DemoDataCommand"
        val intake = providers.gradleProperty("catalogIntakeUrl").getOrElse("")
        require(intake.isEmpty() || intake.matches(Regex("https://script\\.google\\.com/macros/s/[A-Za-z0-9_-]+/exec")))
        buildConfigField("String", "CATALOG_INTAKE_URL", "\"$intake\"")
    }
    buildFeatures { compose = true; buildConfig = true }
    buildTypes {
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("cz.adaptech.tesseract4android:tesseract4android:4.9.0")
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation("com.google.zxing:core:3.5.3")
}

// Measured unit-test baseline for hand-written, Android-independent source files.
// These are counts of uncovered lines/branches, not a claimed whole-app target.
tasks.register("verifyUnitCoverageBaseline") {
    group = "verification"
    description = "Fails if pure-logic unit coverage regresses from the measured baseline."
    dependsOn("createDebugUnitTestCoverageReport")
    doLast {
        val baseline = mapOf(
            "com.niamniam.kbjy/DisplayNumbers.kt" to (0 to 0),
            "com.niamniam.kbjy/GoalCalculator.kt" to (0 to 8),
            "com.niamniam.kbjy/PortionCalculator.kt" to (0 to 14),
            "com.niamniam.kbjy/DiaryTotals.kt" to (0 to 0),
            "com.niamniam.kbjy.profile/CurrentWeight.kt" to (0 to 0),
            "com.niamniam.kbjy.profile/ProfileGoals.kt" to (2 to 6),
            "com.niamniam.kbjy.scanner/LabelParser.kt" to (0 to 4),
            "com.niamniam.kbjy.scanner/LabelOcrFallback.kt" to (0 to 4),
            "com.niamniam.kbjy.reports/DailyReport.kt" to (2 to 11),
            "com.niamniam.kbjy.reports/XlsxExporter.kt" to (2 to 10),
            "com.niamniam.kbjy.update/DownloadAddress.kt" to (0 to 3),
            "com.niamniam.kbjy.update/ReleaseSelection.kt" to (0 to 9),
            "com.niamniam.kbjy.update/UpdatePayload.kt" to (0 to 3),
            "com.niamniam.kbjy.update/UpdateValidation.kt" to (0 to 3),
            "com.niamniam.kbjy.update/UpdateDownloader.kt" to (1 to 2),
        )
        val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD, "")
            setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
        }
        val report = factory.newDocumentBuilder().parse(layout.buildDirectory.file("reports/coverage/test/debug/report.xml").get().asFile)
        val observed = mutableSetOf<String>()
        val packages = report.getElementsByTagName("package")
        for (i in 0 until packages.length) {
            val pkg = packages.item(i) as org.w3c.dom.Element
            val files = pkg.getElementsByTagName("sourcefile")
            for (j in 0 until files.length) {
                val source = files.item(j) as org.w3c.dom.Element
                val key = pkg.getAttribute("name").replace('/', '.') + "/" + source.getAttribute("name")
                val limits = baseline[key] ?: continue
                val counters = source.getElementsByTagName("counter")
                val values = (0 until counters.length).associate { index ->
                    val counter = counters.item(index) as org.w3c.dom.Element
                    counter.getAttribute("type") to counter.getAttribute("missed").toInt()
                }
                check(values.containsKey("LINE")) { "Missing line counter for $key" }
                val lines = values.getValue("LINE")
                val branches = values["BRANCH"] ?: 0
                check(lines <= limits.first && branches <= limits.second) {
                    "Coverage regression in $key: $lines missed lines / $branches missed branches; baseline ${limits.first} / ${limits.second}. Add meaningful tests."
                }
                observed += key
            }
        }
        check(observed == baseline.keys) { "Coverage report is missing files: ${baseline.keys - observed}" }
        logger.lifecycle("Unit coverage baseline passed for ${observed.size} pure-logic source files.")
    }
}
