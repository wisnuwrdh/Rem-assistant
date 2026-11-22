package com.ecosystemku.assistant.ui.apps.kronik

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.ecosystemku.assistant.data.local.KronikBackupEntryDto
import com.ecosystemku.assistant.data.local.KronikBackupWrapperDto
import com.ecosystemku.assistant.data.local.KronikEntryEntity
import com.ecosystemku.assistant.data.local.KronikHabitLogDto
import com.ecosystemku.assistant.data.local.KronikMoodDto
import com.ecosystemku.assistant.data.network.KronikMoodDetectResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class KronikMood(
    val status: String,
    val svgKey: String,
    val tocAccentHex: Long
) {
    FOKUS("Fokus", "sword", 0xFF1A3A5C),
    LELAH("Lelah", "shield-crack", 0xFF3A3A3A),
    TERINSPIRASI("Terinspirasi", "star-glow", 0xFF3A2254),
    BERAT("Berat", "cloud-dark", 0xFF1A1A1A),
    STABIL("Stabil", "shield", 0xFF1A3A22),
    SEMANGAT("Semangat", "bolt", 0xFF3A3010),
    KABUR("Kabur", "spiral", 0xFF1E2A38);

    companion object {
        fun fromStatus(status: String?): KronikMood? {
            if (status.isNullOrBlank()) return null
            return entries.firstOrNull { it.status.equals(status.trim(), ignoreCase = true) }
        }
    }
}

data class KronikActions(
    val onCreateEntry: suspend () -> KronikEntryEntity,
    val onUpdateEntry: (id: String, title: String?, content: String?, date: String?) -> Unit = { _, _, _, _ -> },
    val onFlushEntry: (id: String, title: String, content: String, date: String) -> Unit = { _, _, _, _ -> },
    val onTogglePin: (id: String) -> Unit = {},
    val onArchiveEntry: (id: String) -> Unit = {},
    val onDeleteEntries: (Set<String>) -> Unit = {},
    val onSetMood: (id: String, mood: KronikMood?) -> Unit = { _, _ -> },
    val onDetectMoodAi: suspend (String) -> KronikMoodDetectResult = { KronikMoodDetectResult.Failure("Tidak tersedia") },
    val onExportJson: suspend () -> String = { "{}" },
    val onImportJson: suspend (String) -> Int = { 0 },
    val onResetKronik: () -> Unit = {}
)

object KronikHelpers {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    fun todayDateStr(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return fmt.format(Date())
    }

    fun nowIsoStr(): String = Instant.now().toString()

    fun generateId(): String {
        val ts = System.currentTimeMillis().toString(36)
        val rand = (1000..9999).random().toString(36)
        return "$ts$rand"
    }

    fun formatShortIdDate(dateStr: String): String {
        return runCatching {
            val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val d = inFmt.parse(dateStr) ?: return dateStr
            val outFmt = SimpleDateFormat("d MMM yyyy", Locale("in", "ID"))
            outFmt.format(d)
        }.getOrDefault(dateStr.ifBlank { "—" })
    }

    fun formatTimeFromIso(iso: String): String {
        return runCatching {
            val instant = Instant.parse(iso)
            val fmt = SimpleDateFormat("HH:mm", Locale("in", "ID"))
            fmt.format(Date.from(instant))
        }.getOrDefault("")
    }

    fun dateStrToUtcMillis(dateStr: String): Long {
        return runCatching {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            fmt.parse(dateStr)?.time ?: System.currentTimeMillis()
        }.getOrDefault(System.currentTimeMillis())
    }

    fun utcMillisToDateStr(millis: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return fmt.format(Date(millis))
    }

    fun parseHabitLogs(rawJson: String): List<KronikHabitLogDto> {
        if (rawJson.isBlank() || rawJson == "[]") return emptyList()
        return runCatching {
            json.decodeFromString<List<KronikHabitLogDto>>(rawJson)
        }.getOrDefault(emptyList())
    }

    fun encodeHabitLogs(list: List<KronikHabitLogDto>): String {
        return runCatching { json.encodeToString(list) }.getOrDefault("[]")
    }

