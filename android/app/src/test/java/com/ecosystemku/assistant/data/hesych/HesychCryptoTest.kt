package com.ecosystemku.assistant.data.hesych

import com.ecosystemku.assistant.data.local.HesychPwEnvelopeDto
import java.nio.charset.StandardCharsets
import javax.crypto.AEADBadTagException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HesychCryptoTest {

    @Test
    fun base64RoundTripsUtf8Bytes() {
        val raw = "héllo wörld ✓".toByteArray(StandardCharsets.UTF_8)
        assertArrayEquals(raw, HesychCrypto.fromBase64(HesychCrypto.toBase64(raw)))
    }

    @Test
    fun aesGcmEncryptDecryptAndRejectWrongKey() {
        val keyA = HesychCrypto.importAesKey(ByteArray(32) { 1 })
        val keyB = HesychCrypto.importAesKey(ByteArray(32) { 2 })

        val ct1 = HesychCrypto.encryptWith(keyA, "rahasia vault")
        val ct2 = HesychCrypto.encryptWith(keyA, "rahasia vault")
        assertNotEquals(ct1, ct2)
        assertEquals("rahasia vault", HesychCrypto.decryptWith(keyA, ct1))
        assertEquals("rahasia vault", HesychCrypto.decryptWith(keyA, ct2))

        var failed = false
        try {
            HesychCrypto.decryptWith(keyB, ct1)
        } catch (_: AEADBadTagException) {
            failed = true
        }
        assertTrue(failed)
    }

    @Test
    fun metadataEncryptionAndLegacyFallback() {
        val key = HesychCrypto.importAesKey(ByteArray(32) { 9 })
        val encTags = HesychCrypto.encryptMetaValue(key, listOf("kerja", "penting"))
        assertTrue(HesychCrypto.isEncMeta(encTags))

        val decoded = HesychCrypto.decryptMetaValue(key, encTags, emptyList<String>())
        assertEquals(listOf("kerja", "penting"), decoded)

        // Legacy plaintext fallback
        assertFalse(HesychCrypto.isEncMeta("gaming"))
        assertEquals("other", HesychCrypto.decryptMetaValue(key, "gaming", "other"))
        assertEquals("other", HesychCrypto.decryptMetaValue(key, null, "other"))
    }

    @Test
    fun argon2idEnvelopeRoundTripsAndRejectsWrongPassword() {
        val rawDek = HesychCrypto.generateRawDek()
        assertEquals(32, rawDek.size)

        val envA = HesychCrypto.createPasswordEnvelope("correct horse battery", rawDek)
        val envB = HesychCrypto.createPasswordEnvelope("correct horse battery", rawDek)
        assertEquals(2, envA.v)
        assertFalse(HesychCrypto.needsUpgrade(envA))
        assertNotEquals(envA.salt, envB.salt)

        val unwrapped = HesychCrypto.unwrapWithPassword(envA, "correct horse battery")
        assertArrayEquals(rawDek, unwrapped)

        var wrongFailed = false
        try {
            HesychCrypto.unwrapWithPassword(envA, "wrong-password")
        } catch (_: Exception) {
            wrongFailed = true
        }
        assertTrue(wrongFailed)
    }

    @Test
    fun legacyPbkdf2EnvelopeUnwrapsAndFlagsUpgrade() {
        val rawDek = HesychCrypto.generateRawDek()
        val salt = ByteArray(16) { 5 }
        val kek = HesychCrypto.derivePbkdf2Key("legacy-pw", salt, iterations = 10_000)
        // Verify direct PBKDF2 key wrap + v1 envelope detection
        val wrap = HesychCrypto.encryptWith(kek, HesychCrypto.toBase64(rawDek))
        val legacyEnv = HesychPwEnvelopeDto(
            v = 1,
            salt = HesychCrypto.toBase64(salt),
            wrap = wrap,
            kdf = null
        )
        assertTrue(HesychCrypto.needsUpgrade(legacyEnv))

        // Full 600k PBKDF2 v1 unwrap
        val kek600 = HesychCrypto.derivePbkdf2Key("legacy-pw", salt)
        val env600 = legacyEnv.copy(wrap = HesychCrypto.encryptWith(kek600, HesychCrypto.toBase64(rawDek)))
        val unwrapped = HesychCrypto.unwrapWithPassword(env600, "legacy-pw")
        assertArrayEquals(rawDek, unwrapped)
    }

    @Test
    fun verifierValidatesCorrectKeyAndRejectsWrongKey() {
        val keyA = HesychCrypto.importAesKey(ByteArray(32) { 3 })
        val keyB = HesychCrypto.importAesKey(ByteArray(32) { 4 })
        val verifier = HesychCrypto.buildVerifier(keyA, "hesych-vault-ok-v1")
        assertTrue(HesychCrypto.checkVerifier(keyA, verifier, "hesych-vault-ok-v1"))
        assertFalse(HesychCrypto.checkVerifier(keyB, verifier, "hesych-vault-ok-v1"))
    }
}
