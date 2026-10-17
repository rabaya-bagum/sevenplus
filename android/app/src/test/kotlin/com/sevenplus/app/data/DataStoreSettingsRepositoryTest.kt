package com.sevenplus.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.robolectric.RobolectricTestRunner
import com.sevenplus.app.contracts.AppSettings
import com.sevenplus.core.Readiness
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DataStoreSettingsRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val name = "settings-test-${System.nanoTime()}"

    /** Opens the repository, runs [block], then closes the DataStore so the next open reads from disk. */
    private fun <T> withRepo(block: suspend (DataStoreSettingsRepository) -> T): T = runBlocking {
        val job = SupervisorJob()
        try {
            block(DataStoreSettingsRepository(context, CoroutineScope(job + Dispatchers.IO), name))
        } finally {
            job.cancelAndJoin()
        }
    }

    @Test fun defaultsAndUserId() {
        val s = withRepo { it.settings.value }
        assertEquals(AppSettings(userId = s.userId), s)
        assertTrue(s.userId.isNotEmpty())
        assertEquals(s.userId, withRepo { it.settings.value.userId })
    }

    @Test fun updatePersists() {
        withRepo { r -> r.update { it.copy(displayName = "Naz", healthEnabled = true, reminders = it.reminders.copy(enabled = true, hour = 7)) } }
        val s = withRepo { it.settings.value }
        assertEquals("Naz", s.displayName)
        assertTrue(s.healthEnabled)
        assertEquals(7, s.reminders.hour)
        assertTrue(s.reminders.enabled)
    }

    @Test fun eraseAllRegeneratesUserId() {
        val before = withRepo { it.settings.value.userId }
        val after = withRepo { r ->
            r.update { it.copy(displayName = "Naz") }
            r.recordReadiness(LocalDate.of(2026, 1, 1), Readiness(Readiness.Energy.LOW, Readiness.Soreness.NONE))
            r.eraseAll()
            r.settings.value
        }
        assertNotEquals(before, after.userId)
        assertTrue(after.userId.isNotEmpty())
        assertEquals("Me", after.displayName)
        val reopened = withRepo { it.settings.value to it.readiness.value }
        assertEquals(after.userId, reopened.first.userId)
        assertTrue(reopened.second.isEmpty())
    }

    @Test fun readinessKeepsNewest60() {
        val start = LocalDate.of(2026, 1, 1)
        val r = Readiness(Readiness.Energy.OK, Readiness.Soreness.SOME)
        withRepo { repo -> for (i in 0 until 70) repo.recordReadiness(start.plusDays(i.toLong()), r) }
        withRepo { repo ->
            val map = repo.readiness.value
            assertEquals(60, map.size)
            assertNull(map[start.plusDays(9).toString()])
            assertEquals(r, repo.readinessFor(start.plusDays(10)))
            assertEquals(r, repo.readinessFor(start.plusDays(69)))
        }
    }

    @Test fun keyValueStoreRoundTripAndErase() {
        withRepo { r ->
            r.keyValueStore.putString("localChallenges", "[1]")
            assertEquals("[1]", r.keyValueStore.getString("localChallenges"))
            r.keyValueStore.putString("x", "y"); r.keyValueStore.putString("x", null)
            assertNull(r.keyValueStore.getString("x"))
        }
        withRepo { r ->
            assertEquals("[1]", r.keyValueStore.getString("localChallenges"))
            r.eraseAll()
            assertNull(r.keyValueStore.getString("localChallenges"))
        }
    }
}
