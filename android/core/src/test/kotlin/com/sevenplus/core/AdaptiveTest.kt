package com.sevenplus.core

import com.sevenplus.core.Difficulty.JUST_RIGHT
import com.sevenplus.core.Difficulty.TOO_EASY
import com.sevenplus.core.Difficulty.TOO_HARD
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveTest {
    @Test fun noRatingsNoChange() {
        assertEquals(Adaptation(), Adaptation.fromRatings(emptyList()))
        assertNull(Adaptation().summary)
    }

    @Test fun tooHardEasesImmediately() {
        val a = Adaptation.fromRatings(listOf(TOO_HARD))
        assertEquals(Adaptation(-5, 5), a)
        assertEquals(25, a.apply(Intervals(30, 10)).work)
        assertEquals(15, a.apply(Intervals(30, 10)).rest)
        assertEquals("Adjusted from your feedback: work -5 s, rest +5 s", a.summary)
    }

    @Test fun tooEasyNeedsTwoInARow() {
        assertEquals(Adaptation(), Adaptation.fromRatings(listOf(TOO_EASY)))
        assertEquals("run was broken", Adaptation(), Adaptation.fromRatings(listOf(TOO_EASY, JUST_RIGHT, TOO_EASY)))
        assertEquals(Adaptation(5, -5), Adaptation.fromRatings(listOf(TOO_EASY, TOO_EASY)))
    }

    @Test fun deltasAreBounded() {
        val hard = Adaptation.fromRatings(List(10) { TOO_HARD })
        assertEquals(Adaptation.WORK_RANGE.first, hard.workDelta)
        assertEquals(Adaptation.REST_RANGE.last, hard.restDelta)
        val easy = Adaptation.fromRatings(List(20) { TOO_EASY })
        assertEquals(Adaptation.WORK_RANGE.last, easy.workDelta)
        assertEquals(Adaptation.REST_RANGE.first, easy.restDelta)
        assertTrue(easy.apply(Intervals(30, 10)).rest >= 5)
        assertTrue(hard.apply(Intervals(30, 15)).work >= 15)
    }

    @Test fun usesOnlyLastTenRatedSessionsOldestFirst() {
        val base = 1_000_000_000L
        fun s(day: Int, d: Difficulty?) = T.session(base + day * T.DAY_MILLIS, 400, true, d)
        val sessions = (0 until 10).map { s(it, TOO_HARD) } + (10 until 20).map { s(it, JUST_RIGHT) } + s(25, null)
        assertEquals(Adaptation(), Adaptation.fromSessions(sessions.shuffled(kotlin.random.Random(42))))
    }

    @Test fun readinessLength() {
        assertEquals(7, Readiness(Readiness.Energy.LOW, Readiness.Soreness.NONE).adjustedLength(10))
        assertEquals(5, Readiness(Readiness.Energy.LOW, Readiness.Soreness.NONE).adjustedLength(5))
        assertEquals(5, Readiness(Readiness.Energy.LOW, Readiness.Soreness.VERY).adjustedLength(20))
        assertEquals(15, Readiness(Readiness.Energy.HIGH, Readiness.Soreness.VERY).adjustedLength(15))
        assertEquals(5, Readiness(Readiness.Energy.OK, Readiness.Soreness.VERY).extraRest)
        assertEquals(0, Readiness(Readiness.Energy.OK, Readiness.Soreness.SOME).extraRest)
    }

    @Test fun readinessNotes() {
        assertEquals("Taking it easy today: a short, gentle session.", Readiness(Readiness.Energy.LOW, Readiness.Soreness.VERY).note)
        assertEquals("Low energy, so today's workout is a bit shorter.", Readiness(Readiness.Energy.LOW, Readiness.Soreness.SOME).note)
        assertEquals("Extra rest between exercises today.", Readiness(Readiness.Energy.HIGH, Readiness.Soreness.VERY).note)
        assertEquals("Feeling great. Go get it!", Readiness(Readiness.Energy.HIGH, Readiness.Soreness.NONE).note)
        assertNull(Readiness(Readiness.Energy.OK, Readiness.Soreness.NONE).note)
    }

    @Test fun builderUsesIntervalOverride() {
        val w = WorkoutBuilder.build(Profile(level = Level.BEGINNER, minutes = 7), intervals = Intervals(25, 20))
        assertTrue(w.steps.all { it.workSeconds == 25 && it.restSeconds == 20 })
        assertEquals((420 + 20) / 45, w.steps.size)
    }
}
