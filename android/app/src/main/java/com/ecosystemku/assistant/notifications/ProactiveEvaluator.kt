package com.ecosystemku.assistant.notifications

import com.ecosystemku.assistant.data.local.CuciBookEntity
import com.ecosystemku.assistant.data.local.CuciMasterBudgetEntity
import com.ecosystemku.assistant.data.local.CuciTransactionEntity
import com.ecosystemku.assistant.data.local.CuciWishlistEntity
import com.ecosystemku.assistant.data.local.DailyOsHabitEntity
import com.ecosystemku.assistant.data.local.DailyOsTodoEntity
import com.ecosystemku.assistant.data.local.IdekuIdeaEntity
import com.ecosystemku.assistant.data.local.KronikEntryEntity
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject

enum class ProactiveCategory(
    val key: String,
    val channelId: String,
    val label: String
) {
    FINANCE("finance", DailyOsNotificationHelper.CHANNEL_PROACTIVE_FINANCE, "Keuangan"),
    HABITS("habits", DailyOsNotificationHelper.CHANNEL_PROACTIVE_HABITS, "Habit & To-Do"),
    REFLECTION("reflection", DailyOsNotificationHelper.CHANNEL_PROACTIVE_REFLECTION, "Refleksi & Kronik"),
    IDEAS("ideas", DailyOsNotificationHelper.CHANNEL_PROACTIVE_IDEAS, "IdeKu")
}

data class ProactivePreferences(
    val enabled: Boolean = true,
    val financeEnabled: Boolean = true,
    val habitsEnabled: Boolean = true,
    val reflectionEnabled: Boolean = true,
    val ideasEnabled: Boolean = true,
    val quietStart: String = "22:00",
    val quietEnd: String = "07:00",
    val dailyCap: Int = 2
) {
    fun isCategoryEnabled(category: ProactiveCategory): Boolean = when (category) {
        ProactiveCategory.FINANCE -> financeEnabled
        ProactiveCategory.HABITS -> habitsEnabled
        ProactiveCategory.REFLECTION -> reflectionEnabled
        ProactiveCategory.IDEAS -> ideasEnabled
    }
}

data class ProactiveSnapshot(
    val profileName: String = "Tuan",
    val books: List<CuciBookEntity> = emptyList(),
    val transactions: List<CuciTransactionEntity> = emptyList(),
    val masterBudgets: List<CuciMasterBudgetEntity> = emptyList(),
    val wishlists: List<CuciWishlistEntity> = emptyList(),
    val habits: List<DailyOsHabitEntity> = emptyList(),
    val todos: List<DailyOsTodoEntity> = emptyList(),
    val kronikEntries: List<KronikEntryEntity> = emptyList(),
    val ideas: List<IdekuIdeaEntity> = emptyList()
)

data class ProactiveCooldownState(
    val firedTodayCount: Int = 0,
    val lastFiredByKeyMs: Map<String, Long> = emptyMap()
)

data class ProactiveAlert(
    val ruleKey: String,
    val cooldownKey: String,
    val cooldownMs: Long,
    val category: ProactiveCategory,
    val priority: Int,
    val notificationId: Int,
    val title: String,
    val body: String,
    val launchAction: String
)

/**
 * Pure Kotlin rule engine that evaluates local cross-module data with zero LLM tokens
 * and returns the highest-priority proactive notification respecting quiet hours,
 * per-rule cooldowns, and daily caps.
 */
object ProactiveEvaluator {
    private const val DAY_MS = 86_400_000L
    private val json = Json { ignoreUnknownKeys = true }
    private val idLocale = Locale("in", "ID")