    fun buildExportJson(entries: List<KronikEntryEntity>): String {
        val dtos = entries.mapIndexed { idx, e ->
            KronikBackupEntryDto(
                id = e.id,
                date = e.date,
                title = e.title,
                content = e.content,
                order = idx,
                mood = if (!e.moodStatus.isNullOrBlank() && !e.moodSvgKey.isNullOrBlank()) {
                    KronikMoodDto(e.moodStatus, e.moodSvgKey)
                } else {
                    null
                },
                habits_log = parseHabitLogs(e.habitsLogJson),
                source = e.source,
                generatedAt = e.generatedAt,
                createdAt = e.createdAt,
                updatedAt = e.updatedAt,
                completedAt = e.completedAt,
                archivedAt = e.archivedAt,
                isArchived = e.isArchived,
                pinned = e.pinned
            )
        }
        return json.encodeToString(
            KronikBackupWrapperDto(
                version = 2,
                entries = dtos,
                exportedAt = nowIsoStr()
            )
        )
    }

    fun parseImportEntries(raw: String): List<KronikEntryEntity> {
        val trimmed = raw.trim()
        val dtos: List<KronikBackupEntryDto> = if (trimmed.startsWith("[")) {
            json.decodeFromString(trimmed)
        } else {
            json.decodeFromString<KronikBackupWrapperDto>(trimmed).entries
        }
        val now = nowIsoStr()
        return dtos.mapIndexed { idx, d ->
            val resolvedMood = KronikMood.fromStatus(d.mood?.status)
            KronikEntryEntity(
                id = d.id.ifBlank { generateId() },
                date = d.date.ifBlank { todayDateStr() },
                title = d.title,
                content = d.content,
                sortOrder = if (d.order >= 0) d.order else idx,
                moodStatus = resolvedMood?.status ?: d.mood?.status,
                moodSvgKey = resolvedMood?.svgKey ?: d.mood?.svgKey,
                habitsLogJson = encodeHabitLogs(d.habits_log),
                source = d.source.ifBlank { "manual" },
                generatedAt = d.generatedAt,
                createdAt = d.createdAt.ifBlank { "${d.date.ifBlank { todayDateStr() }}T00:00:00.000Z" },
                updatedAt = now,
                completedAt = d.completedAt,
                archivedAt = d.archivedAt,
                isArchived = d.isArchived,
                pinned = d.pinned
            )
        }
    }
}

