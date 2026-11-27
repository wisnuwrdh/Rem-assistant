package com.ecosystemku.assistant.notifications

import com.ecosystemku.assistant.data.local.DailyOsHabitEntity
import com.ecosystemku.assistant.data.local.DailyOsTodoEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyOsNotificationHelperTest {

    private val friday = LocalDate.of(2026, 11, 27) // Jum, 27 Nov (dow = 5)

    @Test
    fun todoReminderIsSuppressedWhenNoPendingTodosEvenWithUnfinishedHabits() {
        val unfinishedHabit = DailyOsHabitEntity(
            id = "h1",
            name = "Old Habit Comeback",
            subTasksJson = """[{"id":"s1","text":"A"},{"id":"s2","text":"B"}]""",
            historyJson = "{}"
        )
        val completedTodo = DailyOsTodoEntity(
            id = "t1",
            title = "Done Task",
            done = true
        )

        val body = DailyOsNotificationHelper.resolveScheduledReminderBody(
            requestCode = DailyOsNotificationHelper.REQ_TODO,
            profileName = "Wisnu",
            habits = listOf(unfinishedHabit),
            todos = listOf(completedTodo),
            today = friday
        )

        assertNull(body)
    }

    @Test
    fun todoReminderFiresWhenPendingTodosExist() {
        val todos = listOf(
            DailyOsTodoEntity(id = "t1", title = "Pending 1", done = false),
            DailyOsTodoEntity(id = "t2", title = "Pending 2", done = false),
            DailyOsTodoEntity(id = "t3", title = "Done", done = true)
        )

        val body = DailyOsNotificationHelper.resolveScheduledReminderBody(
            requestCode = DailyOsNotificationHelper.REQ_TODO,
            profileName = "Wisnu",
            habits = emptyList(),
            todos = todos,
            today = friday
        )

        assertNotNull(body)
        assertTrue(body!!.contains("2 tugas To-Do"))
        assertTrue(body.startsWith("Wisnu,"))
    }

    @Test
    fun habitReminderRespectsScheduleSubTasksAndCompletionStatus() {
        val dailyHabitUnchecked = DailyOsHabitEntity(
            id = "h1",
            name = "Old Habit Comeback",
            activeDaysJson = "[]",
            subTasksJson = """[{"id":"s1","text":"1"},{"id":"s2","text":"2"},{"id":"s3","text":"3"}]""",
            historyJson = """{"2026-11-27":[{"id":"s1"}]}""" // only 1 of 3 subtasks done -> still incomplete
        )
        val weekendOnlyHabit = DailyOsHabitEntity(
            id = "h2",
            name = "Review dan Evaluasi Diri",
            activeDaysJson = "[0, 6]", // Min, Sab -> not scheduled on Friday
            historyJson = "{}"
        )
        val archivedHabit = DailyOsHabitEntity(
            id = "h3",
            name = "Archived",
            archived = true,
            historyJson = "{}"
        )

        val body = DailyOsNotificationHelper.resolveScheduledReminderBody(
            requestCode = DailyOsNotificationHelper.REQ_HABIT,
            profileName = "Wisnu",
            habits = listOf(dailyHabitUnchecked, weekendOnlyHabit, archivedHabit),
            todos = emptyList(),
            today = friday
        )

        assertNotNull(body)
        assertTrue(body!!.contains("1 habit terjadwal"))

        // If all 3 subtasks are completed today, reminder is suppressed
        val dailyHabitDone = dailyHabitUnchecked.copy(
            historyJson = """{"2026-11-27":[{"id":"s1"},{"id":"s2"},{"id":"s3"}]}"""
        )
        val doneBody = DailyOsNotificationHelper.resolveScheduledReminderBody(
            requestCode = DailyOsNotificationHelper.REQ_HABIT,
            profileName = "Wisnu",
            habits = listOf(dailyHabitDone, weekendOnlyHabit, archivedHabit),
            todos = emptyList(),
            today = friday
        )
        assertNull(doneBody)

        // If only unscheduled habits exist today, reminder is suppressed
        val offDayBody = DailyOsNotificationHelper.resolveScheduledReminderBody(
            requestCode = DailyOsNotificationHelper.REQ_HABIT,
            profileName = "Wisnu",
            habits = listOf(weekendOnlyHabit, archivedHabit),
            todos = emptyList(),
            today = friday
        )
        assertNull(offDayBody)
    }

    @Test
    fun habitReminderReportsPartialDailyProgressWhenSomeHabitsAreDone() {
        val doneHabit = DailyOsHabitEntity(
            id = "h1",
            name = "Minum Air",
            historyJson = """{"2026-11-27":[{"id":"main"}]}"""
        )
        val pendingHabit = DailyOsHabitEntity(
            id = "h2",
            name = "Baca Buku",
            historyJson = "{}"
        )

        val body = DailyOsNotificationHelper.resolveScheduledReminderBody(
            requestCode = DailyOsNotificationHelper.REQ_HABIT,
            profileName = "Wisnu",
            habits = listOf(doneHabit, pendingHabit),
            todos = emptyList(),
            today = friday
        )

        assertEquals(
            "Wisnu, masih ada 1 dari 2 habit hari ini yang belum selesai. Yuk lanjutkan!",
            body
        )
    }
}
