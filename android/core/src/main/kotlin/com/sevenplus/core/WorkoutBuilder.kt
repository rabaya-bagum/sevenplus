package com.sevenplus.core

import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
    fun safePool(limits: Set<Limitation>): List<Exercise> =
        ExerciseLibrary.all.filter { ex -> ExerciseTraits.of(ex.id).stresses.none { it in limits } }

    /** Same-area, safe moves other than [exerciseId]; low impact first, then by name. */
    fun swapOptions(exerciseId: String, limits: Set<Limitation>): List<Exercise> {
        val area = ExerciseTraits.of(exerciseId).area
        return safePool(limits)
            .filter { it.id != exerciseId && ExerciseTraits.of(it.id).area == area }
            .sortedWith(compareBy<Exercise>({ if (it.impact == Impact.LOW) 0 else 1 }, { it.name }))
    }

    /** [intervals] overrides the level's work/rest (used by the adaptive engine). */
    fun build(profile: Profile, minutes: Int? = null, dayIndex: Int = 0, intervals: Intervals? = null): Workout {
        val mins = minutes ?: profile.minutes
        val (work, rest) = intervals ?: profile.level.intervals
        val count = maxOf(3, (mins * 60 + rest) / (work + rest)) // final rest is dropped

        val pool = safePool(profile.limitations)
        // Prefer full moves; fall back to swap-only moves for an area that has none left.
        val full = pool.filter { !ExerciseTraits.of(it.id).swapOnly }
        val order = listOf(BodyArea.CARDIO, BodyArea.LOWER, BodyArea.UPPER, BodyArea.CORE)
        val queues = mutableMapOf<BodyArea, List<Exercise>>()
        for (area in order) {
            var list = full.filter { ExerciseTraits.of(it.id).area == area }
            if (list.isEmpty()) list = pool.filter { ExerciseTraits.of(it.id).area == area }
            if (list.isNotEmpty()) {
                val shift = dayIndex % list.size // rotate for day-to-day variety
                list = list.subList(shift, list.size) + list.subList(0, shift)
            }
            queues[area] = list
        }
        val areas = order.filter { !queues[it].isNullOrEmpty() }
        if (areas.isEmpty()) return ExerciseLibrary.classic

        val steps = mutableListOf<WorkoutStep>()
        var round = 0
        while (steps.size < count) {
            for (area in areas) {
                if (steps.size >= count) continue
                val list = queues.getValue(area)
                val ex = list[round % list.size]
                steps.add(WorkoutStep(ex.id, work, rest))
            }
            round += 1
        }
        return Workout(id = "daily-$dayIndex-$mins", name = "$mins-Minute ${profile.goal.title}", steps = steps)
    }

    /** Days since 2001-01-01, used to vary the daily workout. */
    fun dayIndex(date: LocalDate = LocalDate.now()): Int =
        ChronoUnit.DAYS.between(LocalDate.of(2001, 1, 1), date).toInt()
}

/** Custom workout helpers (port of the static functions in iOS `CustomWorkoutEditor`). */
object CustomWorkouts {
    /** The exercise list repeated [rounds] times; blank name becomes "My workout". */
    fun make(id: String, name: String, exerciseIds: List<String>, work: Int, rest: Int, rounds: Int): Workout {
        val ids = (0 until maxOf(1, rounds)).flatMap { exerciseIds }
        // Swift trims `.whitespaces` (spaces and tabs, not newlines).
        val blank = name.all { it == '\t' || Character.getType(it) == Character.SPACE_SEPARATOR.toInt() }
        return Workout(id = id, name = if (blank) "My workout" else name,
            steps = ids.map { WorkoutStep(it, work, rest) })
    }

    /** How many times a saved list repeats, so editing shows the original list. */
    fun detectRounds(ids: List<String>): Int {
        if (ids.isEmpty()) return 1
        for (r in ids.size downTo 2) {
            if (ids.size % r != 0) continue
            val size = ids.size / r
            val first = ids.subList(0, size)
            if ((0 until r).all { ids.subList(it * size, (it + 1) * size) == first }) return r
        }
        return 1
    }
}
