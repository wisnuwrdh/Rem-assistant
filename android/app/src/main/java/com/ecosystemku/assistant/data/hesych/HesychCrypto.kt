package com.ecosystemku.assistant.data.hesych

import com.ecosystemku.assistant.data.local.HesychArgon2ParamsDto
import com.ecosystemku.assistant.data.local.HesychPwEnvelopeDto
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters

/**
 * Byte-compatible cryptographic primitives matching Hesych Web (WebCrypto):
 * - AES-256-GCM ciphertext wire format: base64(iv(12) || ciphertext || authTag(16))
 * - Envelope KDF v2: Argon2id (64 MiB, 3 iterations, parallelism 1)
 * - Envelope KDF v1 (legacy): PBKDF2-HMAC-SHA256 (600,000 iterations)
 */
object HesychCrypto {
    const val AES_IV_LEN = 12
    const val GCM_TAG_BITS = 128
    const val PBKDF2_ITERATIONS = 600_000
    const val DEK_BYTES = 32
    const val SALT_BYTES = 16

    val DEFAULT_ARGON2_PARAMS = HesychArgon2ParamsDto(
        name = "argon2id",
        memoryKiB = 65536,
        iterations = 3,
        parallelism = 1
    )

    private val secureRandom = SecureRandom()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun randomBytes(length: Int): ByteArray = ByteArray(length).also { secureRandom.nextBytes(it) }

    fun toBase64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    fun fromBase64(b64: String): ByteArray = Base64.getDecoder().decode(b64)

    fun importAesKey(raw32Bytes: ByteArray): SecretKey {
        require(raw32Bytes.size == DEK_BYTES) { "AES-256 key must be 32 bytes" }
        return SecretKeySpec(raw32Bytes.copyOf(), "AES")
    }

    fun generateRawDek(): ByteArray = randomBytes(DEK_BYTES)

    fun derivePbkdf2Key(
        password: String,
        salt: ByteArray,
        iterations: Int = PBKDF2_ITERATIONS
    ): SecretKey {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, DEK_BYTES * 8)
        return try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val encoded = factory.generateSecret(spec).encoded
            SecretKeySpec(encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    fun deriveArgon2idKey(
        password: String,
        salt: ByteArray,
        params: HesychArgon2ParamsDto = DEFAULT_ARGON2_PARAMS
    ): SecretKey {
        val builder = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withSalt(salt)
            .withMemoryAsKB(params.memoryKiB)
            .withIterations(params.iterations)
            .withParallelism(params.parallelism)
        val gen = Argon2BytesGenerator()
        gen.init(builder.build())
        val out = ByteArray(DEK_BYTES)
        val pwBytes = password.toByteArray(StandardCharsets.UTF_8)
        return try {
            gen.generateBytes(pwBytes, out, 0, out.size)
            SecretKeySpec(out, "AES")
        } finally {
            pwBytes.fill(0)
            out.fill(0)
        }
    }

    fun encryptWith(key: SecretKey, plaintext: String): String {
        val iv = randomBytes(AES_IV_LEN)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ct = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
        val combined = ByteArray(AES_IV_LEN + ct.size)
        System.arraycopy(iv, 0, combined, 0, AES_IV_LEN)
        System.arraycopy(ct, 0, combined, AES_IV_LEN, ct.size)
        return toBase64(combined)
    }

    fun decryptWith(key: SecretKey, b64Ciphertext: String): String {
        val buf = fromBase64(b64Ciphertext)
        if (buf.size <= AES_IV_LEN) {
            throw IllegalArgumentException("Invalid ciphertext")
        }
        val iv = buf.copyOfRange(0, AES_IV_LEN)
        val ct = buf.copyOfRange(AES_IV_LEN, buf.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val pt = cipher.doFinal(ct)
        return String(pt, StandardCharsets.UTF_8)
    }

    inline fun <reified T> encryptMetaValue(key: SecretKey, value: T?): String? {
        if (value == null) return null
        val encoded = Json.encodeToString(value)
        return encryptWith(key, encoded)
    }

    inline fun <reified T> decryptMetaValue(
        key: SecretKey,
        b64Ciphertext: String?,
        fallback: T
    ): T {
        if (b64Ciphertext.isNullOrBlank()) return fallback
        return runCatching {
            val pt = decryptWith(key, b64Ciphertext)
            Json { ignoreUnknownKeys = true }.decodeFromString<T>(pt)
        }.getOrDefault(fallback)
    }

    fun isEncMeta(value: String?): Boolean {
        if (value == null || value.length < 24) return false
        return value.matches(Regex("^[A-Za-z0-9+/]+=*$"))
    }

    fun buildVerifier(key: SecretKey, magic: String): String = encryptWith(key, magic)

    fun checkVerifier(key: SecretKey, verifierB64: String, expectedMagic: String): Boolean {
        if (verifierB64.isBlank()) return false
        return runCatching { decryptWith(key, verifierB64) == expectedMagic }.getOrDefault(false)
    }

    fun createPasswordEnvelope(
        password: String,
        rawDek: ByteArray
    ): HesychPwEnvelopeDto {
        val salt = randomBytes(SALT_BYTES)
        val kek = deriveArgon2idKey(password, salt, DEFAULT_ARGON2_PARAMS)
        val wrap = encryptWith(kek, toBase64(rawDek))
        return HesychPwEnvelopeDto(
            v = 2,
            salt = toBase64(salt),
            wrap = wrap,
            kdf = DEFAULT_ARGON2_PARAMS
        )
    }

    fun unwrapWithPassword(
        envelope: HesychPwEnvelopeDto,
        password: String
    ): ByteArray {
        val salt = fromBase64(envelope.salt)
        val kek = if (envelope.v == 2) {
            val params = envelope.kdf ?: DEFAULT_ARGON2_PARAMS
            deriveArgon2idKey(password, salt, params)
        } else {
            derivePbkdf2Key(password, salt, PBKDF2_ITERATIONS)
        }
        val b64Dek = decryptWith(kek, envelope.wrap)
        return fromBase64(b64Dek)
    }

    fun needsUpgrade(envelope: HesychPwEnvelopeDto): Boolean = envelope.v == 1

    fun encodeEnvelope(envelope: HesychPwEnvelopeDto): String = json.encodeToString(envelope)

    fun decodeEnvelope(raw: String): HesychPwEnvelopeDto? = runCatching {
        json.decodeFromString<HesychPwEnvelopeDto>(raw)
    }.getOrNull()
}
