package com.ecosystemku.assistant.data.network

import com.ecosystemku.assistant.BuildConfig
import com.ecosystemku.assistant.data.kg.ExtractedTripleDto
import com.ecosystemku.assistant.data.kg.KnowledgeGraphHelpers
import com.ecosystemku.assistant.data.local.ConversationSummaryEntity
import com.ecosystemku.assistant.data.local.UserMemoryEntity
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
    val deleteKeys: List<String> = emptyList(),
    val summary: String? = null,
    val triples: List<ExtractedTripleDto> = emptyList()
)

@Serializable
data class MemoryProfileSectionDto(
    val title: String = "",
    val body: String = ""
)

@Serializable
data class MemoryProfileData(
    val sections: List<MemoryProfileSectionDto> = emptyList(),
    val updatedAt: Long = 0L,
    val manuallyEdited: Boolean = false
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
            existingFacts.take(35).joinToString("\n") { "- ${it.factKey}: ${it.factValue}" }
        } else {
            "(belum ada memori tersimpan)"
        }

        val systemPrompt = """
Kamu adalah modul ekstraksi memori personal & Knowledge Graph untuk asisten virtual bernama Rem.
Tugasmu: baca potongan percakapan terbaru dan ekstrak HANYA informasi personal yang EKSPLISIT diungkapkan oleh User (bukan ucapan fiksi/roleplay Rem, bukan asumsi atau tebakan).

Kategori facts valid:
- identitas (nama, panggilan, pekerjaan, domisili, umur, perangkat)
- preferensi (kesukaan, ketidaksukaan, makanan/minuman, gaya bicara, musik, hobi)
- kebiasaan (rutinitas harian, jam tidur/bangun, pola kerja)
- tujuan (rencana, target belajar, proyek, cita-cita)
- catatan (fakta penting lain yang layak diingat jangka panjang)

Tipe node triples valid:
SELF, NILAI, POLA_PIKIR, KEBIASAAN, TUJUAN, EMOSI_PEMICU, KEPUTUSAN, MINAT, ORANG, KONTEKS
Relasi triples valid:
MENGUTAMAKAN, MEMICU, MENURUNKAN, MENDUKUNG, CENDERUNG_SAAT, ALASANNYA, MENGHINDARI, TERCERMIN_DI, TERKAIT

Aturan ketat:
1. Jawab HANYA dengan objek JSON valid tanpa teks tambahan atau markdown.
2. Format JSON: {"facts":[{"category":"identitas|preferensi|kebiasaan|tujuan|catatan","factKey":"snake_case_singkat","factValue":"nilai ringkas bahasa Indonesia","confidence":0.85}],"deleteKeys":["fact_key_yang_diminta_dihapus"],"summary":"ringkasan 1 kalimat singkat topik obrolan atau null","triples":[{"fromType":"SELF|...","fromLabel":"Tuan|label singkat","relation":"MENGUTAMAKAN|ALASANNYA|...","toType":"NILAI|POLA_PIKIR|...","toLabel":"label singkat","evidence":"alasan/konteks singkat dari chat","confidence":0.85,"replacesLabel":""}]}
3. Isi "triples" (maks 4 item) HANYA untuk pola pikir, nilai hidup, keputusan beserta alasannya, pemicu emosi, atau kaitan kebiasaan nyata. Jika tidak ada, isi "triples": [].
4. Jika User menyatakan perubahan/evolusi dari pola lama ke pola baru (mis. dulu begadang, sekarang tidur jam 10; dulu fokus X, sekarang beralih ke Y), isi "replacesLabel" dengan label pola lama agar node lama ditandai historis.
5. Jika User mengubah atau mengoreksi fakta yang sudah ada, gunakan factKey yang sama dengan factValue baru.
6. Jika User secara eksplisit meminta melupakan/menghapus info tertentu atau menyatakan suatu fakta lama tidak lagi berlaku dan harus dihapus, masukkan factKey yang cocok dari daftar Memori saat ini ke "deleteKeys". Jika tidak ada yang dihapus, isi "deleteKeys": [].
7. Jika tidak ada info baru yang layak disimpan dari percakapan ini, kembalikan {"facts":[],"deleteKeys":[],"summary":null,"triples":[]}.
8. Jangan ulangi fakta yang nilainya persis sama dengan memori yang sudah ada.
        """.trimIndent()

        val userPayload = "Memori saat ini:\n$knownFactsText\n\nPercakapan terbaru:\n${sanitizedTurns.joinToString("\n")}"

        val rawContent = DirectLlmClient.complete(
            httpClient = httpClient,
            messages = listOf(
                ChatTurnDto("system", systemPrompt),
                ChatTurnDto("user", userPayload)
            ),
            temperature = 0.15,
            maxTokens = 750
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

        val normalizedDeleteKeys = parsed.deleteKeys
            .map {
                it.trim()
                    .lowercase()
                    .replace(Regex("[^a-z0-9_]+"), "_")
                    .trim('_')
                    .take(48)
            }
            .filter { it.length >= 2 }
            .distinct()
            .take(8)

        val normalizedTriples = parsed.triples
            .mapNotNull { t ->
                val fromLbl = sanitizePlainText(t.fromLabel).take(64)
                val toLbl = sanitizePlainText(t.toLabel).take(64)
                if (fromLbl.length < 2 || toLbl.length < 2) return@mapNotNull null
                ExtractedTripleDto(
                    fromType = KnowledgeGraphHelpers.normalizeNodeType(t.fromType),
                    fromLabel = fromLbl,
                    relation = KnowledgeGraphHelpers.normalizeRelation(t.relation),
                    toType = KnowledgeGraphHelpers.normalizeNodeType(t.toType),
                    toLabel = toLbl,
                    evidence = sanitizePlainText(t.evidence).take(200),
                    confidence = t.confidence.coerceIn(0.3f, 1.0f),
                    replacesLabel = sanitizePlainText(t.replacesLabel).take(64)
                )
            }
            .take(4)

        val summary = parsed.summary
            ?.trim()
            ?.takeIf { it.length > 4 && !it.equals("null", ignoreCase = true) }
            ?.take(220)

        return MemoryExtractResponseDto(
            facts = normalizedFacts,
            deleteKeys = normalizedDeleteKeys,
            summary = summary,
            triples = normalizedTriples
        )
    }

    suspend fun synthesizeProfileSummary(
        memories: List<UserMemoryEntity>,
        summaries: List<ConversationSummaryEntity>
    ): MemoryProfileData? {
        if (memories.isEmpty() && summaries.isEmpty()) {
            return MemoryProfileData(sections = emptyList(), updatedAt = 0L)
        }

        val factsText = if (memories.isNotEmpty()) {
            memories.take(60).joinToString("\n") { "- [${it.category}] ${it.factKey}: ${it.factValue}" }
        } else "(belum ada fakta)"

        val sumText = if (summaries.isNotEmpty()) {
            summaries.take(10).joinToString("\n") { "- ${it.summaryText}" }
        } else "(tidak ada)"

        val systemPrompt = """
Kamu adalah modul penyusun Ringkasan Memori personal untuk asisten virtual bernama Rem.
Tugasmu: rangkum seluruh fakta personal pengguna menjadi dokumen profil naratif yang rapi, hangat, dan mudah dibaca (mirip halaman Ringkasan Memori).

Aturan ketat:
1. Jawab HANYA dengan objek JSON valid tanpa teks tambahan atau markdown:
   {"sections":[{"title":"Judul Bagian","body":"Paragraf naratif bahasa Indonesia"}]}
2. Gunakan sudut pandang orang kedua ("Kamu...", "Fokusmu...", "Saat ini kamu...").
3. Buat 1 sampai 4 bagian yang paling relevan dengan data nyata (misalnya "Gambaran Umum", "Karier & Tujuan", "Kebiasaan & Preferensi", "Catatan Penting"). Jangan mengarang bagian kalau datanya tidak ada.
4. Setiap body berupa 1 paragraf padat dan natural (2-5 kalimat), teks biasa tanpa markdown (**tebal**, *miring*, bullet) dan tanpa tanda em dash (—).
5. Jangan pernah mengarang fakta di luar data yang diberikan.
        """.trimIndent()

        val userPayload = "Fakta personal pengguna:\n$factsText\n\nRangkuman sesi percakapan:\n$sumText"

        val rawContent = DirectLlmClient.complete(
            httpClient = httpClient,
            messages = listOf(
                ChatTurnDto("system", systemPrompt),
                ChatTurnDto("user", userPayload)
            ),
            temperature = 0.25,
            maxTokens = 900
        ).getOrNull() ?: return null

        val cleaned = DirectLlmClient.stripJsonFences(rawContent)
        val parsed = runCatching {
            json.decodeFromString<MemoryProfileData>(cleaned)
        }.getOrNull() ?: return null

        val sections = parsed.sections
            .mapNotNull { s ->
                val title = sanitizePlainText(s.title).take(60)
                val body = sanitizePlainText(s.body).take(1200)
                if (title.isBlank() || body.isBlank()) null
                else MemoryProfileSectionDto(title = title, body = body)
            }
            .take(5)

        return MemoryProfileData(
            sections = sections,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun compactSessionMessages(
        previousSummary: String?,
        messages: List<ChatTurnDto>
    ): String? {
        val turns = messages
            .filter { it.content.isNotBlank() }
            .take(24)
            .map { "${if (it.role == "assistant") "Rem" else "Tuan"}: ${it.content.trim().take(500)}" }

        if (turns.isEmpty()) return previousSummary?.let(::sanitizePlainText)

        val prevBlock = previousSummary
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { "Ringkasan konteks sebelumnya:\n$it\n\n" }
            .orEmpty()

        val systemPrompt = """
Kamu adalah modul peringkas konteks obrolan (rolling context compaction) untuk asisten virtual bernama Rem.
Tugasmu: gabungkan ringkasan sebelumnya (jika ada) dengan potongan percakapan lama berikut menjadi satu ringkasan padat agar konteks obrolan panjang tetap nyambung.

Aturan ketat:
1. Jawab HANYA teks ringkasan biasa bahasa Indonesia (tanpa JSON, tanpa markdown **tebal**/*miring*/bullet, tanpa tanda em dash —).
2. Pertahankan topik utama, keputusan penting, angka/nama spesifik, pertanyaan terbuka, dan janji/rencana yang disebut.
3. Panjang 3 sampai 6 kalimat ringkas (maksimal 650 karakter).
4. Jangan mengarang fakta di luar isi percakapan.
        """.trimIndent()

        val userPayload = "${prevBlock}Potongan percakapan yang perlu dipadatkan:\n${turns.joinToString("\n")}"

        val rawContent = DirectLlmClient.complete(
            httpClient = httpClient,
            messages = listOf(
                ChatTurnDto("system", systemPrompt),
                ChatTurnDto("user", userPayload)
            ),
            temperature = 0.2,
            maxTokens = 450
        ).getOrNull() ?: return null

        val cleaned = sanitizePlainText(rawContent).take(700)
        return cleaned.takeIf { it.length >= 8 }
    }

    private fun sanitizePlainText(text: String): String {
        return text
            .replace("**", "")
            .replace("__", "")
            .replace(Regex("""\*([^*\r\n]+)\*"""), "$1")
            .replace(Regex("""`([^`\r\n]+)`"""), "$1")
            .replace(Regex("""(^|\n)\s*\u2014\s*"""), "$1")
            .replace(Regex("""([.!?,:;(\[])\s*\u2014\s*"""), "$1 ")
            .replace(Regex("""\s*\u2014\s*"""), ", ")
            .trim()
    }

    companion object {
        private val VALID_CATEGORIES = setOf("identitas", "preferensi", "kebiasaan", "tujuan", "catatan")
    }
}
