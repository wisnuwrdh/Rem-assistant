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
data class ExistingKgNodeDto(
    val nodeKey: String,
    val nodeType: String,
    val label: String
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
        existingFacts: List<ExistingFactDto>,
        existingKgNodes: List<ExistingKgNodeDto> = emptyList()
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

        val knownKgText = if (existingKgNodes.isNotEmpty()) {
            existingKgNodes.take(35).joinToString("\n") { "- [${it.nodeType}] ${it.label} (${it.nodeKey})" }
        } else {
            "(belum ada simpul Otak Digital)"
        }

        val systemPrompt = """
Kamu adalah modul ekstraksi memori personal & Knowledge Graph untuk asisten virtual bernama Rem.
Tugasmu: baca potongan percakapan terbaru dan ekstrak HANYA informasi personal JANGKA PANJANG yang EKSPLISIT diungkapkan oleh User (bukan ucapan fiksi/roleplay Rem, bukan asumsi atau tebakan).

Kategori facts valid:
- identitas (nama, panggilan, pekerjaan, domisili, umur, perangkat)
- preferensi (kesukaan, ketidaksukaan, makanan/minuman, gaya bicara, musik, hobi)
- kebiasaan (rutinitas harian, jam tidur/bangun, pola kerja)
- tujuan (rencana, target belajar, proyek, cita-cita)
- catatan (prinsip/aturan pribadi atau fakta penting jangka panjang)

Tipe node triples valid:
SELF, NILAI, POLA_PIKIR, KEBIASAAN, TUJUAN, EMOSI_PEMICU, KEPUTUSAN, MINAT, ORANG, KONTEKS
Relasi triples valid:
MENGUTAMAKAN, MEMICU, MENYEBABKAN, MENURUNKAN, MENDUKUNG, MEMOTIVASI, BERTENTANGAN, CONTOH_DARI, CENDERUNG_SAAT, ALASANNYA, MENGHINDARI, TERCERMIN_DI, TERKAIT

Aturan ketat:
1. Jawab HANYA dengan objek JSON valid tanpa teks tambahan atau markdown.
2. Format JSON: {"facts":[{"category":"identitas|preferensi|kebiasaan|tujuan|catatan","factKey":"snake_case_singkat","factValue":"nilai ringkas bahasa Indonesia","confidence":0.85}],"deleteKeys":["fact_key_yang_diminta_dihapus"],"summary":"ringkasan 1 kalimat topik reflektif atau null","triples":[{"fromType":"NILAI|EMOSI_PEMICU|SELF|...","fromLabel":"label singkat","relation":"MENYEBABKAN|BERTENTANGAN|MEMOTIVASI|CONTOH_DARI|MENGUTAMAKAN|ALASANNYA|...","toType":"KEPUTUSAN|KEBIASAAN|POLA_PIKIR|...","toLabel":"label singkat","evidence":"alasan/konteks singkat dari chat","confidence":0.85,"replacesLabel":""}]}
3. LARANGAN KERAS (JANGAN PERNAH EKSTRAK ke facts, summary, maupun triples):
   - Status atau perasaan sesaat hari ini (mis. "hari ini uang aman", "pekerjaan hari ini selesai", "barusan capek").
   - Angka saldo akun, aset bersih, atau nominal transaksi sesaat (mis. "Dana Cash Rp526.500", "pemasukan Rp10.000").
   - Log aksi satuan yang sudah tersimpan di modul aplikasi (transaksi CuciMoney+, bookmark/URL LinkKu, entri Kronik, to-do/centang Daily OS, item satuan IdeKu).
4. Isi "summary" dengan null untuk sapaan singkat, obrolan ringan, atau perintah catat/cari/ubah/hapus rutin; isi "summary" HANYA jika ada diskusi reflektif, perencanaan, atau sesi Kenali Aku yang bermakna jangka panjang.
5. Isi "triples" (maks 4 item) HANYA untuk pola pikir, nilai hidup, keputusan jangka panjang beserta alasannya, pemicu emosi berulang, atau kebiasaan nyata — JANGAN membuat triples dari transaksi/kejadian sesaat. Jika tidak ada, isi "triples": [].
6. Prioritaskan koneksi ANTAR-SIMPUL non-SELF bila ada hubungan nyata (mis. EMOSI_PEMICU -> MENYEBABKAN -> KEPUTUSAN, NILAI -> BERTENTANGAN -> KEBIASAAN, NILAI -> MEMOTIVASI -> TUJUAN, KEPUTUSAN -> CONTOH_DARI -> POLA_PIKIR), jangan semuanya ditarik dari SELF.
7. PENCEGAHAN DUPLIKAT: Periksa daftar "Memori saat ini" dan "Simpul Otak Digital saat ini". Jika konsep yang disebut User bermakna sama atau hanya memperbarui fakta/simpul yang sudah ada, WAJIB gunakan "factKey" atau "nodeType"+"label" yang PERSIS SAMA (jangan membuat key sinonim baru).
8. Jika User menyatakan perubahan/evolusi dari pola lama ke pola baru (mis. dulu begadang, sekarang tidur jam 10; dulu fokus X, sekarang beralih ke Y), isi "replacesLabel" dengan label pola lama agar node lama ditandai historis.
9. Jika User secara eksplisit meminta menonaktifkan/melupakan info tertentu atau menyatakan suatu fakta lama tidak lagi berlaku, masukkan factKey yang cocok dari daftar Memori saat ini ke "deleteKeys". Jika tidak ada, isi "deleteKeys": [].
10. Jika tidak ada info jangka panjang baru yang layak disimpan dari percakapan ini, kembalikan {"facts":[],"deleteKeys":[],"summary":null,"triples":[]}.
11. Jangan ulangi fakta yang nilainya persis sama dengan memori yang sudah ada.
        """.trimIndent()

        val userPayload = "Memori saat ini:\n$knownFactsText\n\nSimpul Otak Digital saat ini:\n$knownKgText\n\nPercakapan terbaru:\n${sanitizedTurns.joinToString("\n")}"

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
                if (MemoryNoiseFilter.isNoisyFact(category, normalizedKey, value)) return@mapNotNull null
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
                val ev = sanitizePlainText(t.evidence).take(200)
                if (fromLbl.length < 2 || toLbl.length < 2) return@mapNotNull null
                if (MemoryNoiseFilter.isNoisyTriple(fromLbl, toLbl, ev)) return@mapNotNull null
                ExtractedTripleDto(
                    fromType = KnowledgeGraphHelpers.normalizeNodeType(t.fromType),
                    fromLabel = fromLbl,
                    relation = KnowledgeGraphHelpers.normalizeRelation(t.relation),
                    toType = KnowledgeGraphHelpers.normalizeNodeType(t.toType),
                    toLabel = toLbl,
                    evidence = ev,
                    confidence = t.confidence.coerceIn(0.3f, 1.0f),
                    replacesLabel = sanitizePlainText(t.replacesLabel).take(64)
                )
            }
            .take(4)

        val summary = parsed.summary
            ?.trim()
            ?.takeIf { it.length > 4 && !it.equals("null", ignoreCase = true) }
            ?.take(220)
            ?.takeUnless { MemoryNoiseFilter.isNoisySummary(it) }

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

