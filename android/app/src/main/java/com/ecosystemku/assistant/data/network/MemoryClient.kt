package com.ecosystemku.assistant.data.network

import com.ecosystemku.assistant.BuildConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

@Serializable
data class ExistingFactDto(
    val factKey: String,
    val factValue: String
)

@Serializable
data class ExtractedFactDto(
    val category: String = "catatan",
    val factKey: String = "",
    val factValue: String = "",
    val confidence: Float = 0.8f
)

@Serializable
data class MemoryExtractResponseDto(
    val facts: List<ExtractedFactDto> = emptyList(),
    val summary: String? = null
)

class MemoryClient(
    private val httpClient: OkHttpClient,
    @Suppress("unused") private val baseUrl: String = BuildConfig.API_BASE_URL
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun extractMemories(
        messages: List<ChatTurnDto>,
        existingFacts: List<ExistingFactDto>
    ): MemoryExtractResponseDto? {
        val sanitizedTurns = messages
            .filter { it.content.isNotBlank() }
            .takeLast(8)
            .map { "${if (it.role == "assistant") "Rem" else "User"}: ${it.content.trim().take(600)}" }

        if (sanitizedTurns.isEmpty()) return MemoryExtractResponseDto()

        val knownFactsText = if (existingFacts.isNotEmpty()) {
            existingFacts.take(25).joinToString("\n") { "- ${it.factKey}: ${it.factValue}" }
        } else {
            "(belum ada memori tersimpan)"
        }

        val systemPrompt = """
Kamu adalah modul ekstraksi memori personal untuk asisten virtual bernama Rem.
Tugasmu: baca potongan percakapan terbaru dan ekstrak HANYA fakta personal yang EKSPLISIT diungkapkan oleh User (bukan ucapan fiksi/roleplay Rem, bukan asumsi atau tebakan).

Kategori valid:
- identitas (nama, panggilan, pekerjaan, domisili, umur, perangkat)
- preferensi (kesukaan, ketidaksukaan, makanan/minuman, gaya bicara, musik, hobi)
- kebiasaan (rutinitas harian, jam tidur/bangun, pola kerja)
- tujuan (rencana, target belajar, proyek, cita-cita)
- catatan (fakta penting lain yang layak diingat jangka panjang)

Aturan ketat:
1. Jawab HANYA dengan objek JSON valid tanpa teks tambahan atau markdown.
2. Format JSON: {"facts":[{"category":"identitas|preferensi|kebiasaan|tujuan|catatan","factKey":"snake_case_singkat","factValue":"nilai ringkas bahasa Indonesia","confidence":0.85}],"summary":"ringkasan 1 kalimat singkat topik obrolan atau null"}
3. Jika User mengubah atau mengoreksi fakta yang sudah ada, gunakan factKey yang sama dengan factValue baru.
4. Jika tidak ada fakta personal baru yang layak disimpan dari percakapan ini, kembalikan {"facts":[],"summary":null}.
5. Jangan ulangi fakta yang nilainya persis sama dengan memori yang sudah ada.
        """.trimIndent()

        val userPayload = "Memori saat ini:\n$knownFactsText\n\nPercakapan terbaru:\n${sanitizedTurns.joinToString("\n")}"

        val rawContent = DirectLlmClient.complete(
            httpClient = httpClient,
            messages = listOf(
                ChatTurnDto("system", systemPrompt),
                ChatTurnDto("user", userPayload)
            ),
            temperature = 0.15,
            maxTokens = 450
        ).getOrNull() ?: return null

        val cleaned = DirectLlmClient.stripJsonFences(rawContent)
        val parsed = runCatching {
            json.decodeFromString<MemoryExtractResponseDto>(cleaned)
        }.getOrNull() ?: return null

        val normalizedFacts = parsed.facts
            .mapNotNull { f ->
                val normalizedKey = f.factKey
                    .trim()
                    .lowercase()
                    .replace(Regex("[^a-z0-9_]+"), "_")
                    .trim('_')
                    .take(48)
                val value = f.factValue.trim().take(180)
                if (normalizedKey.length < 2 || value.isEmpty()) return@mapNotNull null
                val catLower = f.category.trim().lowercase()
                val category = if (catLower in VALID_CATEGORIES) catLower else "catatan"
                val confidence = f.confidence.coerceIn(0.3f, 1.0f)
                ExtractedFactDto(
                    category = category,
                    factKey = normalizedKey,
                    factValue = value,
                    confidence = confidence
                )
            }
            .take(6)

        val summary = parsed.summary
            ?.trim()
            ?.takeIf { it.length > 4 && !it.equals("null", ignoreCase = true) }
            ?.take(220)

        return MemoryExtractResponseDto(facts = normalizedFacts, summary = summary)
    }

    companion object {
        private val VALID_CATEGORIES = setOf("identitas", "preferensi", "kebiasaan", "tujuan", "catatan")
    }
}
