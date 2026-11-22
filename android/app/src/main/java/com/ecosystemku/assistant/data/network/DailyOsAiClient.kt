package com.ecosystemku.assistant.data.network

import com.ecosystemku.assistant.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

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

@Serializable
private data class DailyOsCoachReqDto(
    val mode: String = "coach",
    val todayLabel: String,
    val habitSummary: String,
    val todoSummary: String,
    val memory: DailyOsCoachMemoryDto? = null,
    val idekuIdeas: List<DailyOsIdekuIdeaDto> = emptyList()
)

@Serializable
private data class DailyOsMemoryReqDto(
    val mode: String = "memory",
    val todayLabel: String,
    val statsText: String
)

@Serializable
private data class DailyOsParseTodoReqDto(
    val mode: String = "parse_todo",
    val todayLabel: String,
    val input: String
)

@Serializable
private data class DailyOsRekomReqDto(
    val mode: String = "recommend_todo",
    val todayLabel: String,
    val existingHabitsText: String,
    val habitGapSummary: String,
    val doneTodosText: String,
    val activeTodosText: String,
    val memory: DailyOsCoachMemoryDto? = null,
    val idekuIdeas: List<DailyOsIdekuIdeaDto> = emptyList()
)

@Serializable
private data class DailyOsRespDto(
    val text: String? = null,
    val memory: DailyOsCoachMemoryDto? = null,
    val tasks: List<DailyOsParsedTodoDto>? = null,
    val recommendations: List<DailyOsRekomTodoDto>? = null,
    val error: String? = null
)

sealed interface DailyOsAiResult<out T> {
    data class Success<T>(val data: T) : DailyOsAiResult<T>
    data class Failure(val message: String) : DailyOsAiResult<Nothing>
}

class DailyOsAiClient(
    private val httpClient: OkHttpClient,
    private val baseUrl: String = BuildConfig.API_BASE_URL
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private inline fun <reified Req> postDailyOs(payload: Req): Result<DailyOsRespDto> {
        return runCatching {
            val bodyJson = json.encodeToString(payload)
            val request = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/api/dailyos")
                .post(bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            httpClient.newCall(request).execute().use { resp ->
                val raw = resp.body?.string()
                val parsed = raw?.let { runCatching { json.decodeFromString<DailyOsRespDto>(it) }.getOrNull() }
                if (!resp.isSuccessful) {
                    val msg = when (parsed?.error) {
                        "quota" -> "Kuota AI habis, coba beberapa saat lagi."
                        else -> parsed?.error ?: "Server AI mengembalikan kode ${resp.code}."
                    }
                    error(msg)
                }
                parsed ?: error("Respons AI kosong.")
            }
        }
    }

    suspend fun getCoachBriefing(
        todayLabel: String,
        habitSummary: String,
        todoSummary: String,
        memory: DailyOsCoachMemoryDto?,
        idekuIdeas: List<DailyOsIdekuIdeaDto>
    ): DailyOsAiResult<String> = withContext(Dispatchers.IO) {
        val res = postDailyOs(
            DailyOsCoachReqDto(
                todayLabel = todayLabel,
                habitSummary = habitSummary,
                todoSummary = todoSummary,
                memory = memory,
                idekuIdeas = idekuIdeas
            )
        )
        res.fold(
            onSuccess = { dto ->
                val text = dto.text?.trim().orEmpty()
                if (text.isNotEmpty()) DailyOsAiResult.Success(text)
                else DailyOsAiResult.Failure("Briefing AI kosong.")
            },
            onFailure = { DailyOsAiResult.Failure(it.message ?: "Gagal menghubungi AI.") }
        )
    }

    suspend fun analyzeCoachMemory(
        todayLabel: String,
        statsText: String
    ): DailyOsAiResult<DailyOsCoachMemoryDto> = withContext(Dispatchers.IO) {
        val res = postDailyOs(
            DailyOsMemoryReqDto(
                todayLabel = todayLabel,
                statsText = statsText
            )
        )
        res.fold(
            onSuccess = { dto ->
                val mem = dto.memory
                if (mem != null && mem.patterns.isNotBlank()) DailyOsAiResult.Success(mem)
                else DailyOsAiResult.Failure("Data memory tidak lengkap.")
            },
            onFailure = { DailyOsAiResult.Failure(it.message ?: "Gagal menganalisis pola.") }
        )
    }

    suspend fun parseSmartTodo(
        todayLabel: String,
        input: String
    ): DailyOsAiResult<List<DailyOsParsedTodoDto>> = withContext(Dispatchers.IO) {
        val clean = input.trim()
        if (clean.isEmpty()) return@withContext DailyOsAiResult.Failure("Teks input kosong.")
        val res = postDailyOs(
            DailyOsParseTodoReqDto(
                todayLabel = todayLabel,
                input = clean
            )
        )
        res.fold(
            onSuccess = { dto ->
                DailyOsAiResult.Success(dto.tasks.orEmpty())
            },
            onFailure = { DailyOsAiResult.Failure(it.message ?: "Gagal memproses teks.") }
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
    ): DailyOsAiResult<List<DailyOsRekomTodoDto>> = withContext(Dispatchers.IO) {
        val res = postDailyOs(
            DailyOsRekomReqDto(
                todayLabel = todayLabel,
                existingHabitsText = existingHabitsText,
                habitGapSummary = habitGapSummary,
                doneTodosText = doneTodosText,
                activeTodosText = activeTodosText,
                memory = memory,
                idekuIdeas = idekuIdeas
            )
        )
        res.fold(
            onSuccess = { dto ->
                DailyOsAiResult.Success(dto.recommendations.orEmpty())
            },
            onFailure = { DailyOsAiResult.Failure(it.message ?: "Gagal mendapatkan rekomendasi.") }
        )
    }
}
