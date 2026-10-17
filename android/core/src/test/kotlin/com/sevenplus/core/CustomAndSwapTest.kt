package com.sevenplus.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CustomAndSwapTest {
    @Test fun swapOptionsSameAreaSafeAndLowImpactFirst() {
        val opts = WorkoutBuilder.swapOptions("jumping-jacks", setOf(Limitation.WRISTS))
        assertFalse(opts.isEmpty())
        assertFalse("jumping-jacks" in opts.map { it.id })
        for (ex in opts) {
            assertEquals(BodyArea.CARDIO, ExerciseTraits.of(ex.id).area)
            assertFalse(Limitation.WRISTS in ExerciseTraits.of(ex.id).stresses)
        }
        assertEquals(Impact.LOW, opts.first().impact)
    }

    @Test fun swapDuringWorkKeepsTimingAndRecordsSwap() {
        var clock = 0L
        val engine = WorkoutEngine(ExerciseLibrary.classic) { clock }
        val events = mutableListOf<Phase>()
        engine.onPhaseChange = { events.add(it) }
        engine.start()
        clock += 15_000
        engine.tick()
        assertEquals(Phase.Work(0, 20), engine.phase.value)
        engine.swap("step-jacks")
        assertEquals("step-jacks", engine.workout.value.steps[0].exerciseId)
        assertEquals("timing unchanged", Phase.Work(0, 20), engine.phase.value)
        assertEquals(Timeline(ExerciseLibrary.classic).totalSeconds, engine.timeline.totalSeconds)
        assertEquals(mapOf("jumping-jacks" to "step-jacks"), engine.swaps)
        assertEquals("swap re-announces", Phase.Work(0, 20), events.last())

        engine.swap("marching")
        assertEquals(mapOf("jumping-jacks" to "marching"), engine.swaps)
    }

    @Test fun swapDuringRestReplacesNextExercise() {
        var clock = 0L
        val engine = WorkoutEngine(ExerciseLibrary.classic) { clock }
        engine.start()
        clock += 37_000
        engine.tick()
        assertEquals(1, engine.swapTargetIndex)
        engine.swap("glute-bridge")
        assertEquals("glute-bridge", engine.workout.value.steps[1].exerciseId)
        assertEquals("jumping-jacks", engine.workout.value.steps[0].exerciseId)
    }

    @Test fun customWorkoutRoundsRoundTrip() {
        val w = CustomWorkouts.make("c1", "  ", listOf("squat", "plank"), 40, 20, 3)
        assertEquals("My workout", w.name)
        assertEquals(6, w.steps.size)
        assertEquals(listOf("squat", "plank", "squat", "plank", "squat", "plank"), w.steps.map { it.exerciseId })
        assertEquals(3, CustomWorkouts.detectRounds(w.steps.map { it.exerciseId }))
        assertEquals(1, CustomWorkouts.detectRounds(listOf("squat", "plank", "lunge")))
        assertEquals(1, CustomWorkouts.detectRounds(emptyList()))
    }
}
