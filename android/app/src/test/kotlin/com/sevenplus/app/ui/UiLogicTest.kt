package com.sevenplus.app.ui

import com.sevenplus.app.contracts.ProductInfo
import com.sevenplus.app.ui.common.PlayerText
import com.sevenplus.app.ui.common.TodayPlan
import com.sevenplus.app.ui.common.barFractions
import com.sevenplus.app.ui.common.formatClock
import com.sevenplus.app.ui.common.iosWeekdayToDayOfWeek
import com.sevenplus.app.ui.common.lockedLengthsLabel
import com.sevenplus.app.ui.common.priceLine
import com.sevenplus.app.ui.common.privacyPolicyUrl
import com.sevenplus.app.ui.common.shareSummaryText
import com.sevenplus.core.DayMinutes
import com.sevenplus.core.Difficulty
import com.sevenplus.core.ExerciseLibrary
import com.sevenplus.core.Level
import com.sevenplus.core.Phase
import com.sevenplus.core.Profile
import com.sevenplus.core.Readiness
import com.sevenplus.core.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class UiLogicTest {
    @Test fun clock() {
        assertEquals("0:00", formatClock(0))
        assertEquals("7:05", formatClock(425))
        assertEquals("12:59", formatClock(779))
    }

    @Test fun lockedLabel() {
        assertEquals("10, 15, 20 min with Premium", lockedLengthsLabel(false))
        assertNull(lockedLengthsLabel(true))
    }

    @Test fun freeUserFallsBackToSevenMinutes() {
        val plan = TodayPlan.compute(Profile(minutes = 15), null, premium = false, readiness = null, sessions = emptyList())
        assertEquals(7, plan.minutes)
        assertEquals(30, plan.intervals.work)
        assertEquals(15, plan.intervals.rest)
    }

    @Test fun readinessShortensAndAddsRest() {
        val r = Readiness(Readiness.Energy.LOW, Readiness.Soreness.SOME)
        assertEquals(5, TodayPlan.compute(Profile(minutes = 7), null, false, r, emptyList()).minutes)
        // A length picked by hand wins over the check-in.
        assertEquals(7, TodayPlan.compute(Profile(minutes = 7), 7, false, r, emptyList()).minutes)
        val sore = Readiness(Readiness.Energy.OK, Readiness.Soreness.VERY)
        assertEquals(20, TodayPlan.compute(Profile(level = Level.BEGINNER), null, false, sore, emptyList()).intervals.rest)
    }

    @Test fun adaptationOnlyForPremium() {
        val hard = (1..2).map { Session("s$it", "w", it.toLong(), 400, true, difficulty = Difficulty.TOO_HARD) }
        assertEquals(30, TodayPlan.compute(Profile(), null, false, null, hard).intervals.work)
        val p = TodayPlan.compute(Profile(), null, true, null, hard)
        assertEquals(20, p.intervals.work)
        assertEquals(25, p.intervals.rest)
    }

    @Test fun chartFractions() {
        val d = LocalDate.of(2026, 10, 1)
        assertEquals(listOf(0f, 0f), barFractions(listOf(DayMinutes(d, 0), DayMinutes(d, 0))))
        assertEquals(listOf(0.5f, 1f, 0f), barFractions(listOf(DayMinutes(d, 5), DayMinutes(d, 10), DayMinutes(d, 0))))
    }

    @Test fun weekdays() {
        assertEquals(DayOfWeek.SUNDAY, iosWeekdayToDayOfWeek(1))
        assertEquals(DayOfWeek.MONDAY, iosWeekdayToDayOfWeek(2))
        assertEquals(DayOfWeek.SATURDAY, iosWeekdayToDayOfWeek(7))
    }

    @Test fun prices() {
        assertEquals("$9.99 / month", priceLine(ProductInfo("m", "Monthly", "$9.99", "month")))
        assertEquals("1-week free trial, then $59.99 / year", priceLine(ProductInfo("y", "Yearly", "$59.99", "year", 7)))
        assertEquals("3-day free trial, then $59.99 / year", priceLine(ProductInfo("y", "Yearly", "$59.99", "year", 3)))
    }

    @Test fun playerText() {
        val w = ExerciseLibrary.classic
        val name = { id: String -> ExerciseLibrary.exercise(id).name }
        assertEquals("Get ready", PlayerText.title(Phase.GetReady(5), w, name))
        assertEquals("Wall Sit", PlayerText.title(Phase.Work(1, 10), w, name))
        assertEquals("2 of 12", PlayerText.stepLabel(Phase.Rest(1, 3), w))
        assertEquals(0.5f, PlayerText.ringFraction(Phase.Work(0, 15), w, 5), 0.0001f)
        assertEquals(0, PlayerText.remaining(Phase.Finished))
    }

    @Test fun misc() {
        assertEquals("Just finished a 7-minute workout with SevenPlus. 3-day streak!", shareSummaryText(425, 3))
        assertEquals("Just finished a 1-minute workout with SevenPlus.", shareSummaryText(20, 1))
        assertNull(privacyPolicyUrl(""))
        assertNull(privacyPolicyUrl("http://x"))
        assertEquals("https://x", privacyPolicyUrl("https://x"))
    }
}
