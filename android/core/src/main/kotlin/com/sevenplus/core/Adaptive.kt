package com.sevenplus.core

import kotlinx.serialization.Serializable

/** How much to change the level's base intervals, learned from difficulty ratings. Port of iOS `Adaptation`. */
data class Adaptation(val workDelta: Int = 0, val restDelta: Int = 0) {
    fun apply(base: Intervals): Intervals = TODO("core")
    /** e.g. "Adjusted from your feedback: work +5 s, rest -5 s"; null when unchanged. */
    val summary: String? get() = TODO("core")

    companion object {
        val WORK_RANGE = -10..20
        val REST_RANGE = -5..10
        const val STEP = 5
        /** Too hard: -5 work, +5 rest. Two too-easy in a row: +5 work, -5 rest. Just right resets. */
        fun fromRatings(ratings: List<Difficulty>): Adaptation = TODO("core")
        /** Last 10 rated sessions, oldest first. */
        fun fromSessions(sessions: List<Session>): Adaptation = TODO("core")
    }
}

@Serializable
data class Readiness(val energy: Energy, val soreness: Soreness) {
    @Serializable enum class Energy { LOW, OK, HIGH }
    @Serializable enum class Soreness { NONE, SOME, VERY }

    /** Low energy: one length step shorter (never below 5). Low + very sore: 5 minutes. */
    fun adjustedLength(minutes: Int, lengths: List<Int> = WorkoutBuilder.LENGTHS): Int = TODO("core")
    /** Very sore adds 5 s of rest. */
    val extraRest: Int get() = TODO("core")
    val note: String? get() = TODO("core")
}
