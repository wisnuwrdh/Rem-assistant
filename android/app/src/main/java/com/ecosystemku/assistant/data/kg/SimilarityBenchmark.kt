package com.ecosystemku.assistant.data.kg

import com.ecosystemku.assistant.data.local.KgEdgeEntity
import com.ecosystemku.assistant.data.local.KgNodeEntity
import com.ecosystemku.assistant.data.local.UserMemoryEntity
import com.ecosystemku.assistant.data.network.ChatTurnDto
import com.ecosystemku.assistant.data.network.DirectLlmClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

data class SimilarityScenarioDto(
    val id: String,
    val domain: String,
    val situation: String,
    val relevantTypes: List<String>,
    val expectedSignals: List<String>
)

data class SimilarityScenarioResult(
    val scenario: SimilarityScenarioDto,
    val offlineCoveragePercent: Int,
    val matchedSignals: List<String>,
    val missingHints: List<String>,
    val predictedBehavior: String,
    val llmConfidencePercent: Int? = null,
    val rationale: String = ""
)

data class SimilarityRunReport(
    val timestamp: Long,
    val mode: String, // "offline" | "live_llm"
    val totalScenarios: Int,
    val overallScorePercent: Int,
    val domainScores: Map<String, Int>,
    val results: List<SimilarityScenarioResult>,
    val summaryText: String
)

@Serializable
private data class LlmBenchmarkItemDto(
    val id: String = "",
    val prediksi: String = "",
    val keyakinan: Int = 0,
    val alasan: String = ""
)

