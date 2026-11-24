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

enum class AiConfigField(
    val prefKey: String,
    val title: String,
    val isSecret: Boolean,
    val placeholder: String,
    val helper: String,
    val emptyLabel: String
) {
    LLM_BASE_URL(
        prefKey = "llm_base_url",
        title = "Base URL LLM",
        isSecret = false,
        placeholder = "https://openrouter.ai/api/v1",
        helper = "Endpoint OpenAI-compatible. Akhiran /chat/completions otomatis ditambahkan bila belum ada.",
        emptyLabel = "Belum diisi (wajib)"
    ),
    LLM_MODEL(
        prefKey = "llm_model",
        title = "Model LLM",
        isSecret = false,
        placeholder = "openai/gpt-4o-mini",
        helper = "Nama ID model persis sesuai penyedia LLM (dipakai untuk chat, memori, & semua modul AI).",
        emptyLabel = "Belum diisi (wajib)"
    ),
    LLM_API_KEY(
        prefKey = "llm_api_key",
        title = "API Key LLM",
        isSecret = true,
        placeholder = "sk-...",
        helper = "Disimpan terenkripsi AES-256-GCM di Android Keystore perangkat ini.",
        emptyLabel = "Belum diisi (wajib)"
    ),
    TTS_API_KEY(
        prefKey = "tts_api_key",
        title = "API Key TTS",
        isSecret = true,
        placeholder = "Masukkan API Key Fish Audio...",
        helper = "Disimpan terenkripsi AES-256-GCM di Android Keystore perangkat ini.",
        emptyLabel = "Belum diisi"
    ),
    TTS_VOICE_ID(
        prefKey = "tts_voice_id",
        title = "Voice ID TTS",
        isSecret = false,
        placeholder = "eb79df9571014449b42bcd869f86fe0a",
        helper = "Kosongkan untuk memakai Voice ID bawaan Fish Audio.",
        emptyLabel = "Bawaan"
    ),
    TTS_MODEL(
        prefKey = "tts_model",
        title = "Model TTS",
        isSecret = false,
        placeholder = "s2.1-pro-free",
        helper = "Kosongkan untuk memakai model bawaan Fish Audio (s2.1-pro-free).",
        emptyLabel = "Bawaan (s2.1-pro-free)"
    ),
    TTS_BASE_URL(
        prefKey = "tts_base_url",
        title = "Base URL TTS",
        isSecret = false,
        placeholder = "https://api.fish.audio/v1/tts",
        helper = "Kosongkan untuk memakai endpoint bawaan Fish Audio.",
        emptyLabel = "Bawaan (Fish Audio)"
    )
}

data class AiConfigSnapshot(
    val llmBaseUrl: String = AiConfigField.LLM_BASE_URL.emptyLabel,
    val llmModel: String = AiConfigField.LLM_MODEL.emptyLabel,
    val llmMaskedKey: String = AiConfigField.LLM_API_KEY.emptyLabel,
    val ttsMaskedKey: String = AiConfigField.TTS_API_KEY.emptyLabel,
    val ttsVoiceId: String = AiConfigField.TTS_VOICE_ID.emptyLabel,
    val ttsModel: String = AiConfigField.TTS_MODEL.emptyLabel,
    val ttsBaseUrl: String = AiConfigField.TTS_BASE_URL.emptyLabel,
    val isLlmConfigured: Boolean = false
) {
    fun displayFor(field: AiConfigField): String = when (field) {
        AiConfigField.LLM_BASE_URL -> llmBaseUrl
        AiConfigField.LLM_MODEL -> llmModel
        AiConfigField.LLM_API_KEY -> llmMaskedKey
        AiConfigField.TTS_API_KEY -> ttsMaskedKey
        AiConfigField.TTS_VOICE_ID -> ttsVoiceId
        AiConfigField.TTS_MODEL -> ttsModel
        AiConfigField.TTS_BASE_URL -> ttsBaseUrl
    }

    fun isFilled(field: AiConfigField): Boolean = displayFor(field) != field.emptyLabel
}

object ApiKeyStore {
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "ecosystemku_apikey_master"
    private const val PREFS_NAME = "secure_apikeys_prefs"

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun prefs(context: Context? = appContext) =
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getValue(field: AiConfigField, context: Context? = appContext): String {
        val p = prefs(context) ?: return ""
        return if (field.isSecret) {
            val raw = p.getString("enc_${field.prefKey}", null)
                ?: if (field == AiConfigField.TTS_API_KEY) p.getString("enc_fish_audio", null) else null
            if (raw.isNullOrEmpty()) "" else decrypt(raw)
        } else {
            p.getString("cfg_${field.prefKey}", "")?.trim().orEmpty()
        }
    }

