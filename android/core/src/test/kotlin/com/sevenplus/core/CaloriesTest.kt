package com.sevenplus.core

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CaloriesTest {
    @Test fun estimate() {
        assertEquals(56, Calories.estimate(360, null))
        assertEquals(72, Calories.estimate(360, 90.0))
        assertEquals(0, Calories.estimate(0, 80.0))
    }

    @Test fun activeSecondsExcludesRestAndGetReady() {
        val w = ExerciseLibrary.classic
        assertEquals("still getting ready", 0, Calories.activeSeconds(w, 3))
        assertEquals(15, Calories.activeSeconds(w, 20))
        assertEquals("first rest not counted", 30, Calories.activeSeconds(w, 40))
        assertEquals(35, Calories.activeSeconds(w, 50))
        assertEquals(360, Calories.activeSeconds(w, 475))
        assertEquals(360, Calories.activeSeconds(w, 10_000))
    }

    @Test fun oldSessionsWithoutCaloriesStillDecode() {
        val json = """[{"id":"x","workoutId":"w","dateMillis":0,"durationSeconds":60,"completed":true,"skipped":[]}]"""
        val sessions = Json.decodeFromString(ListSerializer(Session.serializer()), json)
        assertNull(sessions.first().calories)
    }
}
