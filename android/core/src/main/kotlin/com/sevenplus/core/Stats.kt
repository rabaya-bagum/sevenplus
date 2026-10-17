package com.sevenplus.core

import java.time.LocalDate
import java.time.ZoneId

data class DayMinutes(val date: LocalDate, val minutes: Int)

/** Progress numbers from session history. Port of iOS `Stats` + `SessionStore.streak`. */
object Stats {
    /** Consecutive days with a completed workout, ending today or yesterday. */
    fun streak(sessions: List<Session>, today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): Int = TODO("core")
    /** Longest run of consecutive days with a completed workout. */
    fun bestStreak(sessions: List<Session>, zone: ZoneId = ZoneId.systemDefault()): Int = TODO("core")
    /** Minutes trained on each of the last [days] days, oldest first, including today. */
    fun dailyMinutes(sessions: List<Session>, days: Int = 7, today: LocalDate = LocalDate.now(),
                     zone: ZoneId = ZoneId.systemDefault()): List<DayMinutes> = TODO("core")
    fun totalMinutes(sessions: List<Session>): Int = TODO("core")
}
