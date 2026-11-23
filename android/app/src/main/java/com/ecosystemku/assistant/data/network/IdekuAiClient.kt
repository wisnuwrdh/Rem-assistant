package com.ecosystemku.assistant.data.network

import com.ecosystemku.assistant.BuildConfig
import okhttp3.OkHttpClient

sealed interface IdekuAiResult {
    data class Success(val text: String) : IdekuAiResult
    data class Failure(val message: String) : IdekuAiResult
}

class IdekuAiClient(
    private val httpClient: OkHttpClient,
    @Suppress("unused") private val baseUrl: String = BuildConfig.API_BASE_URL
) {
    suspend fun generate(
        title: String,
        notes: String,
        category: String,
        priority: String,
        mode: String
    ): IdekuAiResult {
        val cleanTitle = title.trim().take(200)
        if (cleanTitle.isEmpty()) {
            return IdekuAiResult.Failure("Judul ide kosong")
        }

        val cleanNotes = notes.trim().take(1500)
        val cleanCategory = category.ifBlank { "Personal" }
        val cleanPriority = priority.ifBlank { "Nanti" }
        val isBreakdown = mode == "breakdown"

        val priorityCtx = when (cleanPriority) {
            "Penting" -> "tinggi — eksekusi dalam 1–2 minggu"
            "Nanti" -> "sedang — bisa dimulai bulan ini"
            "Kapan-kapan" -> "rendah — tanpa deadline ketat"
            else -> "fleksibel"
        }

        val systemPrompt = if (isBreakdown) {
            "Kamu adalah asisten yang membantu merapikan dan menyusun ide menjadi prompt breakdown yang siap dipakai di AI lain (ChatGPT, Gemini, dll). Tugasmu bukan mengeksekusi breakdown, tapi menghasilkan prompt yang terstruktur, jelas, dan kaya konteks — supaya AI yang menerima prompt ini bisa langsung menghasilkan rencana eksekusi yang konkret dan actionable. Gunakan bahasa Indonesia yang santai."
        } else {
            "Kamu adalah asisten kreatif dan strategis yang membantu memperkaya ide singkat menjadi catatan yang tajam, konkret, dan siap dieksekusi dalam bahasa Indonesia yang ringkas dan natural."
        }

        val userPrompt = if (isBreakdown) {
            val notesLine = if (cleanNotes.isNotEmpty()) "\nCatatan: $cleanNotes" else ""
            """
Rapikan ide berikut menjadi prompt breakdown yang siap di-paste ke AI lain:

Judul: $cleanTitle
Kategori: $cleanCategory
Prioritas: $cleanPriority ($priorityCtx)$notesLine

Hasilkan prompt yang:
1. Punya konteks ide yang jelas dan lengkap
2. Meminta rencana eksekusi 5–8 langkah konkret dengan estimasi waktu
3. Menyebutkan kategori "$cleanCategory" supaya AI bisa menyesuaikan saran tools/platform yang relevan
4. Mencerminkan urgensi berdasarkan prioritas "$cleanPriority"
5. Meminta langkah pertama yang bisa dilakukan hari ini atau minggu ini

Keluarkan hanya teks prompt-nya saja, tanpa pembuka atau penutup tambahan.
            """.trimIndent()
        } else {
            val notesLine = if (cleanNotes.isNotEmpty()) "\nCatatan awal: $cleanNotes" else ""
            """
Perkaya ide berikut menjadi catatan ringkas namun kaya wawasan:

Judul: $cleanTitle
Kategori: $cleanCategory
Prioritas: $cleanPriority$notesLine

Berikan:
- Inti konsep & nilai utamanya (2 kalimat)
- 3 langkah awal paling konkret untuk memulai
- Potensi tantangan atau tools/referensi yang relevan

Tulis rapi, padat, langsung ke poin dalam bahasa Indonesia.
            """.trimIndent()
        }

        val result = DirectLlmClient.complete(
            httpClient = httpClient,
            messages = listOf(
                ChatTurnDto("system", systemPrompt),
                ChatTurnDto("user", userPrompt)
            ),
            temperature = 0.65,
            maxTokens = 700
        )

        return result.fold(
            onSuccess = { text -> IdekuAiResult.Success(text) },
            onFailure = { err ->
                val errText = when (err.message) {
                    "quota" -> "Kuota AI habis, coba beberapa saat lagi."
                    else -> err.message ?: "Gagal menghubungi AI. Periksa koneksi internet."
                }
                IdekuAiResult.Failure(errText)
            }
        )
    }
}