internal object MemoryNoiseFilter {
    private val URL_OR_LINKKU_KEY_REGEX = Regex("""^(?:link|bookmark|tautan|url)_""")
    private val URL_OR_LINKKU_VAL_REGEX = Regex("""https?://|\bdi\s+linkku\b|\bmenyimpan\s+(?:link|tautan|bookmark)\b""")
    private val TRANSIENT_KEY_PREFIX_REGEX = Regex("""^(?:pemasukan|pengeluaran|transaksi|saldo|aset|dana_cash|dompet|pengingat|todo|tugas)_""")
    private val TRANSIENT_KEY_SUFFIX_REGEX = Regex("""_(?:hari_ini|today|tadi|barusan|saat_ini|malam_ini|pagi_ini|siang_ini|sore_ini)$""")
    private val RP_AMOUNT_REGEX = Regex("""\brp\.?\s*\d[\d.,]*""")
    private val FINANCE_TRANSIENT_WORDS_REGEX = Regex(
        """\b(?:pemasukan|pengeluaran|transaksi|kategori|ke\s+akun|dari\s+akun|saldo|aset\s+bersih|dana\s+cash|hari\s+ini|barusan|tadi|membeli|beli|bayar|dapat)\b"""
    )
    private val TODAY_EPHEMERAL_WORDS_REGEX = Regex(
        """\b(?:aman|selesai|belum|merasa|uang|uangnya|pekerjaan|tugas|capek|lelah|bete|senang|sedih|lancar)\b"""
    )
    private val MODULE_ACTION_LOG_REGEX = Regex(
        """\b(?:di\s+cucimoney|di\s+linkku|di\s+kronik|di\s+daily\s*os|di\s+ideku|mencatat\s+pemasukan|mencatat\s+pengeluaran|mendapat\s+pemasukan|menyimpan\s+link|menyimpan\s+catatan|menambahkan\s+to-?do|mencentang\s+habit)\b"""
    )
    private val REFLECTIVE_SUMMARY_KEEP_REGEX = Regex(
        """\b(?:kenali\s+aku|pola\s+pikir|prinsip|nilai|alasan|strategi|evaluasi|trade-off|tujuan\s+jangka\s+panjang|refleksi|kebiasaan)\b"""
    )
    private val ROUTINE_SUMMARY_REGEX = Regex(
        """\b(?:menambahkan\s+pemasukan|mencatat\s+pemasukan|mendapat\s+pemasukan|menambahkan\s+pengeluaran|mencatat\s+pengeluaran|menghapus\s+pengeluaran|membatalkan\s+pengeluaran|menyimpan\s+link|menyimpan\s+bookmark|menyapa\s+(?:pagi|siang|sore|malam)|pekerjaan\s+hari\s+ini\s+sudah\s+selesai|mengonfirmasi\s+pekerjaan\s+hari\s+ini|kondisi\s+keuangannya\s+hari\s+ini)\b"""
    )

