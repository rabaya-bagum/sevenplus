package com.sevenplus.core

import org.junit.Assert.assertEquals
import org.junit.Test

class CoachScriptTest {
    private val w = ExerciseLibrary.classic

    @Test fun getReady() = assertEquals("Get ready. First up, Jumping Jacks.", CoachScript.line(Phase.GetReady(5), w))
    @Test fun work() = assertEquals("Wall Sit. Knees over ankles, back flat on the wall.", CoachScript.line(Phase.Work(1, 30), w))
    @Test fun rest() = assertEquals("Rest. Next, Push-up.", CoachScript.line(Phase.Rest(1, 10), w))
    @Test fun finished() = assertEquals("Workout complete. Great job!", CoachScript.line(Phase.Finished, w))
}
