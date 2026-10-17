package com.sevenplus.app.contracts

import android.app.Activity
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sevenplus.core.Challenge
import com.sevenplus.core.Difficulty
import com.sevenplus.core.InMemoryKeyValueStore
import com.sevenplus.core.KeyValueStore
import com.sevenplus.core.Profile
import com.sevenplus.core.Readiness
import com.sevenplus.core.ReminderSettings
import com.sevenplus.core.Session
import com.sevenplus.core.Workout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.util.UUID

// In-memory stand-ins: used for UI previews/tests and until real implementations are wired.

class FakeSessionRepository(profile: Profile? = null) : SessionRepository {
    override val sessions = MutableStateFlow<List<Session>>(emptyList())
    override val profile = MutableStateFlow(profile)
    override val customWorkouts = MutableStateFlow<List<Workout>>(emptyList())
    override suspend fun add(session: Session) { sessions.value = listOf(session) + sessions.value }
    override suspend fun rate(sessionId: String, difficulty: Difficulty) {
        sessions.value = sessions.value.map { if (it.id == sessionId) it.copy(difficulty = difficulty) else it }
    }
    override suspend fun setProfile(profile: Profile?) { this.profile.value = profile }
    override suspend fun saveCustom(workout: Workout) {
        customWorkouts.value = listOf(workout) + customWorkouts.value.filter { it.id != workout.id }
    }
    override suspend fun deleteCustom(id: String) { customWorkouts.value = customWorkouts.value.filter { it.id != id } }
    override suspend fun eraseAll() { sessions.value = emptyList(); customWorkouts.value = emptyList(); profile.value = null }
}

class FakeSettingsRepository : SettingsRepository {
    override val settings = MutableStateFlow(AppSettings(userId = UUID.randomUUID().toString()))
    override suspend fun update(transform: (AppSettings) -> AppSettings) { settings.value = transform(settings.value) }
    override val readiness = MutableStateFlow<Map<String, Readiness>>(emptyMap())
    override suspend fun recordReadiness(date: LocalDate, readiness: Readiness) {
        this.readiness.value = this.readiness.value + (date.toString() to readiness)
    }
    override val keyValueStore: KeyValueStore = InMemoryKeyValueStore()
    override suspend fun eraseAll() {
        settings.value = AppSettings(userId = UUID.randomUUID().toString()); readiness.value = emptyMap()
    }
}

class FakeHealthService : HealthService {
    override val availability = HealthAvailability.NOT_INSTALLED
    override val permissions: Set<String> = emptySet()
    override fun permissionContract() = object : ActivityResultContract<Set<String>, Set<String>>() {
        override fun createIntent(context: Context, input: Set<String>) = Intent()
        override fun parseResult(resultCode: Int, intent: Intent?): Set<String> = emptySet()
    }
    override suspend fun hasPermissions() = false
    override suspend fun saveWorkout(startMillis: Long, endMillis: Long, kcal: Int) {}
    override suspend fun latestWeightKg(): Double? = null
    override suspend fun deleteSavedData() = true
}

class FakeReminderScheduler : ReminderScheduler {
    var last: ReminderSettings? = null
    override fun apply(settings: ReminderSettings) { last = settings }
    override fun canPostNotifications() = true
}

class FakePremiumService : PremiumService {
    override val isPremium = MutableStateFlow(false)
    override val products = MutableStateFlow(listOf(
        ProductInfo("com.sevenplus.premium.monthly", "Premium Monthly", "$9.99", "month"),
        ProductInfo("com.sevenplus.premium.yearly", "Premium Yearly", "$59.99", "year", freeTrialDays = 7),
    ))
    override val lastError = MutableStateFlow<String?>(null)
    override fun start() {}
    override suspend fun purchase(activity: Activity, productId: String): Boolean { isPremium.value = true; return true }
    override suspend fun restore() {}
}

class FakeChallengeRepository : ChallengeRepository {
    override val isOnline = false
    override val challenges = MutableStateFlow<List<Challenge>>(emptyList())
    override val error = MutableStateFlow<String?>(null)
    override suspend fun refresh(sessions: List<Session>) {}
    override suspend fun create(title: String, target: Int, days: Int, sessions: List<Session>) {}
    override suspend fun join(code: String, sessions: List<Session>) = false
    override suspend fun reportProgress(sessions: List<Session>) {}
    override suspend fun leaveAll() { challenges.value = emptyList() }
    override fun clearLocal() { challenges.value = emptyList() }
}

class FakeCrashReporting(override val isConfigured: Boolean = false) : CrashReporting {
    var isEnabled = false
        private set
    override fun setEnabled(enabled: Boolean) { isEnabled = enabled }
}

class NoRepCounterUi : RepCounterUi {
    @Composable
    override fun Panel(exerciseId: String, active: Boolean, modifier: Modifier) {}
}

class FakeYourDataService : YourDataService {
    override suspend fun writeExport(): Uri = Uri.EMPTY
    override suspend fun erase(alsoHealth: Boolean, skipRemote: Boolean): EraseOutcome = EraseOutcome.Erased(true)
}

fun fakeAppGraph(profile: Profile? = null) = AppGraph(
    sessions = FakeSessionRepository(profile),
    settings = FakeSettingsRepository(),
    health = FakeHealthService(),
    reminders = FakeReminderScheduler(),
    premium = FakePremiumService(),
    challenges = FakeChallengeRepository(),
    crash = FakeCrashReporting(),
    reps = NoRepCounterUi(),
    yourData = FakeYourDataService(),
)