object SimilarityBenchmark {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 25 skenario "Kalau aku di situasi X, apa yang bakal kulakukan?"
     * mencakup 5 domain kepribadian utama agar konsistensi kembaran digital
     * bisa diukur dan dibandingkan dari waktu ke waktu.
     */
    val SCENARIOS: List<SimilarityScenarioDto> = listOf(
        // 1. Keuangan (5)
        SimilarityScenarioDto(
            id = "fin_1",
            domain = "Keuangan",
            situation = "Tiba-tiba dapat rezeki/bonus tak terduga Rp 5 juta hari ini, apa langkah pertamaku?",
            relevantTypes = listOf("NILAI", "KEBIASAAN", "TUJUAN"),
            expectedSignals = listOf("tabung", "wishlist", "hutang", "dana", "investasi", "uang", "keuangan")
        ),
        SimilarityScenarioDto(
            id = "fin_2",
            domain = "Keuangan",
            situation = "Ada barang impian diskon besar malam ini tapi bujet bulan ini lagi mepet. Apa keputusanku?",
            relevantTypes = listOf("NILAI", "POLA_PIKIR", "KEBIASAAN"),
            expectedSignals = listOf("tahan", "tunda", "impulsif", "wishlist", "bujet", "uang", "prioritas")
        ),
        SimilarityScenarioDto(
            id = "fin_3",
            domain = "Keuangan",
            situation = "Pengeluaran bulan ini ternyata sudah melewati target di minggu ketiga. Bagaimana reaksiku?",
            relevantTypes = listOf("KEBIASAAN", "POLA_PIKIR", "PERASAAN"),
            expectedSignals = listOf("catat", "rem", "hemat", "cemas", "evaluasi", "cuci", "pengeluaran")
        ),
        SimilarityScenarioDto(
            id = "fin_4",
            domain = "Keuangan",
            situation = "Teman dekat mengajak patungan proyek atau nongkrong mahal saat kondisi keuangan sedang pas-pasan.",
            relevantTypes = listOf("NILAI", "RELASI", "POLA_PIKIR"),
            expectedSignals = listOf("jujur", "tolak", "sungkan", "prioritas", "teman", "uang", "batas")
        ),
        SimilarityScenarioDto(
            id = "fin_5",
            domain = "Keuangan",
            situation = "Harus memilih antara upgrade alat kerja/produktivitas atau menyimpan seluruh uang sebagai cadangan aman.",
            relevantTypes = listOf("NILAI", "TUJUAN", "POLA_PIKIR"),
            expectedSignals = listOf("aman", "risiko", "alat", "kerja", "kualitas", "tabungan", "tujuan")
        ),

        // 2. Produktivitas (5)
        SimilarityScenarioDto(
            id = "prod_1",
            domain = "Produktivitas",
            situation = "Bangun tidur sudah kesiangan dan jadwal pagi berantakan. Bagaimana sisa hariku berjalan?",
            relevantTypes = listOf("KEBIASAAN", "POLA_PIKIR", "PERASAAN"),
            expectedSignals = listOf("ulang", "reset", "fokus", "menunda", "lelah", "target", "habit")
        ),
        SimilarityScenarioDto(
            id = "prod_2",
            domain = "Produktivitas",
            situation = "Ada 5 tugas menumpuk dengan deadline mepet minggu ini. Mana yang kukerjakan duluan?",
            relevantTypes = listOf("POLA_PIKIR", "KEBIASAAN", "TUJUAN"),
            expectedSignals = listOf("urgent", "kecil", "sulit", "prioritas", "sistem", "daftar", "todo")
        ),
        SimilarityScenarioDto(
            id = "prod_3",
            domain = "Produktivitas",
            situation = "Proyek pribadi sudah berjalan 70% lalu mendadak terasa membosankan dan muncul ide baru yang lebih seru.",
            relevantTypes = listOf("POLA_PIKIR", "KEBIASAAN", "MINAT"),
            expectedSignals = listOf("selesai", "bosan", "ide", "fokus", "loncat", "disiplin", "proyek")
        ),
        SimilarityScenarioDto(
            id = "prod_4",
            domain = "Produktivitas",
            situation = "Sudah mencoba fokus 15 menit tapi pikiran terus terdistraksi notifikasi atau hal sepele.",
            relevantTypes = listOf("KEBIASAAN", "PERASAAN", "POLA_PIKIR"),
            expectedSignals = listOf("jeda", "pomodoro", "timer", "musik", "paksa", "istirahat", "fokus")
        ),
        SimilarityScenarioDto(
            id = "prod_5",
            domain = "Produktivitas",
            situation = "Punya waktu luang 3 jam tanpa gangguan di akhir pekan. Biasanya kuisi dengan apa?",
            relevantTypes = listOf("MINAT", "KEBIASAAN", "TUJUAN"),
            expectedSignals = listOf("ngoding", "belajar", "istirahat", "proyek", "game", "tidur", "hobi")
        ),

        // 3. Nilai & Keputusan (5)
        SimilarityScenarioDto(
            id = "val_1",
            domain = "Nilai & Keputusan",
            situation = "Ditawari jalan pintas yang hasilnya cepat dan menguntungkan, tapi bertentangan dengan prinsip pribadiku.",
            relevantTypes = listOf("NILAI", "POLA_PIKIR"),
            expectedSignals = listOf("prinsip", "jujur", "tolak", "integritas", "jangka", "amanah", "nilai")
        ),
        SimilarityScenarioDto(
            id = "val_2",
            domain = "Nilai & Keputusan",
            situation = "Dihadapkan pada dua pilihan hidup besar yang sama-sama berisiko tanpa jaminan pasti.",
            relevantTypes = listOf("NILAI", "POLA_PIKIR", "TUJUAN"),
            expectedSignals = listOf("data", "riset", "intuisi", "risiko", "keluarga", "tujuan", "tenang")
        ),
        SimilarityScenarioDto(
            id = "val_3",
            domain = "Nilai & Keputusan",
            situation = "Menerima tawaran kerja/proyek bergaji tinggi tapi menuntut waktu keluarga dan istirahat hampir habis.",
            relevantTypes = listOf("NILAI", "RELASI", "TUJUAN"),
            expectedSignals = listOf("keluarga", "seimbang", "uang", "bebas", "kesehatan", "waktu", "kompromi")
        ),
        SimilarityScenarioDto(
            id = "val_4",
            domain = "Nilai & Keputusan",
            situation = "Menemukan bukti kuat bahwa keyakinan atau rencanaku selama beberapa bulan terakhir ternyata keliru.",
            relevantTypes = listOf("POLA_PIKIR", "NILAI"),
            expectedSignals = listOf("akui", "koreksi", "terbuka", "evaluasi", "ego", "ubah", "belajar")
        ),
        SimilarityScenarioDto(
            id = "val_5",
            domain = "Nilai & Keputusan",
            situation = "Diminta menolong seseorang yang sedang kesulitan di saat energiku sendiri tinggal sedikit.",
            relevantTypes = listOf("NILAI", "RELASI", "PERASAAN"),
            expectedSignals = listOf("bantu", "batas", "empati", "sungkan", "prioritas", "kapasitas", "lelah")
        ),

        // 4. Emosi & Relasi (5)
        SimilarityScenarioDto(
            id = "emo_1",
            domain = "Emosi & Relasi",
            situation = "Sedang merasa sangat berat, kecewa, atau burnout setelah gagal mencapai target penting.",
            relevantTypes = listOf("PERASAAN", "KEBIASAAN", "POLA_PIKIR"),
            expectedSignals = listOf("diam", "curhat", "nulis", "kronik", "tidur", "sendiri", "emosi")
        ),
        SimilarityScenarioDto(
            id = "emo_2",
            domain = "Emosi & Relasi",
            situation = "Terjadi salah paham atau konflik dengan orang terdekat karena perbedaan pendapat.",
            relevantTypes = listOf("RELASI", "NILAI", "PERASAAN"),
            expectedSignals = listOf("tenang", "bicara", "diam", "dengar", "maaf", "jujur", "emosi")
        ),
        SimilarityScenarioDto(
            id = "emo_3",
            domain = "Emosi & Relasi",
            situation = "Hasil kerja keras mendapat kritik tajam atau meremehkan di depan orang lain.",
            relevantTypes = listOf("PERASAAN", "POLA_PIKIR", "NILAI"),
            expectedSignals = listOf("saring", "defensif", "diam", "perbaiki", "tersinggung", "logika", "bukti")
        ),
        SimilarityScenarioDto(
            id = "emo_4",
            domain = "Emosi & Relasi",
            situation = "Melihat pencapaian teman atau rekan sebaya melesat jauh lebih cepat dariku.",
            relevantTypes = listOf("POLA_PIKIR", "PERASAAN", "TUJUAN"),
            expectedSignals = listOf("iri", "terpacu", "banding", "proses", "jalur", "fokus", "syukur")
        ),
        SimilarityScenarioDto(
            id = "emo_5",
            domain = "Emosi & Relasi",
            situation = "Merasa kesepian atau jenuh di malam hari setelah seharian penuh beraktivitas.",
            relevantTypes = listOf("KEBIASAAN", "MINAT", "PERASAAN"),
            expectedSignals = listOf("musik", "nonton", "ngobrol", "jurnal", "tidur", "scroll", "tenang")
        ),

        // 5. Gaya Kerja (5)
        SimilarityScenarioDto(
            id = "work_1",
            domain = "Gaya Kerja",
            situation = "Diminta mempelajari skill atau teknologi baru dari nol dengan dokumentasi yang berantakan.",
            relevantTypes = listOf("POLA_PIKIR", "MINAT", "KEBIASAAN"),
            expectedSignals = listOf("praktik", "coba", "pecah", "eksperimen", "baca", "alur", "belajar")
        ),
        SimilarityScenarioDto(
            id = "work_2",
            domain = "Gaya Kerja",
            situation = "Bekerja dalam tim di mana satu anggota lambat merespons padahal deadline sudah dekat.",
            relevantTypes = listOf("RELASI", "POLA_PIKIR", "NILAI"),
            expectedSignals = listOf("ambil", "ingatkan", "tanya", "mandiri", "kesal", "rapat", "tanggung")
        ),
        SimilarityScenarioDto(
            id = "work_3",
            domain = "Gaya Kerja",
            situation = "Masalah teknis/bug muncul berulang kali dan belum ketemu akar penyebabnya setelah berjam-jam.",
            relevantTypes = listOf("POLA_PIKIR", "PERASAAN", "KEBIASAAN"),
            expectedSignals = listOf("jeda", "log", "sistematis", "pecah", "tanya", "keras", "akar")
        ),
        SimilarityScenarioDto(
            id = "work_4",
            domain = "Gaya Kerja",
            situation = "Diminta memilih antara kerja mandiri dengan kendali penuh atau kerja tim dengan arahan ketat.",
            relevantTypes = listOf("NILAI", "POLA_PIKIR", "RELASI"),
            expectedSignals = listOf("mandiri", "otonomi", "kendali", "bebas", "tim", "struktur", "arahan")
        ),
        SimilarityScenarioDto(
            id = "work_5",
            domain = "Gaya Kerja",
            situation = "Menghadapi tugas rutin administratif yang membosankan tapi wajib beres hari ini.",
            relevantTypes = listOf("KEBIASAAN", "POLA_PIKIR", "PERASAAN"),
            expectedSignals = listOf("tumpuk", "pagi", "otomatis", "cepat", "sabar", "daftar", "selesai")
        )
    )

