package com.sevenplus.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineTest {
    private val tl = Timeline(ExerciseLibrary.classic)

    @Test fun totalDropsFinalRest() = assertEquals(5 + 12 * 40 - 10, tl.totalSeconds)

    @Test fun getReady() {
        assertEquals(Phase.GetReady(5), tl.phaseAt(0))
        assertEquals(Phase.GetReady(1), tl.phaseAt(4))
    }

    @Test fun firstWorkAndRest() {
        assertEquals(Phase.Work(0, 30), tl.phaseAt(5))
        assertEquals(Phase.Work(0, 1), tl.phaseAt(34))
        assertEquals(Phase.Rest(0, 10), tl.phaseAt(35))
        assertEquals(Phase.Work(1, 30), tl.phaseAt(45))
    }

    @Test fun lastExerciseThenFinished() {
        assertEquals(Phase.Work(11, 30), tl.phaseAt(tl.startOfWork(11)))
        assertEquals(Phase.Finished, tl.phaseAt(tl.totalSeconds))
        assertEquals(Phase.Finished, tl.phaseAt(10_000))
    }

    @Test fun quietModeSwapsHighImpact() {
        val ids = ExerciseLibrary.quiet(ExerciseLibrary.classic).steps.map { it.exerciseId }
        assertFalse("jumping-jacks" in ids)
        assertFalse("high-knees" in ids)
        assertEquals(12, ids.size)
    }

    @Test fun libraryIntegrity() {
        val ids = ExerciseLibrary.all.map { it.id }
        assertEquals("duplicate exercise id", ids.size, ids.toSet().size)
        assertTrue(ids.size >= 20)
        for (ex in ExerciseLibrary.all.filter { it.impact == Impact.HIGH }) {
            val swap = ex.quietSwapId
            assertTrue("${ex.id} has no quiet swap", swap != null)
            assertEquals("${ex.id} swap missing or not low-impact", Impact.LOW, ExerciseLibrary.all.firstOrNull { it.id == swap }?.impact)
        }
    }

    @Test fun libraryIdsUniqueAndAllHaveTraits() {
        val ids = ExerciseLibrary.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        for (id in ids) assertTrue("$id missing traits", ExerciseTraits.byId.containsKey(id))
    }

    @Test fun engineSurvivesBackgroundingAndSkip() {
        var clock = 0L
        val engine = WorkoutEngine(ExerciseLibrary.classic) { clock }
        engine.start()
        clock += 50_000
        engine.tick()
        assertEquals(Phase.Work(1, 25), engine.phase.value)
        engine.skip()
        assertEquals(Phase.Work(2, 30), engine.phase.value)
        assertEquals(listOf("wall-sit"), engine.skipped)
        engine.pause()
        assertTrue(engine.isPaused.value)
        clock += 600_000
        engine.tick()
        assertEquals(Phase.Work(2, 30), engine.phase.value)
    }

    @Test fun elapsedTruncatesToWholeSeconds() {
        var clock = 1_000L
        val engine = WorkoutEngine(ExerciseLibrary.classic) { clock }
        engine.start()
        clock += 5_999
        assertEquals(5, engine.elapsedSeconds)
        engine.tick()
        assertEquals(Phase.Work(0, 30), engine.phase.value)
    }

    @Test fun onPhaseChangeFiresOnStartAndStageChangesOnly() {
        var clock = 0L
        val engine = WorkoutEngine(ExerciseLibrary.classic) { clock }
        val events = mutableListOf<Phase>()
        engine.onPhaseChange = { events.add(it) }
        engine.start()
        clock += 1_000; engine.tick()      // GetReady(4): same stage
        clock += 4_000; engine.tick()      // Work(0)
        clock += 1_000; engine.tick()      // Work(0, 29): same stage
        assertEquals(listOf(Phase.GetReady(5), Phase.Work(0, 30)), events)
    }

    @Test fun streak() {
        fun d(n: Int) = T.session(T.NOW_MILLIS - n * T.DAY_MILLIS)
        assertEquals(3, Stats.streak(listOf(d(0), d(1), d(2)), T.TODAY, T.ZONE))
        assertEquals(2, Stats.streak(listOf(d(1), d(2)), T.TODAY, T.ZONE))
        assertEquals(1, Stats.streak(listOf(d(0), d(2)), T.TODAY, T.ZONE))
        assertEquals(0, Stats.streak(listOf(d(3)), T.TODAY, T.ZONE))
    }
}
