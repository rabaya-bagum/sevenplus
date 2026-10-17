package com.sevenplus.app.challenges

import com.sevenplus.app.contracts.FakeSettingsRepository
import com.sevenplus.core.LocalChallengeService
import com.sevenplus.core.Session
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppChallengeRepositoryTest {
    private val settings = FakeSettingsRepository()
    private val service = LocalChallengeService(settings.keyValueStore)
    private fun repo() = AppChallengeRepository(service, settings, TestScope(), service.isOnline)

    private fun sessions(n: Int): List<Session> = SessionFixture.completed(n)

    @Test fun createCountsProgressAndPersists() = runTest {
        val r = repo()
        r.create("", 5, 7, sessions(2))
        val c = r.challenges.value.single()
        assertEquals("5 workouts in 7 days", c.title)
        assertEquals(2, c.participants.single().completed)
        val again = repo(); again.refresh(sessions(2))
        assertEquals(c.id, again.challenges.value.single().id)
        assertFalse(r.isOnline)
    }

    @Test fun reportProgressUpdatesNameAndCount() = runTest {
        val r = repo()
        r.create("T", 5, 7, emptyList())
        settings.update { it.copy(displayName = "Naz") }
        r.reportProgress(sessions(3))
        val p = r.challenges.value.single().participants.single()
        assertEquals(3, p.completed); assertEquals("Naz", p.name)
        assertEquals(3, service.list(settings.settings.value.userId).single().participants.single().completed)
    }

    @Test fun joinValidatesCode() = runTest {
        val r = repo()
        assertFalse(r.join("abc", emptyList()))
        assertEquals("Invite codes are 6 letters and numbers.", r.error.value)
        assertFalse(r.join("ABC-234", emptyList()))
        assertEquals("Joining friends' challenges needs the online service.", r.error.value)
    }

    @Test fun joinLocalCodeWorks() = runTest {
        val r = repo()
        r.create("T", 5, 7, emptyList())
        val code = r.challenges.value.single().code
        r.clearLocal()
        assertTrue(r.join(code.lowercase(), sessions(1)))
        assertEquals(1, r.challenges.value.single().participants.single().completed)
    }

    @Test fun leaveAllAndClear() = runTest {
        val r = repo()
        r.create("T", 5, 7, emptyList())
        r.leaveAll()
        assertTrue(r.challenges.value.isEmpty())
        assertTrue(service.list(settings.settings.value.userId).isEmpty())
        r.join("zz", emptyList()); assertNotNull(r.error.value)
        r.clearLocal(); assertEquals(null, r.error.value)
    }
}
