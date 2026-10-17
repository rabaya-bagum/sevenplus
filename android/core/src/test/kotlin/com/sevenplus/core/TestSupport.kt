package com.sevenplus.core

import java.time.LocalDate
import java.time.ZoneOffset

internal object T {
    val ZONE: ZoneOffset = ZoneOffset.UTC
    /** 1_760_000_000 s = 2025-10-09T08:53:20Z, as in the Swift tests. */
    const val NOW_MILLIS = 1_760_000_000_000L
    val TODAY: LocalDate = LocalDate.of(2025, 10, 9)
    const val DAY_MILLIS = 86_400_000L

    fun session(dateMillis: Long, seconds: Int = 420, completed: Boolean = true, difficulty: Difficulty? = null) =
        Session(id = java.util.UUID.randomUUID().toString(), workoutId = "w", dateMillis = dateMillis,
            durationSeconds = seconds, completed = completed, difficulty = difficulty)
}