    fun evaluate(
        snapshot: ProactiveSnapshot,
        prefs: ProactivePreferences,
        cooldownState: ProactiveCooldownState,
        nowMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        ignoreQuietHours: Boolean = false,
        ignoreDailyCap: Boolean = false
    ): ProactiveAlert? {
        if (!prefs.enabled) return null
        if (!ignoreDailyCap && cooldownState.firedTodayCount >= prefs.dailyCap.coerceAtLeast(1)) {
            return null
        }

        val zonedNow = Instant.ofEpochMilli(nowMs).atZone(zoneId)
        val localTime = zonedNow.toLocalTime()
        if (!ignoreQuietHours && isWithinQuietHours(localTime, prefs.quietStart, prefs.quietEnd)) {
            return null
        }

        val today = zonedNow.toLocalDate()
        val candidates = collectCandidates(snapshot, prefs, today, localTime, nowMs)
        return candidates
            .filter { candidate ->
                val lastAt = cooldownState.lastFiredByKeyMs[candidate.cooldownKey] ?: 0L
                lastAt <= 0L || (nowMs - lastAt) >= candidate.cooldownMs
            }
            .maxByOrNull { it.priority }
    }

    fun isWithinQuietHours(
        now: LocalTime,
        quietStartStr: String,
        quietEndStr: String
    ): Boolean {
        val start = parseLocalTimeOrNull(quietStartStr) ?: return false
        val end = parseLocalTimeOrNull(quietEndStr) ?: return false
        if (start == end) return false
        return if (start.isBefore(end)) {
            !now.isBefore(start) && now.isBefore(end)
        } else {
            // Spans midnight (e.g., 22:00 .. 07:00)
            !now.isBefore(start) || now.isBefore(end)
        }
    }

    private fun parseLocalTimeOrNull(raw: String): LocalTime? {
        val parts = raw.trim().split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull()?.takeIf { it in 0..23 } ?: return null
        val m = parts[1].toIntOrNull()?.takeIf { it in 0..59 } ?: return null
        return LocalTime.of(h, m)
    }

    private fun collectCandidates(
        snapshot: ProactiveSnapshot,
        prefs: ProactivePreferences,
        today: LocalDate,
        localTime: LocalTime,
        nowMs: Long
    ): List<ProactiveAlert> {
        val name = snapshot.profileName.trim().ifBlank { "Tuan" }
        val alerts = mutableListOf<ProactiveAlert>()

        if (prefs.isCategoryEnabled(ProactiveCategory.FINANCE)) {
            evaluateBudgetGuard(snapshot, name, today)?.let(alerts::add)
            evaluateWishlistProgress(snapshot, name)?.let(alerts::add)
        }
        if (prefs.isCategoryEnabled(ProactiveCategory.HABITS)) {
            evaluateUrgentTodos(snapshot, name, today)?.let(alerts::add)
            evaluateStreakRescue(snapshot, name, today, localTime)?.let(alerts::add)
        }
        if (prefs.isCategoryEnabled(ProactiveCategory.REFLECTION)) {
            evaluateKronikSignals(snapshot, name, today)?.let(alerts::add)
        }
        if (prefs.isCategoryEnabled(ProactiveCategory.IDEAS)) {
            evaluateStagnantIdeas(snapshot, name, nowMs)?.let(alerts::add)
        }
        return alerts
    }