    fun isNoisyFact(category: String, factKey: String, factValue: String): Boolean {
        val cat = category.trim().lowercase()
        val key = factKey.trim().lowercase()
        val valLower = factValue.trim().lowercase()
        if (valLower.isEmpty()) return true

        if (URL_OR_LINKKU_KEY_REGEX.containsMatchIn(key) || URL_OR_LINKKU_VAL_REGEX.containsMatchIn(valLower)) {
            return true
        }
        if (TRANSIENT_KEY_PREFIX_REGEX.containsMatchIn(key) || TRANSIENT_KEY_SUFFIX_REGEX.containsMatchIn(key)) {
            return true
        }
        if (RP_AMOUNT_REGEX.containsMatchIn(valLower) && FINANCE_TRANSIENT_WORDS_REGEX.containsMatchIn(valLower)) {
            return true
        }
        if (MODULE_ACTION_LOG_REGEX.containsMatchIn(valLower)) {
            return true
        }
        if (cat == "catatan" &&
            Regex("""\bhari\s+ini\b""").containsMatchIn(valLower) &&
            TODAY_EPHEMERAL_WORDS_REGEX.containsMatchIn(valLower)
        ) {
            return true
        }
        return false
    }

    fun isNoisySummary(summaryText: String): Boolean {
        val lower = summaryText.trim().lowercase()
        if (lower.length <= 8) return true
        if (REFLECTIVE_SUMMARY_KEEP_REGEX.containsMatchIn(lower)) return false
        return ROUTINE_SUMMARY_REGEX.containsMatchIn(lower)
    }

    fun isNoisyTriple(fromLabel: String, toLabel: String, evidence: String): Boolean {
        val combined = "$fromLabel $toLabel $evidence".lowercase()
        if (combined.contains("http://") || combined.contains("https://")) return true
        if (RP_AMOUNT_REGEX.containsMatchIn(combined) &&
            Regex("""\b(?:pemasukan|pengeluaran|transaksi|saldo|dana\s+cash|aset\s+bersih|hari\s+ini)\b""").containsMatchIn(combined)
        ) {
            return true
        }
        return false
    }
}

