package com.ecosystemku.assistant.data.network

import com.ecosystemku.assistant.BuildConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import okhttp3.OkHttpClient

@Serializable
data class DailyOsIdekuIdeaDto(
    val title: String,
    val category: String = "",
    val notes: String = ""
)

@Serializable
data class DailyOsCoachMemoryDto(
    val patterns: String = "",
    val weakSpots: String = "-",
    val strengths: String = "-",
    val coachInsight: String = "",
    val confidence: String = "medium",
    val lastUpdated: String = "",
    val habitCount: Int = 0
)

@Serializable
data class DailyOsParsedTodoDto(
    val judul: String = "",
    val deadline: String = "",
    val priority: String = "normal",
    val subtasks: List<String> = emptyList()
)

@Serializable
data class DailyOsRekomTodoDto(
    val judul: String = "",
    val deadline: String = "",
    val priority: String = "normal",
    val alasan: String = ""
)

sealed interface DailyOsAiResult<out T> {
    data class Success<T>(val data: T) : DailyOsAiResult<T>
    data class Failure(val message: String) : DailyOsAiResult<Nothing>
}

class DailyOsAiClient(
    private val httpClient: OkHttpClient,
    @Suppress("unused") private val baseUrl: String = BuildConfig.API_BASE_URL
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun mapFailureMessage(err: Throwable, fallback: String): String =
        when (err.message) {
            "quota" -> "Kuota AI habis, coba beberapa saat lagi."
            else -> err.message ?: fallback
        }

    private fun normalizePriority(p: String): String {
        val v = p.trim().lowercase()
        return when (v) {
            "urgent", "hari ini" -> "urgent"
            "soon", "minggu ini" -> "soon"
            else -> "normal"
        }
    }

    private fun normalizeDeadline(dl: String?): String {
        val s = dl?.trim().orEmpty()
        if (s.isEmpty() || s.equals("null", ignoreCase = true)) return ""
        return if (DATE_REGEX.matches(s)) s else ""
    }

    suspend fun getCoachBriefing(
        todayLabel: String,
        habitSummary: String,
        todoSummary: String,
        memory: DailyOsCoachMemoryDto?,
        idekuIdeas: List<DailyOsIdekuIdeaDto>
    ): DailyOsAiResult<String> {
        val safeHabitSummary = habitSummary.trim().ifEmpty { "(tidak ada habit dijadwalkan hari ini)" }
        val safeTodoSummary = todoSummary.trim().ifEmpty { "(tidak ada task aktif)" }

        val memorySection = if (memory != null && memory.patterns.isNotBlank()) {
            """
--- CATATAN MENTOR (pola 30 hari terakhir) ---
Pola umum: ${memory.patterns}
Titik lemah: ${memory.weakSpots.ifBlank { "-" }}
Yang sudah kuat: ${memory.strengths.ifBlank { "-" }}
Insight: ${memory.coachInsight.ifBlank { "-" }}
----------------------------------------------
            """.trimIndent().let { "\n$it" }
        } else ""

        val idekuSection = if (idekuIdeas.isNotEmpty()) {
            val lines = idekuIdeas.joinToString("\n") { idea ->
                val cat = if (idea.category.isNotBlank()) " [${idea.category}]" else ""
                val notes = if (idea.notes.isNotBlank()) ": ${idea.notes}" else ""
                "- ${idea.title}$cat$notes"
            }
            "\n\nIDE YANG SEDANG DIKERJAKAN USER (dari IdeKu):\n$lines\n(Gunakan ini sebagai konteks untuk rekomendasi yang lebih relevan dengan proyek/ide user.)"
        } else ""

        val systemPrompt =
            "Kamu adalah mentor produktivitas yang sudah kenal user ini. Bicara seperti teman yang jujur — singkat, langsung, kadang nyelekit kalau perlu. Bahasa Indonesia kasual, boleh campur English kalau natural. Hindari kalimat motivasi klise seperti \"aku percaya kamu bisa\", \"mari kita\", \"komitmen yang kuat\". Jangan terdengar seperti bot."

        val userPrompt = """
Hari ini $todayLabel.

HABIT TERJADWAL HARI INI:
$safeHabitSummary

TO-DO (dikelompokkan berdasarkan urgensi waktu):
$safeTodoSummary$memorySection$idekuSection

Tulis briefing harian — maksimal 2-3 paragraf pendek, padat, dan langsung ke intinya. Gaya: pesan WhatsApp dari mentor yang kenal kamu baik, bukan pidato motivasi. Kalau ada yang sudah selesai, sebut spesifik dan lanjut. Kalau ada yang belum dan kamu punya data polanya, tantang langsung — jangan basa-basi atau filosofis. Pilih 1-2 hal paling penting untuk disorot, jangan bahas semuanya. Jangan pakai bullet point. Jangan mulai dengan sapaan klise seperti "Hei!", "Wah!", "Oke,", atau "Jadi,". PENTING: todo yang berlabel [BESOK], [MINGGU INI], atau [NANTI] jangan disuruh dikerjakan hari ini — cukup sebut sebagai pengingat kalau perlu, bukan tuntutan.
        """.trimIndent()

        val res = DirectLlmClient.complete(
            httpClient = httpClient,
            messages = listOf(
                ChatTurnDto("system", systemPrompt),
                ChatTurnDto("user", userPrompt)
            ),
            temperature = 0.7,
            maxTokens = 600
        )

        return res.fold(
            onSuccess = { text ->
                if (text.isNotBlank()) DailyOsAiResult.Success(text)
                else DailyOsAiResult.Failure("Briefing AI kosong.")
            },
            onFailure = { DailyOsAiResult.Failure(mapFailureMessage(it, "Gagal menghubungi AI.")) }
        )
    }

    suspend fun analyzeCoachMemory(
        todayLabel: String,
        statsText: String
    ): DailyOsAiResult<DailyOsCoachMemoryDto> {
        val cleanStats = statsText.trim()
        if (cleanStats.isEmpty()) {
            return DailyOsAiResult.Failure("Data statistik kosong.")
        }

        val systemPrompt =
            "Kamu adalah analis data perilaku. Return HANYA JSON valid tanpa teks lain, tanpa backtick, tanpa markdown."
        val userPrompt = """
Analisis data habit 30 hari terakhir user ini dan buat catatan mentor yang berguna.

Tanggal analisis: $todayLabel

DATA HABIT:
$cleanStats

Return HANYA JSON valid tanpa penjelasan, tanpa backtick, tanpa markdown. Format:
{
  "patterns": "deskripsi pola konsistensi secara keseluruhan, habit mana yang kuat dan lemah",
  "weakSpots": "habit atau hari spesifik yang sering di-skip, pola yang perlu diperhatikan",
  "strengths": "apa yang sudah berjalan baik, streak atau konsistensi yang patut diapresiasi",
  "coachInsight": "insight untuk coach: cara terbaik mendekati user ini, apakah perlu di-push keras atau didukung, pola avoidance yang terdeteksi",
  "confidence": "high | medium | low (low kalau data kurang dari 7 hari)"
}
        """.trimIndent()

        val res = DirectLlmClient.complete(
            httpClient = httpClient,
            messages = listOf(
                ChatTurnDto("system", systemPrompt),
                ChatTurnDto("user", userPrompt)
            ),
            temperature = 0.3,
            maxTokens = 550
        )

        return res.fold(
            onSuccess = { raw ->
                val cleaned = DirectLlmClient.stripJsonFences(raw)
                val mem = runCatching { json.decodeFromString<DailyOsCoachMemoryDto>(cleaned) }.getOrNull()
                if (mem != null && mem.patterns.isNotBlank() && mem.coachInsight.isNotBlank()) {
                    DailyOsAiResult.Success(
                        mem.copy(
                            weakSpots = mem.weakSpots.ifBlank { "-" },
                            strengths = mem.strengths.ifBlank { "-" },
                            confidence = mem.confidence.ifBlank { "medium" }
                        )
                    )
                } else {
                    DailyOsAiResult.Failure("Data memory AI tidak lengkap.")
                }
            },
            onFailure = { DailyOsAiResult.Failure(mapFailureMessage(it, "Gagal menganalisis pola.")) }
        )
    }

    suspend fun parseSmartTodo(
        todayLabel: String,
        input: String
    ): DailyOsAiResult<List<DailyOsParsedTodoDto>> {
        val clean = input.trim()
        if (clean.isEmpty()) return DailyOsAiResult.Failure("Teks input kosong.")

        val systemPrompt =
            "Kamu adalah parser JSON. Return HANYA JSON array valid tanpa teks lain, tanpa backtick, tanpa markdown."
        val userPrompt = """
Ekstrak SEMUA task/todo dari teks berikut. Bisa 1 task, bisa banyak — tergantung isi teks.
Return HANYA JSON array tanpa penjelasan, tanpa backtick, tanpa markdown.

Hari ini: $todayLabel
Teks: "${clean.take(1500)}"

Format — array of objects:
[
  {
    "judul": "string max 60 karakter",
    "deadline": "YYYY-MM-DD atau null",
    "priority": "urgent | soon | normal",
    "subtasks": ["string"]
  }
]

Aturan judul:
- Judul HARUS mencakup detail spesifik dari teks: durasi (15 menit, 1 jam), target (5 lembar, 10 kali), atau konteks penting lainnya
- Contoh BENAR: "Latihan Reading English 15 Menit", "Baca Buku Psychology 5 Lembar", "Push Up 20 Kali"
- Contoh SALAH: "Latihan Reading", "Baca Buku", "Push Up" — terlalu umum, detail hilang
- Jangan potong informasi penting hanya untuk mempersingkat judul

Aturan priority:
- urgent = hari ini / besok / sekarang
- soon = 2–7 hari ke depan / minggu ini
- normal = lebih dari seminggu / tidak ada urgensi

Subtasks: max 3, isi hanya jika ada langkah persiapan jelas di teks.
Deadline: format YYYY-MM-DD, atau null jika tidak disebutkan.
Pisahkan setiap task/keinginan yang berbeda menjadi objek terpisah.
        """.trimIndent()

        val res = DirectLlmClient.complete(
            httpClient = httpClient,
            messages = listOf(
                ChatTurnDto("system", systemPrompt),
                ChatTurnDto("user", userPrompt)
            ),
            temperature = 0.25,
            maxTokens = 700
        )

        return res.fold(
            onSuccess = { raw ->
                val cleaned = DirectLlmClient.stripJsonFences(raw)
                val list = runCatching {
                    when (val elem = json.parseToJsonElement(cleaned)) {
                        is JsonArray -> elem.mapNotNull {
                            runCatching { json.decodeFromJsonElement(DailyOsParsedTodoDto.serializer(), it) }.getOrNull()
                        }
                        is JsonObject -> listOf(
                            json.decodeFromJsonElement(DailyOsParsedTodoDto.serializer(), elem)
                        )
                        else -> emptyList()
                    }
                }.getOrElse {
                    return DailyOsAiResult.Failure("Format respons AI tidak valid.")
                }

                val normalized = list
                    .filter { it.judul.isNotBlank() }
                    .map { t ->
                        t.copy(
                            judul = t.judul.trim().take(80),
                            deadline = normalizeDeadline(t.deadline),
                            priority = normalizePriority(t.priority),
                            subtasks = t.subtasks.map { it.trim() }.filter { it.isNotEmpty() }.take(3)
                        )
                    }
                DailyOsAiResult.Success(normalized)
            },
            onFailure = { DailyOsAiResult.Failure(mapFailureMessage(it, "Gagal memproses teks.")) }
        )
    }

    suspend fun getRecommendations(
        todayLabel: String,
        existingHabitsText: String,
        habitGapSummary: String,
        doneTodosText: String,
        activeTodosText: String,
        memory: DailyOsCoachMemoryDto?,
        idekuIdeas: List<DailyOsIdekuIdeaDto>
    ): DailyOsAiResult<List<DailyOsRekomTodoDto>> {
        val safeHabits = existingHabitsText.trim().ifEmpty { "(tidak ada)" }
        val safeGap = habitGapSummary.trim().ifEmpty { "(tidak ada habit hari ini)" }
        val safeDone = doneTodosText.trim().ifEmpty { "(belum ada)" }
        val safeActive = activeTodosText.trim().ifEmpty { "(tidak ada)" }

        val memorySection = if (memory != null && memory.patterns.isNotBlank()) {
            "\nPOLA KEBIASAAN USER (dari analisis 30 hari):\n${memory.patterns}\nTitik lemah: ${memory.weakSpots.ifBlank { "-" }}\nInsight: ${memory.coachInsight.ifBlank { "-" }}"
        } else ""

        val idekuSection = if (idekuIdeas.isNotEmpty()) {
            val lines = idekuIdeas.joinToString("\n") { idea ->
                val cat = if (idea.category.isNotBlank()) " [${idea.category}]" else ""
                val notes = if (idea.notes.isNotBlank()) ": ${idea.notes}" else ""
                "- ${idea.title}$cat$notes"
            }
            "\n\nIDE YANG SEDANG DIKERJAKAN USER (dari IdeKu — gunakan sebagai konteks rekomendasi yang lebih relevan):\n$lines"
        } else ""

        val systemPrompt =
            "Kamu adalah AI task recommender. Return HANYA JSON array valid tanpa teks lain, tanpa backtick, tanpa markdown."
        val userPrompt = """
Kamu adalah AI yang membantu user merekomendasikan task to-do yang relevan dan actionable.

Tanggal hari ini: $todayLabel

PRIORITAS KONTEKS:
1) Habit gaps dan progres habit hari ini
2) IdeKu
3) To-do aktif / selesai sebagai konteks, bukan sumber utama

HABIT YANG SUDAH ADA (jangan direkomendasikan sebagai to-do rutin):
$safeHabits

HABIT GAP HARI INI:
$safeGap

TO-DO YANG SUDAH SELESAI (konteks pekerjaan terakhir):
$safeDone

TO-DO YANG MASIH AKTIF (jangan direkomendasikan lagi):
$safeActive$memorySection$idekuSection

Tugas kamu: rekomendasikan task to-do yang BARU, SPESIFIK, dan ONE-OFF — bukan kegiatan rutin yang sudah ada di habit.

Fokus pada:
- Follow-up logis dari to-do yang baru selesai
- Task konkret yang mendukung goal jangka panjang dari to-do aktif
- Task yang muncul dari habit gap atau dari ide di IdeKu

JANGAN rekomendasikan:
- Apapun yang namanya mirip atau sama dengan habit yang sudah ada
- Task yang sudah ada di to-do aktif
- Task generik yang tidak spesifik ke konteks user ini

Buat maksimal 5 rekomendasi yang paling relevan. Kalau yang benar-benar cocok hanya 1-2, cukup segitu. Kalau tidak ada yang cocok, return array kosong [].

Return HANYA JSON array tanpa penjelasan, tanpa backtick, tanpa markdown:
[
  {
    "judul": "string max 60 karakter, spesifik dan actionable",
    "deadline": "YYYY-MM-DD atau null",
    "priority": "urgent | soon | normal",
    "alasan": "1 kalimat singkat kenapa task ini relevan sekarang"
  }
]
        """.trimIndent()

        val res = DirectLlmClient.complete(
            httpClient = httpClient,
            messages = listOf(
                ChatTurnDto("system", systemPrompt),
                ChatTurnDto("user", userPrompt)
            ),
            temperature = 0.5,
            maxTokens = 850
        )

        return res.fold(
            onSuccess = { raw ->
                val cleaned = DirectLlmClient.stripJsonFences(raw)
                val list = runCatching {
                    when (val elem = json.parseToJsonElement(cleaned)) {
                        is JsonArray -> elem.mapNotNull {
                            runCatching { json.decodeFromJsonElement(DailyOsRekomTodoDto.serializer(), it) }.getOrNull()
                        }
                        is JsonObject -> listOf(
                            json.decodeFromJsonElement(DailyOsRekomTodoDto.serializer(), elem)
                        )
                        else -> emptyList()
                    }
                }.getOrElse {
                    return DailyOsAiResult.Failure("Format respons AI tidak valid.")
                }

                val normalized = list
                    .filter { it.judul.isNotBlank() }
                    .take(5)
                    .map { t ->
                        t.copy(
                            judul = t.judul.trim().take(80),
                            deadline = normalizeDeadline(t.deadline),
                            priority = normalizePriority(t.priority),
                            alasan = t.alasan.trim()
                        )
                    }
                DailyOsAiResult.Success(normalized)
            },
            onFailure = { DailyOsAiResult.Failure(mapFailureMessage(it, "Gagal mendapatkan rekomendasi.")) }
        )
    }

    companion object {
        private val DATE_REGEX = Regex("""^\d{4}-\d{2}-\d{2}$""")
    }
}
