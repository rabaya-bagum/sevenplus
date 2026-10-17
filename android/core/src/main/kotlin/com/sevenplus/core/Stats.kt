package com.sevenplus.core

import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId

data class DayMinutes(val date: LocalDate, val minutes: Int)

/** Progress numbers from session history. Port of iOS `Stats` + `SessionStore.streak`. */
object Stats {
    /** Consecutive days with a completed workout, ending today or yesterday. */
    fun streak(sessions: List<Session>, today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): Int {
        val days = sessions.filter { it.completed }.map { day(it, zone) }.toSet()
        var day = today
        if (day !in days) day = day.minusDays(1)
        var count = 0
        while (day in days) {
            count += 1
            day = day.minusDays(1)
        }
        return count
    }
    /** Longest run of consecutive days with a completed workout. */
    fun bestStreak(sessions: List<Session>, zone: ZoneId = ZoneId.systemDefault()): Int {
        val days = sessions.filter { it.completed }.map { day(it, zone) }.toSet().sorted()
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        for (d in days) {
            run = if (previous != null && previous.plusDays(1) == d) run + 1 else 1
            best = maxOf(best, run)
            previous = d
        }
        return best
    }
    /** Minutes trained on each of the last [days] days, oldest first, including today. */
    fun dailyMinutes(sessions: List<Session>, days: Int = 7, today: LocalDate = LocalDate.now(),
                     zone: ZoneId = ZoneId.systemDefault()): List<DayMinutes> {
        val seconds = mutableMapOf<LocalDate, Int>()
        for (s in sessions) seconds[day(s, zone)] = (seconds[day(s, zone)] ?: 0) + s.durationSeconds
        return (0 until days).reversed().map { back ->
            val d = today.minusDays(back.toLong())
            DayMinutes(d, (seconds[d] ?: 0) / 60)
        }
    }

    fun totalMinutes(sessions: List<Session>): Int = sessions.sumOf { it.durationSeconds } / 60

    private fun day(s: Session, zone: ZoneId): LocalDate = Instant.ofEpochMilli(s.dateMillis).atZone(zone).toLocalDate()
}
