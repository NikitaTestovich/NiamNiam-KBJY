package com.niamniam.kbjy.update

import org.junit.Assert.*
import org.junit.Test

class ReleaseSelectionTest {
    @Test fun choosesOnlyNewerMatchingAssetWithValidDigestAndTrustedUrl() {
        val hash = "ab".repeat(32)
        val asset = ReleaseAsset("niamniam-kbjy-vc5.apk", "https://github.com/NikitaTestovich/NiamNiam-KBJY/releases/download/v1.20/niamniam-kbjy-vc5.apk", "sha256:$hash")
        assertEquals(5L, chooseReleaseAsset(listOf(asset), 4)?.versionCode)
        assertNull(chooseReleaseAsset(listOf(asset), 5))
        assertNull(chooseReleaseAsset(listOf(asset.copy(url = "https://evil.example/file.apk")), 4))
        assertNull(chooseReleaseAsset(listOf(asset.copy(url = "https://github.com/NikitaTestovich/unrelated-project/releases/download/v1.20/niamniam-kbjy-vc5.apk")), 4))
        assertNull(chooseReleaseAsset(listOf(asset.copy(digest = "sha256:aa")), 4))
        assertNull(chooseReleaseAsset(listOf(asset.copy(name = "other-vc5.apk")), 4))
    }
}
