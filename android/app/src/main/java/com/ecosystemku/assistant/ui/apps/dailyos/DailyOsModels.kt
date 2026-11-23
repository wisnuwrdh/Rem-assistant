package com.ecosystemku.assistant.ui.apps.dailyos

import android.util.LruCache
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.NightlightRound
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.ecosystemku.assistant.data.local.DailyOsHabitEntity
import com.ecosystemku.assistant.data.local.DailyOsTodoEntity
import com.ecosystemku.assistant.data.network.DailyOsAiResult
import com.ecosystemku.assistant.data.network.DailyOsCoachMemoryDto
import com.ecosystemku.assistant.data.network.DailyOsParsedTodoDto
import com.ecosystemku.assistant.data.network.DailyOsRekomTodoDto
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

enum class DailyOsTab { HABIT, TODO, STATS, AI, SETTINGS }

@Serializable
data class DailyOsSubTask(
    val id: String,
    val text: String
)

@Serializable
data class DailyOsHistoryLog(
    val id: String,
    val text: String = "",
    val completedAt: String = ""
)

@Serializable
data class DailyOsTodoSubtask(
    val id: String,
    val text: String,
    val done: Boolean = false
)

@Serializable
data class DailyOsHabitBackupDto(
    val id: String? = null,
    val name: String = "",
    val category: String = "General",
    val color: String = "#22C55E",
    val icon: String = "star",
    val activeDays: List<Int> = emptyList(),
    val subTasks: List<DailyOsSubTask> = emptyList(),
    val history: Map<String, JsonElement> = emptyMap(),
    val collapsed: Boolean = false,
    val archived: Boolean = false,
    val createdAt: String? = null
)

@Serializable
data class DailyOsTodoBackupDto(
    val id: String = "",
    val title: String = "",
    val priority: String = "normal",
    val deadline: String = "",
    val done: Boolean = false,
    val completedAt: String? = null,
    val subtasks: List<DailyOsTodoSubtask> = emptyList()
)

@Serializable
data class DailyOsBackupWrapperDto(
    val version: Int = 3,
    val habits: List<DailyOsHabitBackupDto> = emptyList(),
    val todos: List<DailyOsTodoBackupDto> = emptyList(),
    val totalPoints: Int = 0,
    val coachMemory: DailyOsCoachMemoryDto? = null,
    val exportedAt: String = ""
)

data class DailyOsUiState(
    val habits: List<DailyOsHabitEntity> = emptyList(),
    val todos: List<DailyOsTodoEntity> = emptyList(),
    val coachMemory: DailyOsCoachMemoryDto? = null,
    val memoryDate: String = "",
    val notifHabitTime: String = "07:00",
    val notifTodoTime: String = "15:00",
    val isDarkTheme: Boolean = false,
    val doneSectionOpen: Boolean = true
)

data class DailyOsActions(
    val onSaveHabit: suspend (
        editId: String?,
        name: String,
        category: String,
        color: String,
        icon: String,
        activeDays: List<Int>,
        subTasks: List<DailyOsSubTask>
    ) -> Unit,
    val onDeleteHabit: suspend (habitId: String) -> Unit,
    val onToggleHabitCheck: suspend (habitId: String, subId: String) -> Unit,
    val onToggleHabitCollapse: suspend (habitId: String) -> Unit,
    val onMoveHabit: suspend (habitId: String, delta: Int) -> Unit,
    val onSaveTodo: suspend (
        editId: String?,
        title: String,
        priority: String,
        deadline: String,
        subtaskTexts: List<String>
    ) -> Unit,
    val onToggleTodo: suspend (todoId: String) -> Unit,
    val onToggleTodoSubtask: suspend (todoId: String, subId: String) -> Unit,
    val onDeleteTodo: suspend (todoId: String) -> Unit,
    val onMoveTodo: suspend (todoId: String, delta: Int) -> Unit,
    val onToggleDoneSectionOpen: () -> Unit,
    val onToggleTheme: () -> Unit,
    val onClearDayHistory: suspend (dateStr: String) -> Int,
    val onSaveNotifTimes: (habitTime: String, todoTime: String) -> Unit,
    val onGetAiCoach: suspend () -> DailyOsAiResult<String>,
    val onParseSmartTodo: suspend (input: String) -> DailyOsAiResult<List<DailyOsParsedTodoDto>>,
    val onAddParsedTodos: suspend (tasks: List<DailyOsParsedTodoDto>) -> Unit,
    val onGetAiRecommendations: suspend () -> DailyOsAiResult<List<DailyOsRekomTodoDto>>,
    val onAddRecommendedTodos: suspend (tasks: List<DailyOsRekomTodoDto>) -> Unit,
    val onBuildExportJson: suspend () -> String,
    val onImportJson: suspend (raw: String) -> Result<Pair<Int, Int>>,
    val onExportCsv: suspend () -> String?,
    val onBuildSummaryMarkdown: suspend (kind: String, offset: Int) -> Pair<String, String>,
    val onResetAll: suspend () -> Unit
)

data class DailyOsIconDef(
    val key: String,
    val label: String,
    val icon: ImageVector
)

data class DailyOsHabit30Stat(
    val name: String,
    val category: String,
    val scheduledCount: Int,
    val completedCount: Int,
    val skipCount: Int,
    val completionRate: Int,
    val currentStreak: Int,
    val weakDays: List<String>
)

