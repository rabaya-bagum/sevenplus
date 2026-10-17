package com.sevenplus.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportFileTest {
    @Test fun encodeDecodeRoundTrip() {
        val file = ExportFile(
            exportedAtMillis = T.NOW_MILLIS,
            profile = Profile(Goal.GET_STRONG, Level.ADVANCED, 15, setOf(Limitation.KNEES, Limitation.NO_JUMPING)),
            sessions = listOf(T.session(T.NOW_MILLIS, difficulty = Difficulty.TOO_HARD).copy(skipped = listOf("plank"), calories = 40)),
            customWorkouts = listOf(CustomWorkouts.make("c1", "Legs", listOf("squat", "lunge"), 30, 10, 2)),
            reminders = ReminderSettings(enabled = true, hour = 7, minute = 30),
            readiness = mapOf("2025-10-09" to Readiness(Readiness.Energy.LOW, Readiness.Soreness.SOME)),
            challenges = listOf(Challenge.make("T", 3, 7, Challenge.Participant("me", "Me", 1), T.TODAY, T.ZONE)),
            displayName = "Nazmul",
        )
        val json = file.encode()
        assertTrue("pretty-printed", json.contains("\n"))
        assertTrue("defaults encoded", json.contains("\"version\""))
        assertEquals(file, ExportFile.decode(json))
    }

    @Test fun decodeIgnoresUnknownKeys() {
        val json = ExportFile(exportedAtMillis = 0, profile = null, sessions = emptyList(), customWorkouts = emptyList(),
            reminders = ReminderSettings(), readiness = emptyMap(), challenges = emptyList(), displayName = "")
            .encode().replaceFirst("{", "{\"extra\": 1,")
        assertEquals(1, ExportFile.decode(json).version)
    }
}
