package com.niamniam.kbjy.update

import org.junit.Assert.*
import org.junit.Test

class UpdateValidationTest {
    @Test fun updateMustBeNewerAndMatchIdentitySignatureAndDigest() {
        val certificate = "ab".repeat(32)
        val digest = "01".repeat(32)
        val installed = UpdateIdentity("com.niamniam.kbjy", 4, certificate.uppercase().chunked(2).joinToString(":"))
        val candidate = UpdateIdentity("com.niamniam.kbjy", 5, certificate)
        assertTrue(isAcceptableUpdate(installed, candidate, digest, digest))
        assertFalse(isAcceptableUpdate(installed, candidate.copy(versionCode = 4), digest, digest))
        assertFalse(isAcceptableUpdate(installed, candidate.copy(packageName = "other"), digest, digest))
        assertFalse(isAcceptableUpdate(installed, candidate.copy(certificateSha256 = "cc".repeat(32)), digest, digest))
        assertFalse(isAcceptableUpdate(installed, candidate, digest, "45".repeat(32)))
        assertFalse(isAcceptableUpdate(installed, candidate, "", ""))
    }
}
