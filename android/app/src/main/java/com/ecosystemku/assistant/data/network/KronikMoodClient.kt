package com.ecosystemku.assistant.data.network

import com.ecosystemku.assistant.BuildConfig
import okhttp3.OkHttpClient

sealed interface KronikMoodDetectResult {
    data class Success(val mood: String) : KronikMoodDetectResult
    data class Failure(val message: String) : KronikMoodDetectResult
}

class KronikMoodClient(
    private val httpClient: OkHttpClient,
    @Suppress("unused") private val baseUrl: String = BuildConfig.API_BASE_URL
) {
    suspend fun detectMood(text: String): KronikMoodDetectResult {
        val clean = text.trim()
        if (clean.length < 10) {
            return KronikMoodDetectResult.Failure("Tulis dulu isi jurnalnya ya...")
        }

        val prompt =
            "Analisis teks jurnal berikut dan tentukan satu mood yang paling sesuai dari pilihan ini: " +
                "${VALID_MOODS.joinToString(", ")}. Balas hanya dengan satu kata dari pilihan tersebut " +
                "tanpa tanda baca atau penjelasan.\n\nTeks:\n${clean.take(900)}"

        val result = DirectLlmClient.complete(
            httpClient = httpClient,
            messages = listOf(ChatTurnDto("user", prompt)),
            temperature = 0.1,
            maxTokens = 40
        )

        return result.fold(
            onSuccess = { raw ->
                val lower = raw.trim().lowercase()
                val found = VALID_MOODS.firstOrNull { lower == it.lowercase() }
                    ?: VALID_MOODS.firstOrNull { lower.contains(it.lowercase()) }
                if (found != null) {
                    KronikMoodDetectResult.Success(found)
                } else {
                    KronikMoodDetectResult.Failure("Mood tidak terdeteksi, pilih manual ya")
                }
            },
            onFailure = { err ->
                val msg = when (err.message) {
                    "quota" -> "Kuota AI habis, coba beberapa saat lagi."
                    else -> err.message ?: "Gagal terhubung ke AI. Coba lagi."
                }
                KronikMoodDetectResult.Failure(msg)
            }
        )
    }

    companion object {
        private val VALID_MOODS = listOf(
            "Fokus",
            "Lelah",
            "Terinspirasi",
            "Berat",
            "Stabil",
            "Semangat",
            "Kabur"
        )
    }
}
