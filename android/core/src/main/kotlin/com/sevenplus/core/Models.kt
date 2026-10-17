package com.sevenplus.core

import kotlinx.serialization.Serializable

@Serializable
enum class Impact { LOW, HIGH }

@Serializable
data class Exercise(
    val id: String,
    val name: String,
    /** One-line form cue spoken by the coach. */
    val cue: String,
    val impact: Impact,
    /** Low-impact alternative used in quiet mode. */
    val quietSwapId: String? = null,
)

@Serializable
data class WorkoutStep(val exerciseId: String, val workSeconds: Int, val restSeconds: Int)

@Serializable
data class Workout(val id: String, val name: String, val steps: List<WorkoutStep>) {
    val totalSeconds: Int get() = steps.sumOf { it.workSeconds + it.restSeconds }

    /** Same workout with step [index] doing a different exercise (timing unchanged). */
    fun replacing(index: Int, exerciseId: String): Workout =
        copy(steps = steps.mapIndexed { i, s -> if (i == index) s.copy(exerciseId = exerciseId) else s })
}

@Serializable
enum class Difficulty(val label: String) { TOO_EASY("Too easy"), JUST_RIGHT("Just right"), TOO_HARD("Too hard") }

@Serializable
data class Session(
    val id: String,
    val workoutId: String,
    /** When the workout ended, epoch milliseconds. */
    val dateMillis: Long,
    val durationSeconds: Int,
    val completed: Boolean,
    val skipped: List<String> = emptyList(),
    val difficulty: Difficulty? = null,
    val calories: Int? = null,
)
