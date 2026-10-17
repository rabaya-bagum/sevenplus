package com.sevenplus.core

import kotlinx.coroutines.flow.MutableStateFlow
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
    val totalSeconds: Int get() = getReadySeconds + workout.totalSeconds - (workout.steps.lastOrNull()?.restSeconds ?: 0)

    fun phaseAt(elapsed: Int): Phase {
        var t = elapsed
        if (t < getReadySeconds) return Phase.GetReady(getReadySeconds - t)
        t -= getReadySeconds
        for ((i, step) in workout.steps.withIndex()) {
            if (t < step.workSeconds) return Phase.Work(i, step.workSeconds - t)
            t -= step.workSeconds
            if (i == workout.steps.size - 1) return Phase.Finished
            if (t < step.restSeconds) return Phase.Rest(i, step.restSeconds - t)
            t -= step.restSeconds
        }
        return Phase.Finished
    }

    /** Elapsed second at which exercise [index] starts working. */
    fun startOfWork(index: Int): Int =
        getReadySeconds + workout.steps.take(index).sumOf { it.workSeconds + it.restSeconds }
}

/**
 * Drives the timeline from wall-clock time ([nowMillis]) so it stays right after the app is
 * backgrounded. Not thread-safe: call from the main thread. The UI calls [tick] about every
 * 250 ms while visible (the engine has no timer of its own, which keeps it testable).
 * Port of iOS `WorkoutEngine`.
 */
class WorkoutEngine(workout: Workout, private val nowMillis: () -> Long = System::currentTimeMillis) {
    private var _timeline = Timeline(workout)
    private val _phase = MutableStateFlow(_timeline.phaseAt(0))
    private val _isPaused = MutableStateFlow(false)
    private val _workout = MutableStateFlow(workout)
    private val _skipped = mutableListOf<String>()
    private val _swaps = mutableMapOf<String, String>()
    private var startMillis: Long? = null
    /** Accumulated elapsed before the current run. */
    private var offsetMillis: Long = 0

    val phase: StateFlow<Phase> get() = _phase
    val isPaused: StateFlow<Boolean> get() = _isPaused
    /** Current (possibly swapped) workout. */
    val workout: StateFlow<Workout> get() = _workout
    val timeline: Timeline get() = _timeline
    val skipped: List<String> get() = _skipped.toList()
    /** original exercise id -> replacement id */
    val swaps: Map<String, String> get() = _swaps.toMap()
    val elapsedSeconds: Int
        get() {
            val running = startMillis?.let { nowMillis() - it } ?: 0L
            return Math.floorDiv(offsetMillis + running, 1000L).toInt()
        }
    val progress: Float get() = minOf(1f, elapsedSeconds.toFloat() / _timeline.totalSeconds.toFloat())
    /** Called when the stage changes (not on every second): voice coach, watch, etc. */
    var onPhaseChange: ((Phase) -> Unit)? = null

    fun start() {
        if (startMillis != null) return
        startMillis = nowMillis()
        _isPaused.value = false
        onPhaseChange?.invoke(_phase.value)
    }

    fun pause() {
        val s = startMillis ?: return
        offsetMillis += nowMillis() - s
        startMillis = null
        _isPaused.value = true
    }

    fun resume() = start()

    /** Jump to the next exercise (records the current one as skipped if mid-work). */
    fun skip() {
        val next = when (val p = _phase.value) {
            is Phase.GetReady -> 0
            is Phase.Work -> {
                _skipped.add(_timeline.workout.steps[p.index].exerciseId)
                p.index + 1
            }
            is Phase.Rest -> p.index + 1
            Phase.Finished -> return
        }
        val target = if (next >= _timeline.workout.steps.size) _timeline.totalSeconds else _timeline.startOfWork(next)
        offsetMillis = target * 1000L
        if (startMillis != null) startMillis = nowMillis()
        tick()
    }

    fun tick() {
        val p = _timeline.phaseAt(elapsedSeconds)
        val old = _phase.value
        if (p == old) return
        val changedStage = !sameStage(p, old)
        _phase.value = p
        if (changedStage) onPhaseChange?.invoke(p)
        if (p == Phase.Finished) startMillis = null
    }

    /** Current exercise while working/getting ready, the next one while resting; null when finished. */
    val swapTargetIndex: Int?
        get() = when (val p = _phase.value) {
            is Phase.GetReady -> 0
            is Phase.Work -> p.index
            is Phase.Rest -> p.index + 1
            Phase.Finished -> null
        }

    /** Replace one exercise without changing timing; re-announces the stage. */
    fun swap(exerciseId: String) {
        val i = swapTargetIndex ?: return
        val current = _timeline.workout
        if (i >= current.steps.size) return
        val original = current.steps[i].exerciseId
        if (original == exerciseId) return
        val key = _swaps.entries.firstOrNull { it.value == original }?.key ?: original
        _swaps[key] = exerciseId
        _timeline = Timeline(current.replacing(i, exerciseId), _timeline.getReadySeconds)
        _workout.value = _timeline.workout
        onPhaseChange?.invoke(_phase.value)
    }

    private fun sameStage(a: Phase, b: Phase): Boolean = when {
        a is Phase.GetReady && b is Phase.GetReady -> true
        a is Phase.Finished && b is Phase.Finished -> true
        a is Phase.Work && b is Phase.Work -> a.index == b.index
        a is Phase.Rest && b is Phase.Rest -> a.index == b.index
        else -> false
    }
}

/** What the voice coach says when a stage starts (port of iOS `Coach.announce`). */
object CoachScript {
    fun line(phase: Phase, workout: Workout): String = when (phase) {
        is Phase.GetReady -> "Get ready. First up, ${ExerciseLibrary.exercise(workout.steps[0].exerciseId).name}."
        is Phase.Work -> {
            val ex = ExerciseLibrary.exercise(workout.steps[phase.index].exerciseId)
            "${ex.name}. ${ex.cue}"
        }
        is Phase.Rest -> "Rest. Next, ${ExerciseLibrary.exercise(workout.steps[phase.index + 1].exerciseId).name}."
        Phase.Finished -> "Workout complete. Great job!"
    }
}
