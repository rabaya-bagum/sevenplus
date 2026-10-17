package com.sevenplus.app.ui.common

import com.sevenplus.app.contracts.ProductInfo
import com.sevenplus.core.Adaptation
import com.sevenplus.core.DayMinutes
import com.sevenplus.core.Gate
import com.sevenplus.core.Intervals
import com.sevenplus.core.Phase
import com.sevenplus.core.Profile
import com.sevenplus.core.Readiness
import com.sevenplus.core.Session
import com.sevenplus.core.Workout
import com.sevenplus.core.WorkoutBuilder
import com.sevenplus.core.intervals
import java.time.DayOfWeek
import java.time.LocalDate

/** "m:ss", as on the iOS summary. */
fun formatClock(seconds: Int): String {
    val s = maxOf(0, seconds)
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

/** Today's workout plan (port of the derived state in iOS TodayView). */
data class TodayPlan(
    val minutes: Int,
    val intervals: Intervals,
    val adaptation: Adaptation,
    val workout: Workout,
) {
    companion object {
        fun compute(
            profile: Profile,
            chosenMinutes: Int?,
            premium: Boolean,
            readiness: Readiness?,
            sessions: List<Session>,
            today: LocalDate = LocalDate.now(),
        ): TodayPlan {
            val m = chosenMinutes ?: profile.minutes
            val effective = if (Gate.canUseLength(m, premium)) m else 7
            val planned = if (chosenMinutes == null && readiness != null) readiness.adjustedLength(effective) else effective
            // Adaptive difficulty is a Premium feature.
            val adaptation = if (premium) Adaptation.fromSessions(sessions) else Adaptation()
            val base = adaptation.apply(profile.level.intervals)
            val intervals = Intervals(base.work, minOf(30, base.rest + (readiness?.extraRest ?: 0)))
            val workout = WorkoutBuilder.build(profile, planned, WorkoutBuilder.dayIndex(today), intervals)
            return TodayPlan(planned, intervals, adaptation, workout)
        }
    }
}

/** "10, 15, 20 min with Premium", or null when nothing is locked. */
fun lockedLengthsLabel(premium: Boolean): String? {
    val locked = WorkoutBuilder.LENGTHS.filter { !Gate.canUseLength(it, premium) }
    return if (locked.isEmpty()) null else "${locked.joinToString(", ")} min with Premium"
}

/** Bar heights 0..1 for the 7-day chart. */
fun barFractions(days: List<DayMinutes>): List<Float> {
    val max = days.maxOfOrNull { it.minutes } ?: 0
    if (max <= 0) return days.map { 0f }
    return days.map { it.minutes.toFloat() / max }
}

/** iOS weekday numbering (1 = Sunday ... 7 = Saturday) to java.time. */
fun iosWeekdayToDayOfWeek(day: Int): DayOfWeek = DayOfWeek.of(((day + 5) % 7) + 1)

/** "CA$59.99 / year", with a trial prefix like iOS. */
fun priceLine(p: ProductInfo): String {
    var line = p.price + if (p.period == "year") " / year" else " / month"
    val trial = p.freeTrialDays
    if (trial != null && trial > 0) {
        val text = if (trial % 7 == 0) "${trial / 7}-week" else "$trial-day"
        line = "$text free trial, then $line"
    }
    return line
}

/** Player display helpers. */
object PlayerText {
    fun title(phase: Phase, workout: Workout, exName: (String) -> String): String = when (phase) {
        is Phase.GetReady -> "Get ready"
        is Phase.Work -> exName(workout.steps[phase.index].exerciseId)
        is Phase.Rest -> "Rest"
        Phase.Finished -> "Done!"
    }

    fun stepLabel(phase: Phase, workout: Workout): String = when (phase) {
        is Phase.Work -> "${phase.index + 1} of ${workout.steps.size}"
        is Phase.Rest -> "${phase.index + 1} of ${workout.steps.size}"
        else -> ""
    }

    fun remaining(phase: Phase): Int = when (phase) {
        is Phase.GetReady -> phase.remaining
        is Phase.Work -> phase.remaining
        is Phase.Rest -> phase.remaining
        Phase.Finished -> 0
    }

    fun phaseLength(phase: Phase, workout: Workout, getReady: Int): Int = when (phase) {
        is Phase.GetReady -> getReady
        is Phase.Work -> workout.steps[phase.index].workSeconds
        is Phase.Rest -> workout.steps[phase.index].restSeconds
        Phase.Finished -> 1
    }

    fun ringFraction(phase: Phase, workout: Workout, getReady: Int): Float =
        remaining(phase).toFloat() / maxOf(1, phaseLength(phase, workout, getReady)).toFloat()
}

fun shareSummaryText(durationSeconds: Int, streak: Int): String {
    val mins = maxOf(1, durationSeconds / 60)
    val s = if (streak > 1) " $streak-day streak!" else ""
    return "Just finished a $mins-minute workout with SevenPlus.$s"
}

/** Privacy link only when it is a real https URL (iOS LegalLinks). */
fun privacyPolicyUrl(raw: String): String? = raw.takeIf { it.startsWith("https://") }

const val TERMS_URL = "https://play.google.com/about/play-terms/"
const val MANAGE_SUBSCRIPTIONS_URL = "https://play.google.com/store/account/subscriptions"
