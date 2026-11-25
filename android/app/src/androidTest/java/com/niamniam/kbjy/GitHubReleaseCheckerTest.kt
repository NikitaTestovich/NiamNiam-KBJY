package com.niamniam.kbjy

import com.niamniam.kbjy.update.GitHubReleaseChecker
import com.niamniam.kbjy.update.ReleaseCheckResult
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.URL
import java.security.cert.Certificate
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubReleaseCheckerTest {
    @Test
    fun returnsCompatibleUpdateWithReleaseNotesAndClosesConnection() {
        val (result, connection) = checkResponse(200, release("Список изменений"))
        val update = result as ReleaseCheckResult.Available
        assertEquals("v0.6", update.versionName)
        assertEquals("Список изменений", update.releaseNotes)
        assertEquals(2L, update.asset.versionCode)
        assertEquals("ab".repeat(32), update.asset.sha256)
        assertEquals("application/vnd.github+json", connection.getRequestProperty("Accept"))
        assertFalse(connection.instanceFollowRedirects)
        assertTrue(connection.disconnected)
    }

    @Test
    fun missingOrNonTextReleaseNotesAreDisplayedAsEmpty() {
        for (body in listOf(null, JSONObject.NULL, 42)) {
            val (result, _) = checkResponse(200, release(body))
            assertEquals("", (result as ReleaseCheckResult.Available).releaseNotes)
        }
    }

    @Test
    fun installedVersionIsNotOfferedAgain() {
        val (result, _) = checkResponse(200, release(""), installedVersion = 2)
        assertEquals(ReleaseCheckResult.NoCompatibleUpdate, result)
    }

    @Test
    fun distinguishesMissingReleaseFromServerErrorAndRedirect() {
        for ((status, expected) in listOf(
            404 to ReleaseCheckResult.NoRelease,
            503 to ReleaseCheckResult.Failed,
            302 to ReleaseCheckResult.Failed,
        )) {
            val (result, connection) = checkResponse(status, "")
            assertEquals("HTTP $status", expected, result)
            assertFalse(connection.bodyOpened)
            assertTrue(connection.disconnected)
        }
    }

    @Test
    fun rejectsPreReleaseDraftMalformedAndOversizedMetadata() {
        val malformedResponses = listOf(
            "not JSON",
            release("", prerelease = true),
            release("", draft = true),
            release("x".repeat(1_048_576)),
        )
        for ((index, response) in malformedResponses.withIndex()) {
            val (result, connection) = checkResponse(200, response)
            assertEquals("Unsupported response #$index", ReleaseCheckResult.Failed, result)
            assertTrue(connection.disconnected)
        }
    }

    @Test
    fun transportErrorBecomesFailedAndCancellationPropagates() {
        runBlocking {
            assertEquals(ReleaseCheckResult.Failed, GitHubReleaseChecker { throw IOException("offline") }.check(1))
            val cancellation = runCatching {
                GitHubReleaseChecker { throw CancellationException("cancelled") }.check(1)
            }.exceptionOrNull()
            assertTrue(cancellation is CancellationException)
        }
    }

    private fun checkResponse(status: Int, body: String, installedVersion: Long = 1): Pair<ReleaseCheckResult, StubConnection> {
        lateinit var connection: StubConnection
        val checker = GitHubReleaseChecker { url ->
            assertEquals("https://api.github.com/repos/NikitaTestovich/NiamNiam-KBJY/releases/latest", url.toExternalForm())
            StubConnection(url, status, body).also { connection = it }
        }
        return runBlocking { checker.check(installedVersion) } to connection
    }

    private fun release(body: Any?, draft: Boolean = false, prerelease: Boolean = false): String {
        val name = "niamniam-kbjy-vc2.apk"
        val asset = JSONObject()
            .put("name", name)
            .put("browser_download_url", "https://github.com/NikitaTestovich/NiamNiam-KBJY/releases/download/v0.6/$name")
            .put("digest", "sha256:" + "ab".repeat(32))
        return JSONObject()
            .put("tag_name", "v0.6")
            .put("body", body)
            .put("draft", draft)
            .put("prerelease", prerelease)
            .put("assets", JSONArray().put(asset))
            .toString()
    }

    private class StubConnection(url: URL, private val status: Int, private val body: String) : HttpsURLConnection(url) {
        var disconnected = false
        var bodyOpened = false

        override fun getResponseCode() = status
        override fun getInputStream(): ByteArrayInputStream {
            bodyOpened = true
            return ByteArrayInputStream(body.toByteArray(Charsets.UTF_8))
        }
        override fun disconnect() { disconnected = true }
        override fun connect() = Unit
        override fun usingProxy() = false
        override fun getCipherSuite() = ""
        override fun getLocalCertificates(): Array<Certificate>? = null
        override fun getServerCertificates(): Array<Certificate> = emptyArray()
    }
}
