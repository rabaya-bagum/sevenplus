package com.sevenplus.core

import java.time.LocalDate

/** Work/rest seconds per exercise. */
data class Intervals(val work: Int, val rest: Int)

val Level.intervals: Intervals get() = Intervals(work, rest)

/**
 * Builds the day's workout from the profile. Pure and deterministic: the same profile and
 * day always give the same workout. Port of iOS `WorkoutBuilder.swift`.
 */
object WorkoutBuilder {
    val LENGTHS: List<Int> = listOf(5, 7, 10, 15, 20)

    /** Exercises safe for every one of the user's limitations. */
    fun safePool(limits: Set<Limitation>): List<Exercise> = TODO("core")

    /** Same-area, safe moves other than [exerciseId]; low impact first, then by name. */
    fun swapOptions(exerciseId: String, limits: Set<Limitation>): List<Exercise> = TODO("core")

    /** [intervals] overrides the level's work/rest (used by the adaptive engine). */
    fun build(profile: Profile, minutes: Int? = null, dayIndex: Int = 0, intervals: Intervals? = null): Workout =
        TODO("core")

    /** Days since 2001-01-01, used to vary the daily workout. */
    fun dayIndex(date: LocalDate = LocalDate.now()): Int = TODO("core")
}

/** Custom workout helpers (port of the static functions in iOS `CustomWorkoutEditor`). */
object CustomWorkouts {
    /** The exercise list repeated [rounds] times; blank name becomes "My workout". */
    fun make(id: String, name: String, exerciseIds: List<String>, work: Int, rest: Int, rounds: Int): Workout = TODO("core")

    /** How many times a saved list repeats, so editing shows the original list. */
    fun detectRounds(ids: List<String>): Int = TODO("core")
}
