package com.sevenplus.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.robolectric.RobolectricTestRunner
import com.sevenplus.app.contracts.ChallengeRepository
import com.sevenplus.app.contracts.CrashReporting
import com.sevenplus.app.contracts.EraseOutcome
import com.sevenplus.app.contracts.FakeHealthService
import com.sevenplus.app.contracts.FakeReminderScheduler
import com.sevenplus.app.contracts.FakeSessionRepository
import com.sevenplus.app.contracts.FakeSettingsRepository
import com.sevenplus.core.Challenge
import com.sevenplus.core.Profile
import com.sevenplus.core.ReminderSettings
import com.sevenplus.core.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidYourDataServiceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val calls = mutableListOf<String>()

    private val sessions by lazy { RecordingSessions(FakeSessionRepository(Profile())) }
    private inner class RecordingSessions(val inner: FakeSessionRepository) : com.sevenplus.app.contracts.SessionRepository by inner {
        override suspend fun eraseAll() { calls += "sessions.eraseAll"; inner.eraseAll() }
    }
    private val fakeSettings = FakeSettingsRepository()
    private inner class RecordingSettings : com.sevenplus.app.contracts.SettingsRepository by fakeSettings {
        override suspend fun eraseAll() { calls += "settings.eraseAll"; fakeSettings.eraseAll() }
    }
    private inner class RecordingHealth(val ok: Boolean) : com.sevenplus.app.contracts.HealthService by FakeHealthService() {
        override suspend fun deleteSavedData(): Boolean { calls += "health.delete"; return ok }
    }
    private val reminders = FakeReminderScheduler()
    private inner class RecordingReminders : com.sevenplus.app.contracts.ReminderScheduler by reminders {
        override fun apply(settings: ReminderSettings) { calls += "reminders.apply"; reminders.apply(settings) }
    }
    private inner class RecordingCrash : CrashReporting {
        override val isConfigured = true
        override fun setEnabled(enabled: Boolean) { calls += "crash.$enabled" }
    }
    private inner class Challenges(override val isOnline: Boolean, val fail: Boolean) : ChallengeRepository {
        override val challenges = MutableStateFlow<List<Challenge>>(emptyList())
        override val error = MutableStateFlow<String?>(null)
        override suspend fun refresh(sessions: List<Session>) {}
        override suspend fun create(title: String, target: Int, days: Int, sessions: List<Session>) {}
        override suspend fun join(code: String, sessions: List<Session>) = false
        override suspend fun reportProgress(sessions: List<Session>) {}
        override suspend fun leaveAll() { calls += "challenges.leaveAll"; if (fail) throw IllegalStateException("offline") }
        override fun clearLocal() { calls += "challenges.clearLocal" }
    }

    private fun service(online: Boolean = true, fail: Boolean = false, healthOk: Boolean = true) =
        AndroidYourDataService(context, sessions, RecordingSettings(), RecordingHealth(healthOk), RecordingReminders(),
            Challenges(online, fail), RecordingCrash())

    @Test fun remoteFailureDeletesNothing() = runTest {
        sessions.inner.add(Session("s1", "w", 1, 420, true))
        val out = service(fail = true).erase(alsoHealth = true, skipRemote = false)
        assertEquals(EraseOutcome.RemoteFailed("offline"), out)
        assertEquals(listOf("challenges.leaveAll"), calls)
        assertEquals(1, sessions.sessions.value.size)
        assertNotNull(sessions.profile.value)
    }

    @Test fun successRunsInOrder() = runTest {
        File(context.cacheDir, "exports").apply { mkdirs() }.resolve("SevenPlus-data-old.json").writeText("{}")
        sessions.inner.add(Session("s1", "w", 1, 420, true))
        val before = fakeSettings.settings.value.userId
        val out = service(healthOk = false).erase(alsoHealth = true, skipRemote = false)
        assertEquals(EraseOutcome.Erased(healthDeleted = false), out)
        assertEquals(listOf("challenges.leaveAll", "health.delete", "settings.eraseAll", "reminders.apply",
            "crash.false", "challenges.clearLocal", "sessions.eraseAll"), calls)
        assertEquals(ReminderSettings(), reminders.last)
        assertTrue(sessions.sessions.value.isEmpty())
        assertTrue(before != fakeSettings.settings.value.userId)
        assertTrue(File(context.cacheDir, "exports").listFiles().isNullOrEmpty())
    }

    @Test fun skipRemoteSkipsLeaveAll() = runTest {
        val out = service(fail = true).erase(alsoHealth = false, skipRemote = true)
        assertEquals(EraseOutcome.Erased(healthDeleted = true), out)
        assertTrue("challenges.leaveAll" !in calls)
        assertTrue("health.delete" !in calls)
    }

    @Test fun offlineChallengesSkipLeaveAll() = runTest {
        service(online = false, fail = true).erase(alsoHealth = true, skipRemote = false)
        assertTrue("challenges.leaveAll" !in calls)
        assertTrue("health.delete" in calls)
    }

    @Ignore("needs core ExportFile.encode; enable after merge")
    @Test fun writeExportContainsSessionIds() = runTest {
        sessions.inner.add(Session("session-abc", "w", 1, 420, true))
        sessions.inner.add(Session("session-xyz", "w", 2, 420, true))
        val uri = service().writeExport()
        assertEquals("content", uri.scheme)
        val file = File(context.cacheDir, "exports").listFiles()!!.single { it.name.startsWith("SevenPlus-data-") }
        val text = file.readText()
        assertTrue(text.contains("session-abc") && text.contains("session-xyz"))
    }
}
