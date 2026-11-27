package com.ecosystemku.assistant.notifications

import com.ecosystemku.assistant.data.local.CuciBookEntity
import com.ecosystemku.assistant.data.local.CuciMasterBudgetEntity
import com.ecosystemku.assistant.data.local.CuciTransactionEntity
import com.ecosystemku.assistant.data.local.CuciWishlistEntity
import com.ecosystemku.assistant.data.local.DailyOsHabitEntity
import com.ecosystemku.assistant.data.local.DailyOsTodoEntity
import com.ecosystemku.assistant.data.local.IdekuIdeaEntity
import com.ecosystemku.assistant.data.local.KronikEntryEntity
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProactiveEvaluatorTest {

    private val zone = ZoneId.of("Asia/Jakarta")

    private fun millisAt(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long {
        return LocalDateTime.of(year, month, day, hour, minute)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
    }

    @Test
    fun quietHoursHandlesMidnightSpanAndSameDayRange() {
        assertTrue(ProactiveEvaluator.isWithinQuietHours(LocalTime.of(22, 30), "22:00", "07:00"))
        assertTrue(ProactiveEvaluator.isWithinQuietHours(LocalTime.of(6, 59), "22:00", "07:00"))
        assertFalse(ProactiveEvaluator.isWithinQuietHours(LocalTime.of(7, 0), "22:00", "07:00"))
        assertFalse(ProactiveEvaluator.isWithinQuietHours(LocalTime.of(21, 59), "22:00", "07:00"))
        assertTrue(ProactiveEvaluator.isWithinQuietHours(LocalTime.of(13, 0), "12:00", "14:00"))
    }

    @Test
    fun cuciMoneyBudgetTriggersAt80And100AndIgnoresTransfers() {
        val now = millisAt(2026, 11, 20, 14, 0)
        val book = CuciBookEntity(id = 1L, name = "Personal")
        val budget = CuciMasterBudgetEntity(id = 1L, bookId = 1L, month = 10, year = 2026, limitAmount = 1_000_000L)

        // 790k expense + 500k transfer -> transfer ignored, 79% should not trigger
        val snap79 = ProactiveSnapshot(
            profileName = "Mahfud",
            books = listOf(book),
            masterBudgets = listOf(budget),
            transactions = listOf(
                CuciTransactionEntity(1L, 1L, "Makan", 790_000L, "out", 1L, 1L, "2026-11-10"),
                CuciTransactionEntity(2L, 1L, "Transfer", 500_000L, "out", 12L, 1L, "2026-11-12", transferPair = 99L)
            )
        )
        assertNull(ProactiveEvaluator.evaluate(snap79, ProactivePreferences(), ProactiveCooldownState(), now, zone))

        // 800k -> 80% warning
        val snap80 = snap79.copy(
            transactions = snap79.transactions + CuciTransactionEntity(3L, 1L, "Kopi", 10_000L, "out", 2L, 1L, "2026-11-15")
        )
        val alert80 = ProactiveEvaluator.evaluate(snap80, ProactivePreferences(), ProactiveCooldownState(), now, zone)
        assertNotNull(alert80)
        assertEquals("BUDGET_WARN_80", alert80?.ruleKey)
        assertEquals(DailyOsNotificationHelper.ACTION_OPEN_CUCIMONEY, alert80?.launchAction)

        // Cooldown prevents repeating 80% alert, but 100% has a distinct key and still triggers
        val cooldownWith80 = ProactiveCooldownState(
            firedTodayCount = 1,
            lastFiredByKeyMs = mapOf(alert80!!.cooldownKey to now - 3_600_000L)
        )
        assertNull(ProactiveEvaluator.evaluate(snap80, ProactivePreferences(), cooldownWith80, now, zone))

        val snap100 = snap80.copy(
            transactions = snap80.transactions + CuciTransactionEntity(4L, 1L, "Belanja", 250_000L, "out", 4L, 1L, "2026-11-18")
        )
        val alert100 = ProactiveEvaluator.evaluate(snap100, ProactivePreferences(), cooldownWith80, now, zone)
        assertEquals("BUDGET_OVER_100", alert100?.ruleKey)
    }

    @Test
    fun streakRescueOnlyFiresInEveningForActiveUncheckedStreak() {
        val history3Days = """
            {
              "2026-11-17": [{"id":"1"}],
              "2026-11-18": [{"id":"1"}],
              "2026-11-19": [{"id":"1"}]
            }
        """.trimIndent()
        val habit = DailyOsHabitEntity(
            id = "h1",
            name = "Baca Buku",
            historyJson = history3Days
        )
        val snap = ProactiveSnapshot(profileName = "Mahfud", habits = listOf(habit))

        // Morning (10:00) -> too early for streak rescue
        val morning = millisAt(2026, 11, 20, 10, 0)
        assertNull(ProactiveEvaluator.evaluate(snap, ProactivePreferences(), ProactiveCooldownState(), morning, zone))

        // Evening (19:30) -> triggers streak rescue
        val evening = millisAt(2026, 11, 20, 19, 30)
        val alert = ProactiveEvaluator.evaluate(snap, ProactivePreferences(), ProactiveCooldownState(), evening, zone)
        assertNotNull(alert)
        assertEquals("STREAK_RESCUE", alert?.ruleKey)
        assertTrue(alert!!.body.contains("3 hari"))
        assertEquals(DailyOsNotificationHelper.ACTION_OPEN_DAILYOS, alert.launchAction)

        // Already checked today -> no rescue
        val checkedToday = habit.copy(
            historyJson = """
                {
                  "2026-11-17": [{"id":"1"}],
                  "2026-11-18": [{"id":"1"}],
                  "2026-11-19": [{"id":"1"}],
                  "2026-11-20": [{"id":"1"}]
                }
            """.trimIndent()
        )
        assertNull(
            ProactiveEvaluator.evaluate(
                snap.copy(habits = listOf(checkedToday)),
                ProactivePreferences(),
                ProactiveCooldownState(),
                evening,
                zone
            )
        )
    }

    @Test
    fun kronikHeavyMoodRoutesToCheckInAndRespectsDailyCap() {
        val now = millisAt(2026, 11, 20, 15, 0)
        val snap = ProactiveSnapshot(
            profileName = "Mahfud",
            kronikEntries = listOf(
                KronikEntryEntity(id = "k1", date = "2026-11-19", title = "Hari berat", moodStatus = "Berat"),
                KronikEntryEntity(id = "k2", date = "2026-11-18", title = "Capek", moodStatus = "Lelah")
            )
        )

        val alert = ProactiveEvaluator.evaluate(snap, ProactivePreferences(), ProactiveCooldownState(), now, zone)
        assertNotNull(alert)
        assertEquals("KRONIK_HEAVY_MOOD", alert?.ruleKey)
        assertEquals(DailyOsNotificationHelper.ACTION_OPEN_CHECKIN, alert?.launchAction)

        // Daily cap reached -> suppressed unless ignoreDailyCap = true
        val capped = ProactiveCooldownState(firedTodayCount = 2)
        assertNull(ProactiveEvaluator.evaluate(snap, ProactivePreferences(), capped, now, zone))
        assertNotNull(
            ProactiveEvaluator.evaluate(
                snap,
                ProactivePreferences(),
                capped,
                now,
                zone,
                ignoreDailyCap = true
            )
        )
    }

    @Test
    fun wishlistTodoAndStagnantIdeaRulesAndPriorityOrdering() {
        val now = millisAt(2026, 11, 20, 14, 0)
        val oldCreated = now - 10L * 86_400_000L
        val snap = ProactiveSnapshot(
            profileName = "Mahfud",
            wishlists = listOf(
                CuciWishlistEntity(id = "w1", title = "Headphone", price = 1_000_000L, saved = 1_000_000L, status = "Belum")
            ),
            todos = listOf(
                DailyOsTodoEntity(id = "t1", title = "Bayar internet", priority = "urgent", deadline = "2026-11-20", done = false)
            ),
            ideas = listOf(
                IdekuIdeaEntity(id = 10L, title = "Aplikasi Catatan", status = "Proses", createdAt = oldCreated)
            )
        )

        // Urgent/due-today todo (priority 88) beats wishlist (78) and stagnant idea (50)
        val topAlert = ProactiveEvaluator.evaluate(snap, ProactivePreferences(), ProactiveCooldownState(), now, zone)
        assertEquals("TODO_DUE_SOON", topAlert?.ruleKey)

        // Disable habits category -> wishlist takes top spot
        val financeOnly = ProactivePreferences(habitsEnabled = false)
        val nextAlert = ProactiveEvaluator.evaluate(snap, financeOnly, ProactiveCooldownState(), now, zone)
        assertEquals("WISHLIST_READY_100", nextAlert?.ruleKey)

        // Disable finance & reflection -> stagnant idea fires
        val ideasOnly = ProactivePreferences(financeEnabled = false, habitsEnabled = false, reflectionEnabled = false)
        val ideaAlert = ProactiveEvaluator.evaluate(snap, ideasOnly, ProactiveCooldownState(), now, zone)
        assertEquals("IDEA_STAGNANT", ideaAlert?.ruleKey)
        assertEquals(DailyOsNotificationHelper.ACTION_OPEN_IDEKU, ideaAlert?.launchAction)
    }
}
