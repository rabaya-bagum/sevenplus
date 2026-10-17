package com.sevenplus.core

import kotlinx.coroutines.flow.StateFlow

/** Where the workout is at a given moment. */
sealed interface Phase {
    data class GetReady(val remaining: Int) : Phase
    data class Work(val index: Int, val remaining: Int) : Phase
    /** Rest *after* step [index]. */
    data class Rest(val index: Int, val remaining: Int) : Phase
    data object Finished : Phase
}

/** Pure timeline: maps elapsed seconds to a phase. The rest after the final exercise is dropped. */
class Timeline(val workout: Workout, val getReadySeconds: Int = 5) {
    val totalSeconds: Int get() = TODO("core")
    fun phaseAt(elapsed: Int): Phase = TODO("core")
    /** Elapsed second at which exercise [index] starts working. */
    fun startOfWork(index: Int): Int = TODO("core")
}

/**
 * Drives the timeline from wall-clock time ([nowMillis]) so it stays right after the app is
 * backgrounded. Not thread-safe: call from the main thread. The UI calls [tick] about every
 * 250 ms while visible (the engine has no timer of its own, which keeps it testable).
 * Port of iOS `WorkoutEngine`.
 */
class WorkoutEngine(workout: Workout, private val nowMillis: () -> Long = System::currentTimeMillis) {
    val phase: StateFlow<Phase> get() = TODO("core")
    val isPaused: StateFlow<Boolean> get() = TODO("core")
    /** Current (possibly swapped) workout. */
    val workout: StateFlow<Workout> get() = TODO("core")
    val timeline: Timeline get() = TODO("core")
    val skipped: List<String> get() = TODO("core")
    /** original exercise id -> replacement id */
    val swaps: Map<String, String> get() = TODO("core")
    val elapsedSeconds: Int get() = TODO("core")
    val progress: Float get() = TODO("core")
    /** Called when the stage changes (not on every second): voice coach, watch, etc. */
    var onPhaseChange: ((Phase) -> Unit)? = null

    fun start(): Unit = TODO("core")
    fun pause(): Unit = TODO("core")
    fun resume(): Unit = TODO("core")
    /** Jump to the next exercise (records the current one as skipped if mid-work). */
    fun skip(): Unit = TODO("core")
    fun tick(): Unit = TODO("core")
    /** Current exercise while working/getting ready, the next one while resting; null when finished. */
    val swapTargetIndex: Int? get() = TODO("core")
    /** Replace one exercise without changing timing; re-announces the stage. */
    fun swap(exerciseId: String): Unit = TODO("core")
}

/** What the voice coach says when a stage starts (port of iOS `Coach.announce`). */
object CoachScript {
    fun line(phase: Phase, workout: Workout): String = TODO("core")
}
