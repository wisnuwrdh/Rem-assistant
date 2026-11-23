package com.ecosystemku.assistant.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object ApiKeyStore {
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "ecosystemku_apikey_master"
    private const val PREFS_NAME = "secure_apikeys_prefs"
    private const val KEY_USE_PERSONAL = "use_personal_keys"

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun prefs(context: Context? = appContext) =
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isUsingPersonalKeys(context: Context? = appContext): Boolean =
        prefs(context)?.getBoolean(KEY_USE_PERSONAL, false) ?: false

    fun setUsingPersonalKeys(enabled: Boolean, context: Context? = appContext) {
        prefs(context)?.edit()?.putBoolean(KEY_USE_PERSONAL, enabled)?.apply()
    }

    fun getKey(provider: String, context: Context? = appContext): String {
        val p = prefs(context) ?: return ""
        val raw = p.getString("enc_${normalizeProvider(provider)}", null) ?: return ""
        return decrypt(raw)
    }

    fun setKey(provider: String, value: String, context: Context? = appContext) {
        val p = prefs(context) ?: return
        val norm = normalizeProvider(provider)
        val clean = value.trim()
        if (clean.isEmpty()) {
            p.edit().remove("enc_$norm").apply()
        } else {
            val encrypted = encrypt(clean)
            p.edit().putString("enc_$norm", encrypted).apply()
        }
    }

    fun maskedKey(provider: String, context: Context? = appContext): String {
        val raw = getKey(provider, context)
        return when {
            raw.isEmpty() -> "Belum diisi"
            raw.length <= 4 -> "••••"
            else -> "•••• ${raw.takeLast(4)}"
        }
    }

    fun activeHeaders(): Map<String, String> {
        if (!isUsingPersonalKeys()) return emptyMap()
        val headers = mutableMapOf<String, String>()
        val sumopod = getKey("Sumopod")
        val groq = getKey("Groq")
        val fish = getKey("Fish Audio")
        if (sumopod.isNotBlank()) headers["X-Sumopod-Key"] = sumopod
        if (groq.isNotBlank()) headers["X-Groq-Key"] = groq
        if (fish.isNotBlank()) headers["X-Fish-Key"] = fish
        return headers
    }

    private fun normalizeProvider(provider: String): String =
        provider.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_")

    private fun getOrCreateSecretKey(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existing = ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun encrypt(plainText: String): String {
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
            val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val enc = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
            "$iv:$enc"
        }.getOrDefault("")
    }

    private fun decrypt(payload: String): String {
        return runCatching {
            val parts = payload.split(":", limit = 2)
            if (parts.size != 2) return ""
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val cipherBytes = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        }.getOrDefault("")
    }
}
