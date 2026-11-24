package com.ecosystemku.assistant.data.cloud

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Enkripsi & dekripsi arsip ZIP v3 berbasis passphrase (PBKDF2-HMAC-SHA256 + AES-256-GCM).
 * Format biner: [5B magic "REMC1"][16B salt][12B IV][N ciphertext + 16B GCM tag].
 */
object CloudBackupCrypto {
    private val MAGIC = byteArrayOf('R'.code.toByte(), 'E'.code.toByte(), 'M'.code.toByte(), 'C'.code.toByte(), '1'.code.toByte())
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val GCM_TAG_BITS = 128
    private const val PBKDF2_ITERATIONS = 100_000
    private const val KEY_BITS = 256
    private const val HEADER_BYTES = 5 + SALT_BYTES + IV_BYTES

    private val secureRandom = SecureRandom()

    fun isEncrypted(bytes: ByteArray): Boolean {
        if (bytes.size < MAGIC.size) return false
        for (i in MAGIC.indices) {
            if (bytes[i] != MAGIC[i]) return false
        }
        return true
    }

    fun encrypt(plainBytes: ByteArray, passphrase: String): ByteArray {
        val clean = passphrase.trim()
        require(clean.isNotEmpty()) { "Passphrase tidak boleh kosong" }

        val salt = ByteArray(SALT_BYTES).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { secureRandom.nextBytes(it) }
        val key = deriveKey(clean, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val cipherBytes = cipher.doFinal(plainBytes)

        return ByteArray(HEADER_BYTES + cipherBytes.size).apply {
            System.arraycopy(MAGIC, 0, this, 0, MAGIC.size)
            System.arraycopy(salt, 0, this, MAGIC.size, SALT_BYTES)
            System.arraycopy(iv, 0, this, MAGIC.size + SALT_BYTES, IV_BYTES)
            System.arraycopy(cipherBytes, 0, this, HEADER_BYTES, cipherBytes.size)
        }
    }

    fun decrypt(payload: ByteArray, passphrase: String): ByteArray {
        val clean = passphrase.trim()
        require(clean.isNotEmpty()) { "Passphrase diperlukan untuk membuka cadangan terenkripsi" }
        require(isEncrypted(payload) && payload.size > HEADER_BYTES + 16) {
            "Format berkas cadangan terenkripsi tidak valid"
        }

        val salt = payload.copyOfRange(MAGIC.size, MAGIC.size + SALT_BYTES)
        val iv = payload.copyOfRange(MAGIC.size + SALT_BYTES, HEADER_BYTES)
        val cipherBytes = payload.copyOfRange(HEADER_BYTES, payload.size)
        val key = deriveKey(clean, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(cipherBytes)
    }

    private fun deriveKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_BITS)
        val raw = factory.generateSecret(spec).encoded
        return SecretKeySpec(raw, "AES")
    }
}
