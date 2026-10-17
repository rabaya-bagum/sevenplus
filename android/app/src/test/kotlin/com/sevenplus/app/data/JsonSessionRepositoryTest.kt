package com.sevenplus.app.data

import androidx.test.core.app.ApplicationProvider
import org.robolectric.RobolectricTestRunner
import android.content.Context
import com.sevenplus.core.Difficulty
import com.sevenplus.core.Goal
import com.sevenplus.core.Profile
import com.sevenplus.core.Session
import com.sevenplus.core.Workout
import com.sevenplus.core.WorkoutStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class JsonSessionRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var dir: File
    private val scope = CoroutineScope(Dispatchers.Unconfined)

    @Before fun setUp() { dir = File(context.filesDir, "store-test-${System.nanoTime()}") }

    private fun repo() = JsonSessionRepository(context, scope, dir)
    private fun session(id: String, date: Long) = Session(id, "w", date, 420, true)
    private fun workout(id: String, name: String) = Workout(id, name, listOf(WorkoutStep("jumping-jacks", 30, 10)))

    @Test fun addRateProfileCustom() = runTest {
        val r = repo()
        r.add(session("a", 1)); r.add(session("b", 2))
        assertEquals(listOf("b", "a"), r.sessions.value.map { it.id })
        r.rate("a", Difficulty.TOO_HARD)
        assertEquals(Difficulty.TOO_HARD, r.sessions.value.first { it.id == "a" }.difficulty)
        r.setProfile(Profile(goal = Goal.GET_FIT, minutes = 10))
        assertEquals(10, r.profile.value?.minutes)
        r.saveCustom(workout("1", "One")); r.saveCustom(workout("2", "Two")); r.saveCustom(workout("1", "One v2"))
        assertEquals(listOf("One v2", "Two"), r.customWorkouts.value.map { it.name })
        r.deleteCustom("2")
        assertEquals(listOf("1"), r.customWorkouts.value.map { it.id })
    }

    @Test fun survivesRelaunch() = runTest {
        val r = repo()
        r.add(session("a", 1)); r.setProfile(Profile(minutes = 5)); r.saveCustom(workout("1", "One"))
        val again = repo()
        assertEquals(listOf("a"), again.sessions.value.map { it.id })
        assertEquals(5, again.profile.value?.minutes)
        assertEquals(listOf("1"), again.customWorkouts.value.map { it.id })
    }

    @Test fun eraseAllSurvivesRelaunch() = runTest {
        val r = repo()
        r.add(session("a", 1)); r.setProfile(Profile()); r.saveCustom(workout("1", "One"))
        r.eraseAll()
        val again = repo()
        assertEquals(emptyList<Session>(), again.sessions.value)
        assertNull(again.profile.value)
        assertEquals(emptyList<Workout>(), again.customWorkouts.value)
    }

    @Test fun corruptedFileStartsEmpty() = runTest {
        dir.mkdirs()
        File(dir, "sessions.json").writeText("{not json")
        val r = repo()
        assertEquals(emptyList<Session>(), r.sessions.value)
        r.add(session("a", 1))
        assertEquals(listOf("a"), repo().sessions.value.map { it.id })
    }
}