    fun evaluateOfflineCoverage(
        nodes: List<KgNodeEntity>,
        edges: List<KgEdgeEntity>,
        memories: List<UserMemoryEntity> = emptyList(),
        now: Long = System.currentTimeMillis()
    ): SimilarityRunReport {
        val activeNodes = nodes.filter {
            KnowledgeGraphHelpers.isActiveNode(it) && it.nodeType != "SELF"
        }
        val activeNodeIds = activeNodes.map { it.id }.toSet()
        val activeEdges = edges.filter {
            KnowledgeGraphHelpers.isActiveEdge(it) &&
                it.fromNodeId in activeNodeIds &&
                it.toNodeId in activeNodeIds
        }
        val nonSelfConnectedNodeIds = activeEdges
            .flatMap { listOf(it.fromNodeId, it.toNodeId) }
            .toSet()

        val activeMemories = memories.filter { it.status == "active" }
        val corpusTokens = buildSet {
            activeNodes.forEach { node ->
                addAll(KnowledgeGraphHelpers.semanticTokens("${node.label} ${node.summary}"))
            }
            edges.filter { KnowledgeGraphHelpers.isActiveEdge(it) }.forEach { edge ->
                addAll(KnowledgeGraphHelpers.semanticTokens(edge.evidence))
            }
            activeMemories.forEach { mem ->
                addAll(KnowledgeGraphHelpers.semanticTokens("${mem.factKey} ${mem.factValue}"))
            }
        }

        val activeTypes = activeNodes.map { it.nodeType }.toSet()

        val results = SCENARIOS.map { scenario ->
            val coveredTypes = scenario.relevantTypes.count { it in activeTypes }
            val typeRatio = coveredTypes.toFloat() / scenario.relevantTypes.size.coerceAtLeast(1)
            val typeScore = (typeRatio * 45f)

            val policyNodesForScenario = activeNodes.count {
                it.nodeType in scenario.relevantTypes && it.id in nonSelfConnectedNodeIds
            }
            val policyRatio = (policyNodesForScenario.toFloat() / 2f).coerceIn(0f, 1f)
            val policyScore = policyRatio * 25f

            val matched = scenario.expectedSignals.filter { sig ->
                val sigTokens = KnowledgeGraphHelpers.semanticTokens(sig)
                sig.lowercase() in corpusTokens || sigTokens.any { it in corpusTokens }
            }
            val signalRatio = (matched.size.toFloat() / 2f).coerceIn(0f, 1f)
            val signalScore = signalRatio * 30f

            val coverage = (typeScore + policyScore + signalScore).toInt().coerceIn(0, 100)
            val missingTypes = scenario.relevantTypes.filterNot { it in activeTypes }
            val missingHints = buildList {
                if (missingTypes.isNotEmpty()) {
                    add("Tambah simpul: ${missingTypes.joinToString(", ")}")
                }
                if (policyNodesForScenario == 0 && activeNodes.isNotEmpty()) {
                    add("Hubungkan simpul non-SELF (${scenario.relevantTypes.firstOrNull().orEmpty()})")
                }
                if (matched.isEmpty()) {
                    add("Belum ada sinyal topik ${scenario.domain.lowercase()}")
                }
            }

            val topMatchingNodes = activeNodes
                .filter { it.nodeType in scenario.relevantTypes }
                .sortedByDescending { it.salience * it.confidence }
                .take(2)
                .map { "${it.nodeType}: ${it.label}" }

            val predictedBehavior = if (topMatchingNodes.isEmpty()) {
                "Data otak digital untuk domain ${scenario.domain} masih minim."
            } else {
                "Merujuk ke ${topMatchingNodes.joinToString("; ")}"
            }

            SimilarityScenarioResult(
                scenario = scenario,
                offlineCoveragePercent = coverage,
                matchedSignals = matched,
                missingHints = missingHints,
                predictedBehavior = predictedBehavior,
                llmConfidencePercent = null,
                rationale = if (missingHints.isEmpty()) {
                    "Cakupan sinyal graph sudah kuat."
                } else {
                    missingHints.joinToString(" | ")
                }
            )
        }

        return assembleReport(
            timestamp = now,
            mode = "offline",
            results = results
        )
    }