    private fun evaluateBudgetGuard(
        snapshot: ProactiveSnapshot,
        name: String,
        today: LocalDate
    ): ProactiveAlert? {
        val curYear = today.year
        val curMonth0 = today.monthValue - 1
        val monthPrefix = String.format(Locale.US, "%04d-%02d-", curYear, curMonth0 + 1)
        val bookNames = snapshot.books.associate { it.id to it.name }

        var best: ProactiveAlert? = null
        for (budget in snapshot.masterBudgets) {
            if (budget.year != curYear || budget.month != curMonth0 || budget.limitAmount <= 0L) continue
            val spent = snapshot.transactions
                .asSequence()
                .filter { tx ->
                    tx.bookId == budget.bookId &&
                        tx.direction == "out" &&
                        tx.transferPair == null &&
                        isTxInBudgetWindow(tx.date, monthPrefix, budget.startDate, budget.endDate)
                }
                .sumOf { it.amount }

            val pct = ((spent * 100.0) / budget.limitAmount.toDouble()).toInt()
            if (pct < 80) continue

            val bookLabel = bookNames[budget.bookId]?.takeIf { it.isNotBlank() } ?: "Utama"
            val isOver = pct >= 100
            val ruleKey = if (isOver) "BUDGET_OVER_100" else "BUDGET_WARN_80"
            val cooldownKey = "${if (isOver) "budget_100" else "budget_80"}_${budget.bookId}_${curYear}_${curMonth0}"
            val priority = if (isOver) 95 else 82
            val spentRp = formatRupiah(spent)
            val limitRp = formatRupiah(budget.limitAmount)
            val body = if (isOver) {
                "$name, pengeluaran buku $bookLabel sudah mencapai $pct% ($spentRp dari batas $limitRp). Yuk cek di CuciMoney+."
            } else {
                "$name, anggaran buku $bookLabel sudah terpakai $pct% ($spentRp / $limitRp). Rem sarankan mulai mengerem pengeluaran ya."
            }

            val candidate = ProactiveAlert(
                ruleKey = ruleKey,
                cooldownKey = cooldownKey,
                cooldownMs = 14L * DAY_MS,
                category = ProactiveCategory.FINANCE,
                priority = priority,
                notificationId = 7101,
                title = "Rem · Anggaran CuciMoney+",
                body = body,
                launchAction = DailyOsNotificationHelper.ACTION_OPEN_CUCIMONEY
            )
            if (best == null || candidate.priority > best.priority) {
                best = candidate
            }
        }
        return best
    }

    private fun isTxInBudgetWindow(
        txDate: String,
        monthPrefix: String,
        startDate: String,
        endDate: String
    ): Boolean {
        return if (startDate.isNotBlank() && endDate.isNotBlank()) {
            txDate >= startDate && txDate <= endDate
        } else {
            txDate.startsWith(monthPrefix)
        }
    }

    private fun evaluateWishlistProgress(
        snapshot: ProactiveSnapshot,
        name: String
    ): ProactiveAlert? {
        val ready = snapshot.wishlists
            .filter { !it.status.equals("Selesai", ignoreCase = true) && it.price > 0L && it.saved > 0L }
            .mapNotNull { wish ->
                val pct = ((wish.saved * 100.0) / wish.price.toDouble()).toInt()
                if (pct >= 85) wish to pct else null
            }
            .maxByOrNull { it.second } ?: return null

        val (wish, pct) = ready
        val isFull = wish.saved >= wish.price
        val cooldownKey = "${if (isFull) "wish_100" else "wish_85"}_${wish.id}"
        val body = if (isFull) {
            "$name, tabungan wishlist \"${wish.title}\" sudah genap ${formatRupiah(wish.saved)}! Siap direalisasikan di CuciMoney+."
        } else {
            "$name, tabungan wishlist \"${wish.title}\" sudah $pct% (${formatRupiah(wish.saved)} / ${formatRupiah(wish.price)}). Sedikit lagi tercapai!"
        }
        return ProactiveAlert(
            ruleKey = if (isFull) "WISHLIST_READY_100" else "WISHLIST_NEAR_85",
            cooldownKey = cooldownKey,
            cooldownMs = 7L * DAY_MS,
            category = ProactiveCategory.FINANCE,
            priority = if (isFull) 78 else 62,
            notificationId = 7102,
            title = "Rem · Tabungan Wishlist",
            body = body,
            launchAction = DailyOsNotificationHelper.ACTION_OPEN_CUCIMONEY
        )
    }