data class DailyOsThemePalette(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val border: Color,
    val text: Color,
    val text2: Color,
    val text3: Color,
    val accent: Color,
    val accentFg: Color,
    val urgent: Color,
    val urgentBg: Color,
    val soon: Color,
    val soonBg: Color,
    val normal: Color,
    val normalBg: Color,
    val xp: Color,
    val xpBg: Color,
    val aiBg: Color,
    val aiBorder: Color,
    val aiAccent: Color
)

object DailyOsHelpers {
    val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    val CATEGORY_PRESETS = listOf(
        "General", "Kesehatan", "Olahraga", "Produktivitas",
        "Karir & Bisnis", "Belajar", "Kreativitas", "Sosial",
        "Keuangan", "Spiritual"
    )

    val HABIT_COLORS = listOf(
        "#FFFFFF", "#1A1917", "#C0392B", "#E8735A",
        "#F59E0B", "#EAB308", "#22C55E", "#2C7A7B",
        "#3B82F6", "#6366F1", "#6B4FBB", "#A855F7",
        "#EC4899", "#F472B6", "#7C6F4A", "#64748B"
    )

    val HABIT_ICONS = listOf(
        DailyOsIconDef("star", "Default", Icons.Outlined.Star),
        DailyOsIconDef("dumbbell", "Olahraga", Icons.Outlined.FitnessCenter),
        DailyOsIconDef("book", "Baca", Icons.Outlined.Book),
        DailyOsIconDef("briefcase", "Kerja", Icons.Outlined.WorkOutline),
        DailyOsIconDef("film", "Hiburan", Icons.Outlined.Movie),
        DailyOsIconDef("apple", "Makan/Diet", Icons.Outlined.Restaurant),
        DailyOsIconDef("moon", "Tidur", Icons.Outlined.NightlightRound),
        DailyOsIconDef("feather", "Menulis", Icons.Outlined.Create),
        DailyOsIconDef("dollar", "Keuangan", Icons.Outlined.AttachMoney),
        DailyOsIconDef("gamepad", "Gaming", Icons.Outlined.SportsEsports),
        DailyOsIconDef("walk", "Jalan", Icons.AutoMirrored.Outlined.DirectionsRun),
        DailyOsIconDef("heart", "Kesehatan", Icons.Outlined.FavoriteBorder),
        DailyOsIconDef("music", "Musik", Icons.Outlined.MusicNote),
        DailyOsIconDef("coffee", "Kopi", Icons.Outlined.Coffee),
        DailyOsIconDef("sun", "Pagi", Icons.Outlined.WbSunny),
        DailyOsIconDef("target", "Target", Icons.Outlined.TrackChanges),
        DailyOsIconDef("zap", "Energi", Icons.Outlined.Bolt),
        DailyOsIconDef("droplet", "Minum Air", Icons.Outlined.WaterDrop),
        DailyOsIconDef("globe", "Bahasa", Icons.Outlined.Public),
        DailyOsIconDef("smile", "Mood", Icons.Outlined.SentimentSatisfied),
        DailyOsIconDef("phone", "Detox", Icons.Outlined.Smartphone),
        DailyOsIconDef("flame", "Semangat", Icons.Outlined.LocalFireDepartment),
        DailyOsIconDef("pencil", "Belajar", Icons.Outlined.Edit),
        DailyOsIconDef("camera", "Foto/Vlog", Icons.Outlined.CameraAlt)
    )

    val DAY_LABELS = listOf(
        1 to "Sen",
        2 to "Sel",
        3 to "Rab",
        4 to "Kam",
        5 to "Jum",
        6 to "Sab",
        0 to "Min"
    )

    private val DAY_NAMES_SHORT = arrayOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")
    private val DAY_NAMES_FULL = arrayOf("Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")
    private val ID_LOCALE = Locale("in", "ID")

