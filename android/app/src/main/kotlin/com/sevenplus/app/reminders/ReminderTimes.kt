package com.sevenplus.app.reminders

import com.sevenplus.core.ReminderSettings
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/** Pure trigger-time math for weekly reminders. Weekdays use iOS numbering: 1 = Sunday ... 7 = Saturday. */
object ReminderTimes {
    fun dayOfWeek(iosWeekday: Int): DayOfWeek = DayOfWeek.of(((iosWeekday + 5) % 7) + 1)

    /** Next occurrence strictly after [now] of [iosWeekday] at hour:minute, in now's zone. */
    fun next(now: ZonedDateTime, iosWeekday: Int, hour: Int, minute: Int): ZonedDateTime {
        val time = LocalTime.of(hour, minute)
        val day = dayOfWeek(iosWeekday)
        var date = now.toLocalDate().with(TemporalAdjusters.nextOrSame(day))
        var candidate = ZonedDateTime.of(date, time, now.zone)
        if (!candidate.isAfter(now)) {
            date = date.plusWeeks(1)
            candidate = ZonedDateTime.of(date, time, now.zone)
        }
        return candidate
    }

    /** Next trigger per selected weekday (sorted, soonest first); empty when disabled. */
    fun next(now: ZonedDateTime, settings: ReminderSettings): List<ZonedDateTime> =
        if (!settings.enabled) emptyList()
        else settings.weekdays.filter { it in 1..7 }.map { next(now, it, settings.hour, settings.minute) }.sorted()
}
