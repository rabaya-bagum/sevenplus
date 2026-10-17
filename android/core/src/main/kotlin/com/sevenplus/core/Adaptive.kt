package com.sevenplus.core

import kotlinx.serialization.Serializable

/** How much to change the level's base intervals, learned from difficulty ratings. Port of iOS `Adaptation`. */
data class Adaptation(val workDelta: Int = 0, val restDelta: Int = 0) {
    fun apply(base: Intervals): Intervals =
        Intervals(clamp(base.work + workDelta, 15..60), clamp(base.rest + restDelta, 5..30))
    /** e.g. "Adjusted from your feedback: work +5 s, rest -5 s"; null when unchanged. */
    val summary: String?
        get() {
            if (this == Adaptation()) return null
            fun signed(v: Int) = if (v > 0) "+$v" else "$v"
            return "Adjusted from your feedback: work ${signed(workDelta)} s, rest ${signed(restDelta)} s"
        }

    companion object {
        val WORK_RANGE = -10..20
        val REST_RANGE = -5..10
        const val STEP = 5
        /** Too hard: -5 work, +5 rest. Two too-easy in a row: +5 work, -5 rest. Just right resets. */
        fun fromRatings(ratings: List<Difficulty>): Adaptation {
            var work = 0
            var rest = 0
            var easyRun = 0
            for (r in ratings) {
                when (r) {
                    Difficulty.TOO_HARD -> {
                        easyRun = 0
                        work = clamp(work - STEP, WORK_RANGE)
                        rest = clamp(rest + STEP, REST_RANGE)
                    }
                    Difficulty.TOO_EASY -> {
                        easyRun += 1
                        if (easyRun == 2) {
                            easyRun = 0
                            work = clamp(work + STEP, WORK_RANGE)
                            rest = clamp(rest - STEP, REST_RANGE)
                        }
                    }
                    Difficulty.JUST_RIGHT -> easyRun = 0
                }
            }
            return Adaptation(work, rest)
        }
        /** Last 10 rated sessions, oldest first. */
        fun fromSessions(sessions: List<Session>): Adaptation {
            val rated = sessions.filter { it.difficulty != null }.sortedBy { it.dateMillis }
            return fromRatings(rated.takeLast(10).mapNotNull { it.difficulty })
        }

        private fun clamp(v: Int, r: IntRange): Int = minOf(maxOf(v, r.first), r.last)
    }
}

@Serializable
data class Readiness(val energy: Energy, val soreness: Soreness) {
    @Serializable enum class Energy { LOW, OK, HIGH }
    @Serializable enum class Soreness { NONE, SOME, VERY }

    /** Low energy: one length step shorter (never below 5). Low + very sore: 5 minutes. */
    fun adjustedLength(minutes: Int, lengths: List<Int> = WorkoutBuilder.LENGTHS): Int {
        if (energy == Energy.LOW && soreness == Soreness.VERY) return lengths.firstOrNull() ?: 5
        val i = lengths.indexOf(minutes)
        if (energy != Energy.LOW || i <= 0) return minutes
        return lengths[i - 1]
    }
    /** Very sore adds 5 s of rest. */
    val extraRest: Int get() = if (soreness == Soreness.VERY) 5 else 0
    val note: String?
        get() = when {
            energy == Energy.LOW && soreness == Soreness.VERY -> "Taking it easy today: a short, gentle session."
            energy == Energy.LOW -> "Low energy, so today's workout is a bit shorter."
            soreness == Soreness.VERY -> "Extra rest between exercises today."
            energy == Energy.HIGH && soreness == Soreness.NONE -> "Feeling great. Go get it!"
            else -> null
        }
}