@Composable
fun KronikMoodIcon(
    mood: KronikMood?,
    tint: Color = Color(0xFFC9A84C),
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(
            width = (w * 0.085f).coerceAtLeast(1.8f),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        fun px(x: Float) = w * (x / 24f)
        fun py(y: Float) = h * (y / 24f)

        when (mood) {
            KronikMood.FOKUS -> {
                // Sword
                drawLine(tint, Offset(px(4f), py(20f)), Offset(px(20f), py(4f)), stroke.width, StrokeCap.Round)
                val p1 = Path().apply {
                    moveTo(px(4f), py(20f))
                    lineTo(px(8f), py(19f))
                    lineTo(px(9f), py(15f))
                }
                drawPath(p1, tint, style = stroke)
                val p2 = Path().apply {
                    moveTo(px(20f), py(4f))
                    lineTo(px(16f), py(5f))
                    lineTo(px(15f), py(9f))
                }
                drawPath(p2, tint, style = stroke)
                drawLine(tint, Offset(px(7f), py(13f)), Offset(px(11f), py(17f)), stroke.width, StrokeCap.Round)
            }
            KronikMood.LELAH -> {
                // Cracked shield
                val shield = Path().apply {
                    moveTo(px(12f), py(3f))
                    lineTo(px(4f), py(7f))
                    lineTo(px(4f), py(13f))
                    cubicTo(px(4f), py(17f), px(7.5f), py(20.5f), px(12f), py(22f))
                    cubicTo(px(16.5f), py(20.5f), px(20f), py(17f), px(20f), py(13f))
                    lineTo(px(20f), py(7f))
                    close()
                }
                drawPath(shield, tint, style = stroke)
                val crack = Path().apply {
                    moveTo(px(10f), py(10f))
                    lineTo(px(12f), py(13f))
                    lineTo(px(14f), py(11f))
                }
                drawPath(crack, tint, style = stroke)
            }
            KronikMood.TERINSPIRASI -> {
                // Star glow
                val star = Path()
                val cx = w * 0.5f
                val cy = h * 0.54f
                val outerR = w * 0.42f
                val innerR = w * 0.19f
                for (i in 0 until 10) {
                    val r = if (i % 2 == 0) outerR else innerR
                    val angle = (-90.0 + i * 36.0) * PI / 180.0
                    val x = cx + (r * cos(angle)).toFloat()
                    val y = cy + (r * sin(angle)).toFloat()
                    if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
                }
                star.close()
                drawPath(star, tint, style = stroke)
                drawLine(tint, Offset(px(19.5f), py(4f)), Offset(px(21.2f), py(2.3f)), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(px(4.5f), py(4f)), Offset(px(2.8f), py(2.3f)), stroke.width, StrokeCap.Round)
            }
            KronikMood.BERAT -> {
                // Dark cloud + rain
                val cloud = Path().apply {
                    moveTo(px(6.5f), py(16.5f))
                    lineTo(px(17.5f), py(16.5f))
                    cubicTo(px(20f), py(16.5f), px(21f), py(14.5f), px(21f), py(12.5f))
                    cubicTo(px(21f), py(10.5f), px(19.2f), py(9f), px(17.2f), py(9f))
                    cubicTo(px(16.8f), py(6f), px(14.3f), py(4f), px(11.5f), py(4f))
                    cubicTo(px(8.5f), py(4f), px(6.5f), py(6.2f), px(6f), py(9f))
                    cubicTo(px(4f), py(9.5f), px(3f), py(11.2f), px(3f), py(13f))
                    cubicTo(px(3f), py(15f), px(4.5f), py(16.5f), px(6.5f), py(16.5f))
                }
                drawPath(cloud, tint, style = stroke)
                drawLine(tint, Offset(px(8.5f), py(19.5f)), Offset(px(8.5f), py(21.5f)), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(px(12f), py(19.5f)), Offset(px(12f), py(21.5f)), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(px(15.5f), py(19.5f)), Offset(px(15.5f), py(21.5f)), stroke.width, StrokeCap.Round)
            }
            KronikMood.STABIL -> {
                // Shield + check
                val shield = Path().apply {
                    moveTo(px(12f), py(3f))
                    lineTo(px(4f), py(7f))
                    lineTo(px(4f), py(13f))
                    cubicTo(px(4f), py(17f), px(7.5f), py(20.5f), px(12f), py(22f))
                    cubicTo(px(16.5f), py(20.5f), px(20f), py(17f), px(20f), py(13f))
                    lineTo(px(20f), py(7f))
                    close()
                }
                drawPath(shield, tint, style = stroke)
                val check = Path().apply {
                    moveTo(px(9f), py(12.2f))
                    lineTo(px(11.2f), py(14.4f))
                    lineTo(px(15.3f), py(10f))
                }
                drawPath(check, tint, style = stroke)
            }
            KronikMood.SEMANGAT -> {
                // Bolt
                val bolt = Path().apply {
                    moveTo(px(13f), py(2f))
                    lineTo(px(4f), py(14f))
                    lineTo(px(12f), py(14f))
                    lineTo(px(11f), py(22f))
                    lineTo(px(20f), py(10f))
                    lineTo(px(12f), py(10f))
                    close()
                }
                drawPath(bolt, tint, style = stroke)
            }
            KronikMood.KABUR -> {
                // Spiral
                val spiral = Path()
                val cx = w * 0.5f
                val cy = h * 0.5f
                val turns = 2.4f
                val steps = 42
                for (i in 0..steps) {
                    val t = i.toFloat() / steps
                    val angle = t * turns * 2f * PI.toFloat()
                    val r = t * (w * 0.38f)
                    val x = cx + r * cos(angle)
                    val y = cy + r * sin(angle)
                    if (i == 0) spiral.moveTo(x, y) else spiral.lineTo(x, y)
                }
                drawPath(spiral, tint, style = stroke)
            }
            null -> {
                // Neutral circle
                drawCircle(tint.copy(alpha = 0.35f), radius = w * 0.36f, style = stroke)
                drawCircle(tint.copy(alpha = 0.85f), radius = w * 0.16f, style = stroke)
            }
        }
    }
}
