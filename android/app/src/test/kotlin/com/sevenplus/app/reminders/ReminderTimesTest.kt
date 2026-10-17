package com.sevenplus.app.reminders

import com.sevenplus.core.ReminderSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderTimesTest {
    private val toronto = ZoneId.of("America/Toronto")
    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int) = ZonedDateTime.of(y, m, d, h, min, 0, 0, toronto)

    @Test fun iosWeekdayMapping() {
        assertEquals(DayOfWeek.SUNDAY, ReminderTimes.dayOfWeek(1))
        assertEquals(DayOfWeek.MONDAY, ReminderTimes.dayOfWeek(2))
        assertEquals(DayOfWeek.SATURDAY, ReminderTimes.dayOfWeek(7))
    }

    @Test fun nextAcrossWeekBoundary() {
        // Friday 2026-10-16 19:00, reminders Mon/Wed/Fri at 18:00.
        val now = at(2026, 10, 16, 19, 0)
        val result = ReminderTimes.next(now, ReminderSettings(enabled = true, hour = 18, minute = 0, weekdays = setOf(2, 4, 6)))
        assertEquals(listOf(at(2026, 10, 19, 18, 0), at(2026, 10, 21, 18, 0), at(2026, 10, 23, 18, 0)), result)
    }

    @Test fun laterTodayIsToday() {
        val now = at(2026, 10, 16, 17, 59)
        assertEquals(at(2026, 10, 16, 18, 0), ReminderTimes.next(now, 6, 18, 0))
        assertEquals(at(2026, 10, 23, 18, 0), ReminderTimes.next(at(2026, 10, 16, 18, 0), 6, 18, 0))
    }

    @Test fun disabledIsEmpty() {
        assertEquals(emptyList<ZonedDateTime>(), ReminderTimes.next(at(2026, 10, 16, 0, 0), ReminderSettings(enabled = false)))
    }

    @Test fun keepsWallClockAcrossDst() {
        // DST ends Sunday 2026-11-01 in Toronto. Saturday 2026-10-31 18:30 -> next Sunday 08:00 local, EST.
        val now = at(2026, 10, 31, 18, 30)
        val next = ReminderTimes.next(now, 1, 8, 0)
        assertEquals(at(2026, 11, 1, 8, 0), next)
        assertEquals(8, next.hour)
        assertEquals(-5 * 3600, next.offset.totalSeconds)
        // Spring forward: 2026-03-08 02:30 doesn't exist; resolves to 03:30 EDT.
        val gap = ReminderTimes.next(at(2026, 3, 7, 12, 0), 1, 2, 30)
        assertEquals(3, gap.hour)
    }
}