    private fun evaluateUrgentTodos(
        snapshot: ProactiveSnapshot,
        name: String,
        today: LocalDate
    ): ProactiveAlert? {
        val todayStr = today.toString()
        val tomorrowStr = today.plusDays(1).toString()

        val pending = snapshot.todos.filter { !it.done }
        if (pending.isEmpty()) return null

        val overdueOrToday = pending.filter { it.deadline.isNotBlank() && it.deadline <= todayStr }
        val urgent = pending.filter { it.priority.equals("urgent", ignoreCase = true) }
        val dueTomorrow = pending.filter { it.deadline == tomorrowStr }

        val targetList = when {
            overdueOrToday.isNotEmpty() -> overdueOrToday
            urgent.isNotEmpty() -> urgent
            dueTomorrow.isNotEmpty() -> dueTomorrow
            else -> return null
        }

        val topTitle = targetList.first().title.take(48)
        val priority = when {
            overdueOrToday.isNotEmpty() -> 88
            urgent.isNotEmpty() -> 84
            else -> 72
        }
        val body = when {
            overdueOrToday.size > 1 ->
                "$name, ada ${overdueOrToday.size} tugas Daily OS yang jatuh tempo/terlewat (termasuk \"$topTitle\"). Yuk bereskan hari ini."
            overdueOrToday.size == 1 ->
                "$name, tugas \"$topTitle\" jatuh tempo hari ini. Mau Rem bantu cicil sekarang?"
            urgent.isNotEmpty() ->
                "$name, ada ${urgent.size} tugas prioritas mendesak (\"$topTitle\") yang masih menunggu."
            else ->
                "$name, besok ada deadline untuk \"$topTitle\" (${dueTomorrow.size} tugas). Rem ingatkan lebih awal ya."
        }

        return ProactiveAlert(
            ruleKey = "TODO_DUE_SOON",
            cooldownKey = "todo_due_$todayStr",
            cooldownMs = 20L * 3_600_000L,
            category = ProactiveCategory.HABITS,
            priority = priority,
            notificationId = 7201,
            title = "Rem · Tugas Mendesak",
            body = body,
            launchAction = DailyOsNotificationHelper.ACTION_OPEN_DAILYOS
        )
    }

    private fun evaluateStreakRescue(
        snapshot: ProactiveSnapshot,
        name: String,
        today: LocalDate,
        localTime: LocalTime
    ): ProactiveAlert? {
        // Rescue reminder only runs during late afternoon / evening window
        if (localTime.hour !in 18..21) return null

        val todayStr = today.toString()
        val atRisk = snapshot.habits
            .asSequence()
            .filter { !it.archived && isHabitScheduled(it, today) }
            .mapNotNull { habit ->
                val history = parseHabitHistoryDates(habit.historyJson)
                if (history[todayStr]?.isNotEmpty() == true) return@mapNotNull null
                val streak = calculateHabitStreak(habit, today, history)
                if (streak >= 3) habit to streak else null
            }
            .maxByOrNull { it.second } ?: return null

        val (habit, streak) = atRisk
        return ProactiveAlert(
            ruleKey = "STREAK_RESCUE",
            cooldownKey = "streak_rescue_$todayStr",
            cooldownMs = 20L * 3_600_000L,
            category = ProactiveCategory.HABITS,
            priority = 86,
            notificationId = 7202,
            title = "Rem · Jaga Streak Habit",
            body = "$name, streak ${streak} hari untuk \"${habit.name}\" belum dicentang hari ini. Jangan sampai putus ya!",
            launchAction = DailyOsNotificationHelper.ACTION_OPEN_DAILYOS
        )
    }

    private fun evaluateKronikSignals(
        snapshot: ProactiveSnapshot,
        name: String,
        today: LocalDate
    ): ProactiveAlert? {
        val visible = snapshot.kronikEntries
            .filter { !it.isArchived && it.date.isNotBlank() }
            .sortedByDescending { it.date }
        if (visible.isEmpty()) return null

        val recentTwo = visible.take(2)
        val latestDate = runCatching { LocalDate.parse(visible.first().date) }.getOrNull() ?: return null
        val daysSinceLast = ChronoUnit.DAYS.between(latestDate, today)

        val heavyMoods = setOf("Berat", "Lelah")
        val twoHeavyRecent = recentTwo.size == 2 &&
            daysSinceLast <= 7L &&
            recentTwo.all { it.moodStatus in heavyMoods }

        if (twoHeavyRecent) {
            return ProactiveAlert(
                ruleKey = "KRONIK_HEAVY_MOOD",
                cooldownKey = "kronik_heavy_mood",
                cooldownMs = 3L * DAY_MS,
                category = ProactiveCategory.REFLECTION,
                priority = 80,
                notificationId = 7301,
                title = "Rem · Check-In",
                body = "$name, akhir-akhir ini catatan Kronikmu terasa berat dan lelah. Kalau mau cerita atau tarik napas sebentar, Rem selalu ada.",
                launchAction = DailyOsNotificationHelper.ACTION_OPEN_CHECKIN
            )
        }

        if (daysSinceLast >= 4L) {
            return ProactiveAlert(
                ruleKey = "KRONIK_GAP",
                cooldownKey = "kronik_gap",
                cooldownMs = 4L * DAY_MS,
                category = ProactiveCategory.REFLECTION,
                priority = 55,
                notificationId = 7302,
                title = "Rem · Jurnal Kronik",
                body = "$name, sudah $daysSinceLast hari belum ada catatan baru di Kronik. Mau tulis 1-2 kalimat refleksi hari ini?",
                launchAction = DailyOsNotificationHelper.ACTION_OPEN_KRONIK
            )
        }
        return null
    }