    suspend fun evaluateWithLlm(
        httpClient: OkHttpClient,
        profileName: String,
        nodes: List<KgNodeEntity>,
        edges: List<KgEdgeEntity>,
        memories: List<UserMemoryEntity>
    ): Result<SimilarityRunReport> = withContext(Dispatchers.IO) {
        val offlineReport = evaluateOfflineCoverage(nodes, edges, memories)
        val activeNodes = nodes.filter { KnowledgeGraphHelpers.isActiveNode(it) }
        val activeNodeById = activeNodes.associateBy { it.id }
        val activeEdges = edges.filter { KnowledgeGraphHelpers.isActiveEdge(it) }

        val topNodesSummary = activeNodes
            .filter { it.nodeType != "SELF" }
            .sortedByDescending { it.salience * it.confidence }
            .take(35)
            .joinToString("\n") { n ->
                "- [${n.nodeType}] ${n.label} (conf=${(n.confidence * 100).toInt()}%, validasi=${n.validationState})"
            }
            .ifBlank { "- (belum ada simpul non-SELF aktif)" }

        val topEdgesSummary = activeEdges
            .sortedByDescending { it.weight * it.confidence }
            .take(35)
            .mapNotNull { e ->
                val from = activeNodeById[e.fromNodeId] ?: return@mapNotNull null
                val to = activeNodeById[e.toNodeId] ?: return@mapNotNull null
                "- ${from.label} --${e.relation}--> ${to.label}${if (e.evidence.isNotBlank()) " (${e.evidence.take(70)})" else ""}"
            }
            .joinToString("\n")
            .ifBlank { "- (belum ada relasi aktif)" }

        val memorySummary = memories
            .filter { it.status == "active" }
            .sortedByDescending { it.updatedAt }
            .take(25)
            .joinToString("\n") { "- [${it.category}] ${it.factKey}: ${it.factValue}" }
            .ifBlank { "- (kosong)" }

        val scenariosText = SCENARIOS.joinToString("\n") { s ->
            "${s.id} [${s.domain}]: ${s.situation}"
        }

        val systemPrompt = """
            Kamu mengevaluasi kelengkapan & ketepatan "kembaran digital" untuk pengguna bernama $profileName berdasarkan Otak Digital dan Memori Personalnya.
            Aturan KETAT:
            1. Jawab HANYA berdasarkan fakta pada Otak Digital & Memori di bawah. Jangan mengarang fakta yang tidak ada buktinya.
            2. Untuk setiap skenario, tulis:
               - "prediksi": 1 kalimat singkat perkiraan langkah pengguna
               - "keyakinan": angka 0-100 berdasarkan SEBERAPA KUAT BUKTI NYATA di data (jika bukti tidak ada atau hanya tebakan umum, WAJIB isi 15-40; jika ada bukti jelas di nilai/kebiasaan/relasi, isi 65-95)
               - "alasan": sebut simpul/memori yang dipakai atau celah data yang kurang (maks 15 kata)
            3. Kembalikan HANYA JSON array valid tanpa markdown:
               [{"id":"fin_1","prediksi":"...","keyakinan":75,"alasan":"..."}]
        """.trimIndent()

        val userPrompt = """
            ## Simpul Aktif Otak Digital
            $topNodesSummary

            ## Relasi Aktif Otak Digital
            $topEdgesSummary

            ## Memori Aktif
            $memorySummary

            ## 25 Skenario Evaluasi
            $scenariosText
        """.trimIndent()

        val completionRes = DirectLlmClient.complete(
            httpClient = httpClient,
            messages = listOf(
                ChatTurnDto(role = "system", content = systemPrompt),
                ChatTurnDto(role = "user", content = userPrompt)
            ),
            temperature = 0.2,
            maxTokens = 2600
        )

        val raw = completionRes.getOrElse { err ->
            return@withContext Result.failure(err)
        }

        val cleaned = DirectLlmClient.stripJsonFences(raw)
        val parsedItems = runCatching {
            json.decodeFromString<List<LlmBenchmarkItemDto>>(cleaned)
        }.getOrElse { err ->
            return@withContext Result.failure(
                IllegalStateException("Gagal membaca JSON hasil uji kemiripan: ${err.message}")
            )
        }

        val byId = parsedItems.associateBy { it.id.trim() }
        val mergedResults = offlineReport.results.map { base ->
            val llm = byId[base.scenario.id]
            if (llm != null) {
                val conf = llm.keyakinan.coerceIn(0, 100)
                base.copy(
                    predictedBehavior = llm.prediksi.trim().ifBlank { base.predictedBehavior },
                    llmConfidencePercent = conf,
                    rationale = llm.alasan.trim().ifBlank { base.rationale }
                )
            } else {
                base.copy(llmConfidencePercent = base.offlineCoveragePercent)
            }
        }

        Result.success(
            assembleReport(
                timestamp = System.currentTimeMillis(),
                mode = "live_llm",
                results = mergedResults
            )
        )
    }

