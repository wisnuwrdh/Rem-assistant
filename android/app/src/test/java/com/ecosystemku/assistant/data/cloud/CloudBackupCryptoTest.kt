package com.ecosystemku.assistant.data.cloud

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.AEADBadTagException

class CloudBackupCryptoTest {

    @Test
    fun encryptAndDecryptRoundTripRestoresOriginalBytes() {
        val sample = "PK\u0003\u0004mock-zip-content-rem-assistant".toByteArray(Charsets.UTF_8)
        assertFalse(CloudBackupCrypto.isEncrypted(sample))

        val encrypted = CloudBackupCrypto.encrypt(sample, "rahasia-tuan-123")
        assertTrue(CloudBackupCrypto.isEncrypted(encrypted))

        val decrypted = CloudBackupCrypto.decrypt(encrypted, "rahasia-tuan-123")
        assertArrayEquals(sample, decrypted)
    }

    @Test(expected = AEADBadTagException::class)
    fun decryptWithWrongPassphraseThrows() {
        val sample = "mock-zip-bytes".toByteArray(Charsets.UTF_8)
        val encrypted = CloudBackupCrypto.encrypt(sample, "passphrase-benar")
        CloudBackupCrypto.decrypt(encrypted, "passphrase-salah")
    }

    @Test(expected = IllegalArgumentException::class)
    fun decryptPlainBytesThrowsInvalidFormat() {
        CloudBackupCrypto.decrypt("not-encrypted".toByteArray(Charsets.UTF_8), "abc")
    }
}