    private fun evaluateStagnantIdeas(
        snapshot: ProactiveSnapshot,
        name: String,
        nowMs: Long
    ): ProactiveAlert? {
        val stagnant = snapshot.ideas
            .filter {
                it.status.equals("Proses", ignoreCase = true) &&
                    it.createdAt > 0L &&
                    (nowMs - it.createdAt) >= 7L * DAY_MS
            }
            .minByOrNull { it.createdAt } ?: return null

        val daysOld = ((nowMs - stagnant.createdAt) / DAY_MS).coerceAtLeast(7L)
        return ProactiveAlert(
            ruleKey = "IDEA_STAGNANT",
            cooldownKey = "ideku_stagnant_${stagnant.id}",
            cooldownMs = 7L * DAY_MS,
            category = ProactiveCategory.IDEAS,
            priority = 50,
            notificationId = 7401,
            title = "Rem · Fokus IdeKu",
            body = "$name, ide \"${stagnant.title}\" sudah $daysOld hari di status Proses. Mau dipecah langkah kecilnya atau diparkir dulu?",
            launchAction = DailyOsNotificationHelper.ACTION_OPEN_IDEKU
        )
    }

    private fun isHabitScheduled(habit: DailyOsHabitEntity, date: LocalDate): Boolean {
        val activeDays = parseActiveDays(habit.activeDaysJson)
        if (activeDays.isEmpty()) return true
        val dow = if (date.dayOfWeek == DayOfWeek.SUNDAY) 0 else date.dayOfWeek.value
        return dow in activeDays
    }

    private fun parseActiveDays(raw: String): List<Int> {
        if (raw.isBlank() || raw == "[]") return emptyList()
        return runCatching {
            json.decodeFromString<List<Int>>(raw).filter { it in 0..6 }.distinct()
        }.getOrDefault(emptyList())
    }

    private fun parseHabitHistoryDates(raw: String): Map<String, JsonArray> {
        if (raw.isBlank() || raw == "{}") return emptyMap()
        return runCatching {
            val obj = json.parseToJsonElement(raw).jsonObject
            buildMap {
                obj.forEach { (dateKey, el) ->
                    val arr = el as? JsonArray
                    if (arr != null && arr.isNotEmpty()) {
                        put(dateKey, arr)
                    }
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun calculateHabitStreak(
        habit: DailyOsHabitEntity,
        today: LocalDate,
        history: Map<String, JsonArray>
    ): Int {
        var streak = 0
        var cur = today
        repeat(365) {
            val ds = cur.toString()
            val hasLogs = history[ds]?.isNotEmpty() == true
            when {
                hasLogs -> {
                    streak++
                    cur = cur.minusDays(1)
                }
                cur == today -> cur = cur.minusDays(1)
                !isHabitScheduled(habit, cur) -> cur = cur.minusDays(1)
                else -> return streak
            }
        }
        return streak
    }

    private fun formatRupiah(amount: Long): String {
        val nf = NumberFormat.getIntegerInstance(idLocale)
        return "Rp ${nf.format(kotlin.math.abs(amount))}"
    }
}
