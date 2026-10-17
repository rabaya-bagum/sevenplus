package com.sevenplus.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WorkoutBuilderTest {
    @Test fun everyExerciseHasTraits() {
        for (ex in ExerciseLibrary.all) assertNotNull("${ex.id} missing traits", ExerciseTraits.byId[ex.id])
    }

    @Test fun exerciseCountMatchesLength() {
        val p = Profile(Goal.GET_FIT, Level.BEGINNER, 7, emptySet())
        assertEquals(9, WorkoutBuilder.build(p).steps.size)
        assertEquals(10, WorkoutBuilder.build(Profile(level = Level.INTERMEDIATE, minutes = 7)).steps.size)
        assertEquals(24, WorkoutBuilder.build(Profile(level = Level.ADVANCED, minutes = 20)).steps.size)
    }

    @Test fun nameUsesGoalTitle() {
        val p = Profile(Goal.LOSE_WEIGHT, Level.BEGINNER, 7, emptySet())
        assertEquals("7-Minute Lose Weight", WorkoutBuilder.build(p).name)
        assertEquals("daily-0-7", WorkoutBuilder.build(p).id)
        assertEquals("daily-12-10", WorkoutBuilder.build(p, minutes = 10, dayIndex = 12).id)
    }

    @Test fun actualDurationIsCloseToRequested() {
        for (level in Level.entries) for (mins in WorkoutBuilder.LENGTHS) {
            val w = WorkoutBuilder.build(Profile(level = level, minutes = mins))
            val seconds = Timeline(w, getReadySeconds = 0).totalSeconds
            assertTrue("$level ${mins}m took ${seconds}s", Math.abs(seconds - mins * 60) <= 50)
        }
    }

    @Test fun respectsEveryLimitation() {
        for (lim in Limitation.entries) {
            val w = WorkoutBuilder.build(Profile(minutes = 20, limitations = setOf(lim)))
            for (step in w.steps) assertFalse("${step.exerciseId} used despite $lim", lim in ExerciseTraits.of(step.exerciseId).stresses)
        }
    }

    @Test fun allLimitationsStillGivesAWorkout() {
        val w = WorkoutBuilder.build(Profile(minutes = 10, limitations = Limitation.entries.toSet()))
        assertFalse(w.steps.isEmpty())
        for (step in w.steps) assertTrue(ExerciseTraits.of(step.exerciseId).stresses.isEmpty())
    }

    @Test fun alternatesBodyAreas() {
        val areas = WorkoutBuilder.build(Profile(minutes = 10)).steps.map { ExerciseTraits.of(it.exerciseId).area }
        for ((a, b) in areas.zipWithNext()) assertNotEquals(a, b)
    }

    @Test fun sameDaySameWorkoutAndDaysVary() {
        val p = Profile(minutes = 7)
        assertEquals(WorkoutBuilder.build(p, dayIndex = 3), WorkoutBuilder.build(p, dayIndex = 3))
        assertNotEquals(WorkoutBuilder.build(p, dayIndex = 3).steps, WorkoutBuilder.build(p, dayIndex = 4).steps)
    }

    @Test fun dayIndexCountsFrom2001() {
        assertEquals(0, WorkoutBuilder.dayIndex(LocalDate.of(2001, 1, 1)))
        assertEquals(1, WorkoutBuilder.dayIndex(LocalDate.of(2001, 1, 2)))
        assertEquals(365, WorkoutBuilder.dayIndex(LocalDate.of(2002, 1, 1)))
    }
}