    fun setValue(field: AiConfigField, value: String, context: Context? = appContext) {
        val p = prefs(context) ?: return
        val clean = value.trim()
        val editor = p.edit()
        if (field.isSecret) {
            val key = "enc_${field.prefKey}"
            if (clean.isEmpty()) {
                editor.remove(key)
                if (field == AiConfigField.TTS_API_KEY) editor.remove("enc_fish_audio")
            } else {
                editor.putString(key, encrypt(clean))
            }
        } else {
            val key = "cfg_${field.prefKey}"
            if (clean.isEmpty()) editor.remove(key) else editor.putString(key, clean)
        }
        editor.apply()
    }

    fun displayValue(field: AiConfigField, context: Context? = appContext): String {
        val raw = getValue(field, context)
        if (raw.isEmpty()) return field.emptyLabel
        if (!field.isSecret) return raw
        return if (raw.length <= 4) "••••" else "•••• ${raw.takeLast(4)}"
    }

    fun isLlmConfigured(context: Context? = appContext): Boolean {
        val base = getValue(AiConfigField.LLM_BASE_URL, context)
        val model = getValue(AiConfigField.LLM_MODEL, context)
        val key = getValue(AiConfigField.LLM_API_KEY, context)
        return (base.startsWith("http://", ignoreCase = true) ||
            base.startsWith("https://", ignoreCase = true)) &&
            model.isNotBlank() &&
            key.isNotBlank()
    }

    fun snapshot(context: Context? = appContext): AiConfigSnapshot = AiConfigSnapshot(
        llmBaseUrl = displayValue(AiConfigField.LLM_BASE_URL, context),
        llmModel = displayValue(AiConfigField.LLM_MODEL, context),
        llmMaskedKey = displayValue(AiConfigField.LLM_API_KEY, context),
        ttsMaskedKey = displayValue(AiConfigField.TTS_API_KEY, context),
        ttsVoiceId = displayValue(AiConfigField.TTS_VOICE_ID, context),
        ttsModel = displayValue(AiConfigField.TTS_MODEL, context),
        ttsBaseUrl = displayValue(AiConfigField.TTS_BASE_URL, context),
        isLlmConfigured = isLlmConfigured(context)
    )

    fun activeHeaders(context: Context? = appContext): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        val llmBase = getValue(AiConfigField.LLM_BASE_URL, context)
        val llmModel = getValue(AiConfigField.LLM_MODEL, context)
        val llmKey = getValue(AiConfigField.LLM_API_KEY, context)
        if (llmBase.isNotBlank()) headers["X-LLM-Base"] = llmBase
        if (llmModel.isNotBlank()) headers["X-LLM-Model"] = llmModel
        if (llmKey.isNotBlank()) headers["X-LLM-Key"] = llmKey

        val ttsKey = getValue(AiConfigField.TTS_API_KEY, context)
        val ttsVoice = getValue(AiConfigField.TTS_VOICE_ID, context)
        val ttsModel = getValue(AiConfigField.TTS_MODEL, context)
        val ttsBase = getValue(AiConfigField.TTS_BASE_URL, context)
        if (ttsKey.isNotBlank()) headers["X-TTS-Key"] = ttsKey
        if (ttsVoice.isNotBlank()) headers["X-TTS-Voice"] = ttsVoice
        if (ttsModel.isNotBlank()) headers["X-TTS-Model"] = ttsModel
        if (ttsBase.isNotBlank()) headers["X-TTS-Base"] = ttsBase
        return headers
    }

    fun getCloudPassphrase(context: Context? = appContext): String {
        val p = prefs(context) ?: return ""
        val raw = p.getString("enc_cloud_passphrase", null)
        return if (raw.isNullOrEmpty()) "" else decrypt(raw)
    }

    fun setCloudPassphrase(passphrase: String, context: Context? = appContext) {
        val p = prefs(context) ?: return
        val clean = passphrase.trim()
        val editor = p.edit()
        if (clean.isEmpty()) {
            editor.remove("enc_cloud_passphrase")
        } else {
            editor.putString("enc_cloud_passphrase", encrypt(clean))
        }
        editor.apply()
    }

    fun hasCloudPassphrase(context: Context? = appContext): Boolean =
        getCloudPassphrase(context).isNotEmpty()

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