    fun palette(isDark: Boolean): DailyOsThemePalette = if (isDark) {
        DailyOsThemePalette(
            isDark = true,
            bg = Color(0xFF0F0F0F),
            surface = Color(0xFF1A1A1A),
            surface2 = Color(0xFF222222),
            border = Color(0xFF2A2A2A),
            text = Color(0xFFF0F0F0),
            text2 = Color(0xFFA0A0A0),
            text3 = Color(0xFF606060),
            accent = Color(0xFFF0F0F0),
            accentFg = Color(0xFF0F0F0F),
            urgent = Color(0xFFE05A4A),
            urgentBg = Color(0xFF1F1210),
            soon = Color(0xFFD4922A),
            soonBg = Color(0xFF1A1508),
            normal = Color(0xFF3D9B9C),
            normalBg = Color(0xFF0A1A1A),
            xp = Color(0xFFE0E0E0),
            xpBg = Color(0xFF1E1E1E),
            aiBg = Color(0xFF0A150A),
            aiBorder = Color(0xFF1E3A1F),
            aiAccent = Color(0xFF5BA85E)
        )
    } else {
        DailyOsThemePalette(
            isDark = false,
            bg = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF),
            surface2 = Color(0xFFF5F5F5),
            border = Color(0xFFEBEBEB),
            text = Color(0xFF1A1917),
            text2 = Color(0xFF6B6A65),
            text3 = Color(0xFFA8A7A2),
            accent = Color(0xFF1A1917),
            accentFg = Color(0xFFFFFFFF),
            urgent = Color(0xFFC0392B),
            urgentBg = Color(0xFFFDF2F0),
            soon = Color(0xFFB7770D),
            soonBg = Color(0xFFFEF9EC),
            normal = Color(0xFF2C7A7B),
            normalBg = Color(0xFFF0F8F8),
            xp = Color(0xFF555555),
            xpBg = Color(0xFFF5F5F5),
            aiBg = Color(0xFFF0F7F0),
            aiBorder = Color(0xFFC8E0C9),
            aiAccent = Color(0xFF2C5F2E)
        )
    }

    fun iconFor(key: String?): ImageVector {
        return HABIT_ICONS.firstOrNull { it.key == key }?.icon ?: Icons.Outlined.Star
    }

    fun iconLabel(key: String?): String {
        return HABIT_ICONS.firstOrNull { it.key == key }?.label ?: "Default"
    }

    fun parseColor(hex: String?): Color {
        val clean = hex.orEmpty().trim().removePrefix("#")
        return runCatching {
            when (clean.length) {
                6 -> Color(("FF$clean").toLong(16))
                8 -> Color(clean.toLong(16))
                else -> Color(0xFF22C55E)
            }
        }.getOrDefault(Color(0xFF22C55E))
    }

    fun isDarkColor(hex: String?): Boolean {
        val clean = hex.orEmpty().trim().removePrefix("#")
        if (clean.length < 6) return false
        return runCatching {
            val r = clean.substring(0, 2).toInt(16)
            val g = clean.substring(2, 4).toInt(16)
            val b = clean.substring(4, 6).toInt(16)
            (0.299 * r + 0.587 * g + 0.114 * b) < 80
        }.getOrDefault(false)
    }

    fun isLightColor(hex: String?): Boolean {
        val clean = hex.orEmpty().trim().removePrefix("#")
        if (clean.length < 6) return false
        return runCatching {
            val r = clean.substring(0, 2).toInt(16)
            val g = clean.substring(2, 4).toInt(16)
            val b = clean.substring(4, 6).toInt(16)
            (0.299 * r + 0.587 * g + 0.114 * b) > 200
        }.getOrDefault(false)
    }

    private val fmtTopDate = DateTimeFormatter.ofPattern("EEEE, d MMMM", ID_LOCALE)
    private val fmtFullDate = DateTimeFormatter.ofPattern("d MMMM yyyy", ID_LOCALE)
    private val fmtShortDate = DateTimeFormatter.ofPattern("d MMM", ID_LOCALE)

    private val activeDaysCache = LruCache<String, List<Int>>(64)
    private val subTasksCache = LruCache<String, List<DailyOsSubTask>>(64)
    private val historyCache = LruCache<String, Map<String, List<DailyOsHistoryLog>>>(128)
    private val todoSubtasksCache = LruCache<String, List<DailyOsTodoSubtask>>(64)

    fun todayStr(): String = LocalDate.now().toString()

    fun tomorrowStr(): String = LocalDate.now().plusDays(1).toString()

    fun formatTopDate(date: LocalDate = LocalDate.now()): String {
        return date.format(fmtTopDate).replaceFirstChar { it.uppercase(ID_LOCALE) }
    }

    fun formatFullDate(dateStr: String): String {
        return runCatching {
            val d = LocalDate.parse(dateStr)
            val dow = DAY_NAMES_FULL[dayOfWeekNum(d)]
            "$dow, ${d.format(fmtFullDate)}"
        }.getOrDefault(dateStr)
    }

    fun formatShortDate(dateStr: String): String {
        if (dateStr.isBlank()) return ""
        return runCatching {
            LocalDate.parse(dateStr).format(fmtShortDate)
        }.getOrDefault(dateStr)
    }

    fun dayOfWeekNum(d: LocalDate): Int {
        // 0=Minggu .. 6=Sabtu matching JS Date.getDay()
        return if (d.dayOfWeek == DayOfWeek.SUNDAY) 0 else d.dayOfWeek.value
    }

    fun dayShortLabel(dayNum: Int): String = DAY_NAMES_SHORT.getOrElse(dayNum) { "Min" }

    fun isOverdue(deadline: String, today: String = todayStr()): Boolean {
        if (deadline.isBlank()) return false
        return deadline < today
    }

    fun parseActiveDays(raw: String): List<Int> {
        if (raw.isBlank() || raw == "[]") return emptyList()
        activeDaysCache.get(raw)?.let { return it }
        val parsed = runCatching {
            json.decodeFromString<List<Int>>(raw).filter { it in 0..6 }.distinct()
        }.getOrDefault(emptyList())
        activeDaysCache.put(raw, parsed)
        return parsed
    }

    fun encodeActiveDays(days: List<Int>): String = json.encodeToString(days.filter { it in 0..6 }.distinct())

    fun parseSubTasks(raw: String): List<DailyOsSubTask> {
        if (raw.isBlank() || raw == "[]") return emptyList()
        subTasksCache.get(raw)?.let { return it }
        val parsed = runCatching { json.decodeFromString<List<DailyOsSubTask>>(raw) }.getOrDefault(emptyList())
        subTasksCache.put(raw, parsed)
        return parsed
    }

    fun encodeSubTasks(subs: List<DailyOsSubTask>): String = json.encodeToString(subs)

    fun parseHistory(raw: String): Map<String, List<DailyOsHistoryLog>> {
        if (raw.isBlank() || raw == "{}") return emptyMap()
        historyCache.get(raw)?.let { return it }
        val parsed = runCatching {
            val obj = json.parseToJsonElement(raw).jsonObject
            obj.mapValues { (_, arrEl) ->
                val arr = arrEl as? JsonArray ?: return@mapValues emptyList()
                arr.mapNotNull { item ->
                    when (item) {
                        is JsonPrimitive -> DailyOsHistoryLog(id = item.content)
                        is JsonObject -> {
                            val id = item["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                            val txt = item["text"]?.jsonPrimitive?.contentOrNull.orEmpty()
                            val comp = item["completedAt"]?.jsonPrimitive?.contentOrNull.orEmpty()
                            DailyOsHistoryLog(id = id, text = txt, completedAt = comp)
                        }
                        else -> null
                    }
                }
            }
        }.getOrDefault(emptyMap())
        historyCache.put(raw, parsed)
        return parsed
    }

    fun encodeHistory(history: Map<String, List<DailyOsHistoryLog>>): String = json.encodeToString(history)

    fun parseTodoSubtasks(raw: String): List<DailyOsTodoSubtask> {
        if (raw.isBlank() || raw == "[]") return emptyList()
        todoSubtasksCache.get(raw)?.let { return it }
        val parsed = runCatching { json.decodeFromString<List<DailyOsTodoSubtask>>(raw) }.getOrDefault(emptyList())
        todoSubtasksCache.put(raw, parsed)
        return parsed
    }

    fun encodeTodoSubtasks(subs: List<DailyOsTodoSubtask>): String = json.encodeToString(subs)

    fun isHabitScheduled(habit: DailyOsHabitEntity, dateStr: String): Boolean {
        val activeDays = parseActiveDays(habit.activeDaysJson)
        if (activeDays.isEmpty()) return true
        val d = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: return true
        return dayOfWeekNum(d) in activeDays
    }

    fun recalculateXp(habits: List<DailyOsHabitEntity>): Int {
        var total = 0
        habits.forEach { h ->
            parseHistory(h.historyJson).values.forEach { list ->
                total += list.size * 10
            }
        }
        return total
    }

    fun calculateHabitStreak(habit: DailyOsHabitEntity, today: String = todayStr()): Int {
        val history = parseHistory(habit.historyJson)
        var streak = 0
        var cur = LocalDate.parse(today)
        repeat(365) {
            val ds = cur.toString()
            val logs = history[ds].orEmpty()
            when {
                logs.isNotEmpty() -> {
                    streak++
                    cur = cur.minusDays(1)
                }
                ds == today -> cur = cur.minusDays(1)
                !isHabitScheduled(habit, ds) -> cur = cur.minusDays(1)
                else -> return streak
            }
        }
        return streak
    }

    fun calculateOverallStreak(habits: List<DailyOsHabitEntity>, today: String = todayStr()): Int {
        val activeHabits = habits.filter { !it.archived }
        if (activeHabits.isEmpty()) return 0
        val histMap = activeHabits.associate { it.id to parseHistory(it.historyJson) }
        var streak = 0
        var cur = LocalDate.parse(today)
        repeat(3650) {
            val ds = cur.toString()
            val scheduled = activeHabits.filter { isHabitScheduled(it, ds) }
            val hasActivity = scheduled.isNotEmpty() && scheduled.any { histMap[it.id]?.get(ds).orEmpty().isNotEmpty() }
            when {
                hasActivity -> streak++
                ds == today -> Unit
                scheduled.isEmpty() -> Unit
                else -> return streak
            }
            cur = cur.minusDays(1)
        }
        return streak
    }

    /** Returns (doneSlots, totalSlots, pct, completedHabitCount, scheduledHabitCount) */
    fun calculateTodayProgress(habits: List<DailyOsHabitEntity>, today: String = todayStr()): IntArray {
        val activeHabits = habits.filter { !it.archived }
        var doneSlots = 0
        var totalSlots = 0
        var completedHabits = 0
        var scheduledHabits = 0
        activeHabits.forEach { h ->
            if (!isHabitScheduled(h, today)) return@forEach
            scheduledHabits++
            val subs = parseSubTasks(h.subTasksJson)
            val tc = if (subs.isNotEmpty()) subs.size else 1
            val hist = parseHistory(h.historyJson)[today].orEmpty()
            val done = hist.size
            totalSlots += tc
            doneSlots += done.coerceAtMost(tc)
            if (done >= tc) completedHabits++
        }
        val pct = if (totalSlots == 0) 0 else ((doneSlots.toDouble() / totalSlots.toDouble()) * 100.0).roundToInt()
        return intArrayOf(doneSlots, totalSlots, pct, completedHabits, scheduledHabits)
    }

    fun calculate7DayAvgPct(habits: List<DailyOsHabitEntity>, today: String = todayStr()): Int {
        val activeHabits = habits.filter { !it.archived }
        if (activeHabits.isEmpty()) return 0
        val todayDate = LocalDate.parse(today)
        val parsed = activeHabits.map { it to parseHistory(it.historyJson) }
        var total = 0
        var done = 0
        for (i in 0 until 7) {
            val ds = todayDate.minusDays(i.toLong()).toString()
            parsed.forEach { (h, hist) ->
                if (!isHabitScheduled(h, ds)) return@forEach
                val subs = parseSubTasks(h.subTasksJson)
                total += if (subs.isNotEmpty()) subs.size else 1
                done += hist[ds].orEmpty().size
            }
        }
        return if (total == 0) 0 else ((done.toDouble() / total.toDouble()) * 100.0).roundToInt().coerceAtMost(100)
    }

    fun calculateTrendPoints(habits: List<DailyOsHabitEntity>, rangeDays: Int, today: String = todayStr()): List<Pair<String, Int>> {
        val activeHabits = habits.filter { !it.archived }
        val parsed = activeHabits.map { h ->
            val subs = parseSubTasks(h.subTasksJson)
            Triple(h, if (subs.isNotEmpty()) subs.size else 1, parseHistory(h.historyJson))
        }
        val todayDate = LocalDate.parse(today)
        val lblFmt = DateTimeFormatter.ofPattern(if (rangeDays <= 30) "EEE d" else "d MMM", ID_LOCALE)
        return (rangeDays - 1 downTo 0).map { offset ->
            val d = todayDate.minusDays(offset.toLong())
            val ds = d.toString()
            var dt = 0
            var dd = 0
            parsed.forEach { (h, slots, hist) ->
                if (!isHabitScheduled(h, ds)) return@forEach
                dt += slots
                dd += hist[ds].orEmpty().size
            }
            val pct = if (dt == 0) 0 else ((dd.toDouble() / dt.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
            d.format(lblFmt) to pct
        }
    }

    fun getHabitStats30Days(habits: List<DailyOsHabitEntity>, today: String = todayStr()): List<DailyOsHabit30Stat> {
        val todayDate = LocalDate.parse(today)
        return habits.filter { !it.archived }.map { h ->
            val hist = parseHistory(h.historyJson)
            val subs = parseSubTasks(h.subTasksJson)
            val totalSlots = if (subs.isNotEmpty()) subs.size else 1
            val scheduledDays = mutableListOf<String>()
            val completedDays = mutableListOf<String>()
            val skipDays = mutableListOf<String>()

            for (i in 0 until 30) {
                val ds = todayDate.minusDays(i.toLong()).toString()
                if (!isHabitScheduled(h, ds)) continue
                scheduledDays.add(ds)
                val logCount = hist[ds].orEmpty().size
                if (logCount >= totalSlots) completedDays.add(ds)
                else if (ds != today) skipDays.add(ds)
            }
            val rate = if (scheduledDays.isEmpty()) 0 else ((completedDays.size.toDouble() / scheduledDays.size) * 100.0).roundToInt()
            val streak = calculateHabitStreak(h, today)

            val skipByDay = IntArray(7)
            val schedByDay = IntArray(7)
            scheduledDays.forEach { ds -> schedByDay[dayOfWeekNum(LocalDate.parse(ds))]++ }
            skipDays.forEach { ds -> skipByDay[dayOfWeekNum(LocalDate.parse(ds))]++ }
            val weakDays = (0 until 7).mapNotNull { dow ->
                val total = schedByDay[dow]
                if (total >= 3 && ((skipByDay[dow].toDouble() / total) * 100.0).roundToInt() >= 50) {
                    DAY_NAMES_FULL[dow]
                } else null
            }

            DailyOsHabit30Stat(
                name = h.name,
                category = h.category.ifBlank { "General" },
                scheduledCount = scheduledDays.size,
                completedCount = completedDays.size,
                skipCount = skipDays.size,
                completionRate = rate,
                currentStreak = streak,
                weakDays = weakDays
            )
        }
    }

    fun buildSummaryMarkdown(
        habits: List<DailyOsHabitEntity>,
        todos: List<DailyOsTodoEntity>,
        kind: String, // "week" | "month"
        offset: Int,
        today: String = todayStr()
    ): Pair<String, String> {
        val isWeek = kind == "week"
        val now = LocalDate.parse(today)
        val (startDate, endDate) = if (isWeek) {
            val dowFromMon = (now.dayOfWeek.value - 1).toLong()
            val start = now.minusDays(dowFromMon + (offset * 7L))
            start to start.plusDays(6)
        } else {
            val ym = YearMonth.from(now).minusMonths(offset.toLong())
            ym.atDay(1) to ym.atEndOfMonth()
        }

        val dates = mutableListOf<String>()
        var cursor = startDate
        while (!cursor.isAfter(endDate)) {
            dates.add(cursor.toString())
            cursor = cursor.plusDays(1)
        }

        val periodLabel = if (isWeek) {
            "${formatShortDate(startDate.toString())} – ${formatShortDate(endDate.toString())} ${endDate.year}"
        } else {
            startDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", ID_LOCALE))
                .replaceFirstChar { it.uppercase(ID_LOCALE) }
        }

        val pctOf = { d: Int, t: Int -> if (t == 0) "–" else "${((d.toDouble() / t) * 100.0).roundToInt()}%" }
        val cellSafe = { s: String -> s.replace("|", "\\|").replace(Regex("\\s*\\n\\s*"), " ") }
        val longFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", ID_LOCALE)

        data class DayCell(val ds: String, val status: String, val done: Int = 0, val slots: Int = 0)
        data class HabitAnalysis(
            val habit: DailyOsHabitEntity,
            val days: List<DayCell>,
            val scheduled: Int,
            val full: Int,
            val partial: Int,
            val miss: Int,
            val slotsDone: Int,
            val slotsTotal: Int
        )

        val rows = habits.filter { !it.archived }.mapNotNull { h ->
            val subs = parseSubTasks(h.subTasksJson)
            val slots = if (subs.isNotEmpty()) subs.size else 1
            val hist = parseHistory(h.historyJson)
            val histDates = hist.filterValues { it.isNotEmpty() }.keys.sorted()
            val startFrom = h.createdAt.ifBlank { histDates.firstOrNull() ?: "0000-00-00" }
            var scheduled = 0; var full = 0; var partial = 0; var miss = 0; var sDone = 0; var sTotal = 0
            val cells = dates.map { ds ->
                when {
                    ds > today -> DayCell(ds, "future")
                    ds < startFrom || !isHabitScheduled(h, ds) -> DayCell(ds, "off")
                    else -> {
                        val d = (hist[ds].orEmpty().size).coerceAtMost(slots)
                        val st = when {
                            d >= slots -> "full"
                            d > 0 -> "partial"
                            else -> "miss"
                        }
                        scheduled++
                        when (st) {
                            "full" -> full++
                            "partial" -> partial++
                            "miss" -> miss++
                        }
                        sDone += d; sTotal += slots
                        DayCell(ds, st, d, slots)
                    }
                }
            }
            if (scheduled > 0) HabitAnalysis(h, cells, scheduled, full, partial, miss, sDone, sTotal) else null
        }

        val totDone = rows.sumOf { it.slotsDone }
        val totTotal = rows.sumOf { it.slotsTotal }
        val totFull = rows.sumOf { it.full }
        val totPartial = rows.sumOf { it.partial }
        val totMiss = rows.sumOf { it.miss }
        val totDays = rows.sumOf { it.scheduled }

        val startStr = startDate.toString()
        val endStr = endDate.toString()

        data class DoneTodo(val t: DailyOsTodoEntity, val ds: String)
        val doneTodos = todos.filter { it.done && !it.completedAt.isNullOrBlank() }
            .mapNotNull { t ->
                val rawComp = t.completedAt.orEmpty()
                val ds = runCatching {
                    Instant.parse(rawComp).atZone(ZoneId.systemDefault()).toLocalDate().toString()
                }.getOrElse {
                    if (Regex("""^\d{4}-\d{2}-\d{2}.*""").matches(rawComp)) rawComp.take(10) else return@mapNotNull null
                }
                if (ds in startStr..endStr) DoneTodo(t, ds) else null
            }
            .sortedWith(compareBy<DoneTodo> { it.ds }.thenBy { it.t.completedAt.orEmpty() })

        val noDateCount = todos.count { it.done && it.completedAt.isNullOrBlank() }
        val withDeadline = doneTodos.filter { it.t.deadline.isNotBlank() }
        val onTime = withDeadline.count { it.ds <= it.t.deadline }

        val lines = mutableListOf<String>()
        lines.add("# ${if (isWeek) "Summary Mingguan" else "Summary Bulanan"} — $periodLabel")
        lines.add("")
        lines.add("> **Periode:** ${startDate.format(longFmt)} → ${endDate.format(longFmt)}  ")
        lines.add("> **Dibuat:** ${LocalDate.now().format(longFmt)}")
        if (endStr >= today && startStr <= today) {
            lines.add("> _Periode masih berjalan — data dihitung sampai ${LocalDate.parse(today).format(longFmt)}._")
        }
        lines.add("")
        lines.add("## 📊 Ringkasan")
        lines.add("")
        lines.add("| Metrik | Hasil |")
        lines.add("|---|---|")
        lines.add("| Capaian habit | **${pctOf(totDone, totTotal)}** ($totDone dari $totTotal aktivitas) |")
        lines.add("| Hari-habit selesai penuh | $totFull dari $totDays |")
        lines.add("| Hari-habit sebagian | $totPartial |")
        lines.add("| Hari-habit belum selesai | $totMiss |")
        lines.add("| To-do selesai | **${doneTodos.size}** task |")
        if (withDeadline.isNotEmpty()) {
            lines.add("| To-do tepat waktu | $onTime dari ${withDeadline.size} yang punya deadline |")
        }
        lines.add("")
        lines.add("## ✅ Habit")
        lines.add("")
        if (rows.isEmpty()) {
            lines.add("_Tidak ada habit terjadwal pada periode ini._")
            lines.add("")
        } else {
            val sym = mapOf("full" to "✅", "partial" to "🔸", "miss" to "❌", "off" to "➖", "future" to "·")
            if (isWeek) {
                val head = dates.map { ds ->
                    val d = LocalDate.parse(ds)
                    "${DAY_NAMES_SHORT[dayOfWeekNum(d)]} ${d.dayOfMonth}"
                }
                lines.add("| Habit | ${head.joinToString(" | ")} | Capaian |")
                lines.add("|---|${dates.joinToString("|") { ":-:" }}|:-:|")
                rows.forEach { r ->
                    lines.add("| ${cellSafe(r.habit.name)} | ${r.days.joinToString(" | ") { sym[it.status] ?: "·" }} | ${pctOf(r.slotsDone, r.slotsTotal)} |")
                }
            } else {
                lines.add("| Habit | Terjadwal | Selesai penuh | Sebagian | Belum | Capaian |")
                lines.add("|---|:-:|:-:|:-:|:-:|:-:|")
                rows.forEach { r ->
                    lines.add("| ${cellSafe(r.habit.name)} | ${r.scheduled} | ${r.full} | ${r.partial} | ${r.miss} | ${pctOf(r.slotsDone, r.slotsTotal)} |")
                }
            }
            lines.add("")
            lines.add("_✅ selesai penuh · 🔸 sebagian · ❌ belum · ➖ tidak dijadwalkan · `·` belum tiba_")
            lines.add("")
            lines.add("### Habit yang belum selesai")
            lines.add("")
            var anyGap = false
            rows.forEach { r ->
                val lbl = { ds: String ->
                    val d = LocalDate.parse(ds)
                    if (isWeek) "${DAY_NAMES_SHORT[dayOfWeekNum(d)]} ${d.dayOfMonth}" else d.dayOfMonth.toString()
                }
                val miss = r.days.filter { it.status == "miss" }.map { lbl(it.ds) }
                val part = r.days.filter { it.status == "partial" }.map { lbl(it.ds) }
                if (miss.isNotEmpty() || part.isNotEmpty()) {
                    anyGap = true
                    val parts = buildList {
                        if (miss.isNotEmpty()) add("belum: ${if (isWeek) "" else "tgl "}${miss.joinToString(", ")}")
                        if (part.isNotEmpty()) add("sebagian: ${if (isWeek) "" else "tgl "}${part.joinToString(", ")}")
                    }
                    lines.add("- **${r.habit.name}** — ${parts.joinToString(" · ")}")
                }
            }
            if (!anyGap) lines.add("Semua habit terjadwal selesai penuh. 🎉")
            lines.add("")
        }

        if (!isWeek) {
            val chunks = mutableListOf<MutableList<String>>()
            dates.forEach { ds ->
                val d = LocalDate.parse(ds)
                if (chunks.isEmpty() || d.dayOfWeek == DayOfWeek.MONDAY) chunks.add(mutableListOf())
                chunks.last().add(ds)
            }
            lines.add("## 📈 Tren per Minggu")
            lines.add("")
            lines.add("| Minggu | Rentang | Capaian habit | To-do selesai |")
            lines.add("|---|---|:-:|:-:|")
            chunks.forEachIndexed { i, c ->
                var d = 0; var t = 0
                rows.forEach { r ->
                    r.days.forEach { day ->
                        if (day.ds in c && day.slots > 0) {
                            d += day.done; t += day.slots
                        }
                    }
                }
                val td = doneTodos.count { it.ds in c }
                lines.add("| ${i + 1} | ${formatShortDate(c.first())} – ${formatShortDate(c.last())} | ${pctOf(d, t)} | $td |")
            }
            lines.add("")
        }

        lines.add("## 📝 To-Do Selesai")
        lines.add("")
        if (doneTodos.isEmpty()) {
            lines.add("_Belum ada to-do yang diselesaikan pada periode ini._")
        } else {
            var lastDs: String? = null
            doneTodos.forEach { (t, ds) ->
                if (ds != lastDs) {
                    if (lastDs != null) lines.add("")
                    val d = LocalDate.parse(ds)
                    lines.add("**${DAY_NAMES_FULL[dayOfWeekNum(d)]}, ${formatShortDate(ds)}**")
                    lines.add("")
                    lastDs = ds
                }
                val subs = parseTodoSubtasks(t.subtasksJson)
                val meta = buildList {
                    if (subs.isNotEmpty()) add("${subs.count { it.done }}/${subs.size} subtask")
                    if (t.deadline.isNotBlank()) add("deadline ${formatShortDate(t.deadline)} — ${if (ds <= t.deadline) "tepat waktu" else "terlambat"}")
                }
                lines.add("- [x] ${t.title}${if (meta.isNotEmpty()) " _(${meta.joinToString(" · ")})_" else ""}")
            }
        }
        lines.add("")
        lines.add("## 🧭 Evaluasi Diri")
        lines.add("")
        lines.add("_Isi saat sesi review akhir pekan._")
        lines.add("")
        lines.add("- **Yang berjalan baik:** ")
        lines.add("- **Yang belum berjalan / hambatan:** ")
        lines.add("- **Pelajaran:** ")
        lines.add("- **Fokus ${if (isWeek) "minggu" else "bulan"} depan:** ")
        lines.add("")
        lines.add("---")
        val footNotes = buildList {
            add("Habit dihitung sejak dibuat (atau sejak aktivitas pertama untuk habit lama); habit yang diarsipkan tidak disertakan.")
            if (noDateCount > 0) add("$noDateCount to-do lama yang selesai tanpa tanggal tercatat tidak ikut dihitung.")
        }
        lines.add("_${footNotes.joinToString(" ")}_")
        lines.add("")

        val fileName = if (isWeek) {
            "summary-mingguan_${startStr}_$endStr.md"
        } else {
            "summary-bulanan_${startStr.take(7)}.md"
        }
        return fileName to lines.joinToString("\n")
    }

    fun buildCsvExport(habits: List<DailyOsHabitEntity>): String? {
        val activeHabits = habits.filter { !it.archived }
        if (activeHabits.isEmpty()) return null
        val sb = StringBuilder("Tanggal,Habit,Selesai,Total,Persentase\n")
        val allDates = sortedSetOf<String>()
        val histMap = activeHabits.associate { it.id to parseHistory(it.historyJson) }
        histMap.values.forEach { map -> allDates.addAll(map.keys) }
        allDates.forEach { ds ->
            activeHabits.forEach { h ->
                val subs = parseSubTasks(h.subTasksJson)
                val tc = if (subs.isNotEmpty()) subs.size else 1
                val done = histMap[h.id]?.get(ds)?.size ?: 0
                val pct = if (tc == 0) 0 else ((done.toDouble() / tc) * 100.0).roundToInt()
                sb.append("\"$ds\",\"${h.name.replace("\"", "\"\"")}\",\"$done\",\"$tc\",\"$pct%\"\n")
            }
        }
        return sb.toString()
    }

    fun buildBackupJson(
        habits: List<DailyOsHabitEntity>,
        todos: List<DailyOsTodoEntity>,
        coachMemory: DailyOsCoachMemoryDto?
    ): String {
        val hDtos = habits.map { h ->
            val histRaw = parseHistory(h.historyJson).mapValues { (_, list) ->
                JsonArray(list.map { item ->
                    JsonObject(
                        mapOf(
                            "id" to JsonPrimitive(item.id),
                            "text" to JsonPrimitive(item.text),
                            "completedAt" to JsonPrimitive(item.completedAt)
                        )
                    )
                })
            }
            DailyOsHabitBackupDto(
                id = h.id,
                name = h.name,
                category = h.category,
                color = h.color,
                icon = h.icon,
                activeDays = parseActiveDays(h.activeDaysJson),
                subTasks = parseSubTasks(h.subTasksJson),
                history = histRaw,
                collapsed = h.collapsed,
                archived = h.archived,
                createdAt = h.createdAt
            )
        }
        val tDtos = todos.map { t ->
            DailyOsTodoBackupDto(
                id = t.id,
                title = t.title,
                priority = t.priority,
                deadline = t.deadline,
                done = t.done,
                completedAt = t.completedAt,
                subtasks = parseTodoSubtasks(t.subtasksJson)
            )
        }
        val wrapper = DailyOsBackupWrapperDto(
            version = 3,
            habits = hDtos,
            todos = tDtos,
            totalPoints = recalculateXp(habits),
            coachMemory = coachMemory,
            exportedAt = Instant.now().toString()
        )
        return json.encodeToString(wrapper)
    }

    fun parseBackupJson(raw: String): Triple<List<DailyOsHabitEntity>, List<DailyOsTodoEntity>, DailyOsCoachMemoryDto?> {
        val wrapper = json.decodeFromString<DailyOsBackupWrapperDto>(raw.trim())
        val baseTs = System.currentTimeMillis()
        val habits = wrapper.habits.mapIndexed { i, dto ->
            val subTasks = dto.subTasks
            val histParsed = dto.history.mapValues { (_, arrEl) ->
                val arr = arrEl as? JsonArray ?: return@mapValues emptyList()
                arr.mapNotNull { entry ->
                    when (entry) {
                        is JsonPrimitive -> {
                            val sid = entry.content
                            val txt = if (sid == "main") dto.name else subTasks.firstOrNull { it.id == sid }?.text ?: sid
                            DailyOsHistoryLog(id = sid, text = txt)
                        }
                        is JsonObject -> {
                            val sid = entry["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                            val txt = entry["text"]?.jsonPrimitive?.contentOrNull
                                ?: (if (sid == "main") dto.name else subTasks.firstOrNull { it.id == sid }?.text ?: sid)
                            val comp = entry["completedAt"]?.jsonPrimitive?.contentOrNull.orEmpty()
                            DailyOsHistoryLog(id = sid, text = txt, completedAt = comp)
                        }
                        else -> null
                    }
                }
            }
            DailyOsHabitEntity(
                id = dto.id?.takeIf { it.isNotBlank() } ?: "hab_${baseTs}_$i",
                name = dto.name.ifBlank { "Habit" },
                category = dto.category.ifBlank { "General" },
                color = dto.color.ifBlank { "#1A1917" },
                icon = dto.icon.ifBlank { "star" },
                activeDaysJson = encodeActiveDays(dto.activeDays),
                subTasksJson = encodeSubTasks(subTasks),
                historyJson = encodeHistory(histParsed),
                collapsed = dto.collapsed,
                archived = dto.archived,
                createdAt = dto.createdAt ?: todayStr(),
                sortOrder = i
            )
        }
        val todos = wrapper.todos.mapIndexed { i, t ->
            DailyOsTodoEntity(
                id = t.id.ifBlank { "todo_${baseTs}_$i" },
                title = t.title.ifBlank { "Task" },
                priority = t.priority.ifBlank { "normal" },
                deadline = t.deadline,
                done = t.done,
                completedAt = t.completedAt,
                subtasksJson = encodeTodoSubtasks(t.subtasks),
                sortOrder = i
            )
        }
        return Triple(habits, todos, wrapper.coachMemory)
    }
}
