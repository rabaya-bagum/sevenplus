package com.sevenplus.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class StatsTest {
    private fun s(daysAgo: Int, seconds: Int = 420, completed: Boolean = true) =
        T.session(T.NOW_MILLIS - daysAgo * T.DAY_MILLIS, seconds, completed)

    @Test fun dailyMinutesLastSevenDays() {
        val days = Stats.dailyMinutes(listOf(s(0), s(0, seconds = 300), s(2), s(9)), today = T.TODAY, zone = T.ZONE)
        assertEquals(7, days.size)
        assertEquals("two sessions today: 7 + 5", 12, days.last().minutes)
        assertEquals("two days ago", 7, days[4].minutes)
        assertEquals("9 days ago is outside the window", 19, days.sumOf { it.minutes })
        assertEquals(T.TODAY.minusDays(6), days.first().date)
    }

    @Test fun totalMinutes() = assertEquals(17, Stats.totalMinutes(listOf(s(0), s(1, seconds = 600))))

    @Test fun bestStreakIgnoresIncompleteAndFindsLongestRun() {
        val sessions = listOf(s(10), s(9), s(8), s(7), s(3), s(2, completed = false), s(1), s(0))
        assertEquals(4, Stats.bestStreak(sessions, T.ZONE))
        assertEquals(0, Stats.bestStreak(emptyList(), T.ZONE))
    }

    @Test fun libraryHidesSwapOnlyMoves() {
        val ids = ExerciseLibrary.browsable.map { it.id }
        assertEquals(18, ids.size)
        assertFalse("step-jacks" in ids)
    }
}