    private fun assembleReport(
        timestamp: Long,
        mode: String,
        results: List<SimilarityScenarioResult>
    ): SimilarityRunReport {
        val scoreFn: (SimilarityScenarioResult) -> Int = { r ->
            if (mode == "live_llm") r.llmConfidencePercent ?: r.offlineCoveragePercent
            else r.offlineCoveragePercent
        }
        val overall = if (results.isEmpty()) 0 else {
            (results.sumOf(scoreFn).toFloat() / results.size.toFloat()).toInt().coerceIn(0, 100)
        }
        val domainScores = results
            .groupBy { it.scenario.domain }
            .mapValues { (_, items) ->
                (items.sumOf(scoreFn).toFloat() / items.size.coerceAtLeast(1).toFloat())
                    .toInt()
                    .coerceIn(0, 100)
            }

        val weakestDomain = domainScores.minByOrNull { it.value }
        val modeLabel = if (mode == "live_llm") "Uji LLM + Graph" else "Cakupan Graph (Offline)"
        val summaryText = buildString {
            append("$modeLabel: $overall% (${results.size} skenario)")
            if (weakestDomain != null) {
                append(" • Terlemah: ${weakestDomain.key} (${weakestDomain.value}%)")
            }
        }

        return SimilarityRunReport(
            timestamp = timestamp,
            mode = mode,
            totalScenarios = results.size,
            overallScorePercent = overall,
            domainScores = domainScores,
            results = results,
            summaryText = summaryText
        )
    }
}
