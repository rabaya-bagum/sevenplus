package com.sevenplus.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class RepCounterTest {
    @Test fun angle() {
        assertEquals(90.0, RepProfile.angle(0.0, 1.0, 0.0, 0.0, 1.0, 0.0), 0.001)
        assertEquals(180.0, RepProfile.angle(-1.0, 0.0, 0.0, 0.0, 1.0, 0.0), 0.001)
        assertEquals(180.0, RepProfile.angle(0.0, 0.0, 0.0, 0.0, 1.0, 0.0), 0.0)
    }

    private fun feed(c: RepCounter, reps: Int, low: Double, high: Double) {
        repeat(reps) {
            var a = high; while (a >= low) { c.update(a); a -= 5 }
            a = low; while (a <= high) { c.update(a); a += 5 }
        }
    }

    @Test fun countsFullSquats() {
        val c = RepCounter(100.0, 160.0); feed(c, 5, 80.0, 175.0); assertEquals(5, c.count)
    }

    @Test fun ignoresShallowReps() {
        val c = RepCounter(100.0, 160.0); feed(c, 5, 120.0, 175.0); assertEquals(0, c.count)
    }

    @Test fun jitterAroundThresholdDoesNotDoubleCount() {
        val c = RepCounter(100.0, 160.0)
        for (a in listOf(170.0, 140.0, 110.0, 90.0, 80.0, 95.0, 85.0, 100.0, 90.0, 120.0, 150.0, 165.0, 158.0, 167.0, 162.0, 175.0, 158.0, 163.0)) c.update(a)
        assertEquals(1, c.count)
    }

    @Test fun reset() {
        val c = RepCounter(100.0, 160.0)
        feed(c, 2, 80.0, 175.0)
        c.reset()
        assertEquals(0, c.count)
        assertFalse(c.isDown)
    }

    @Test fun supportedExercisesExist() {
        for (id in RepProfile.BY_EXERCISE.keys) assertNotNull("$id is not in the library", ExerciseTraits.byId[id])
    }
}
