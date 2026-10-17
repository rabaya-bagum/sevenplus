package com.sevenplus.app.contracts

import android.app.Activity
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sevenplus.core.Challenge
import com.sevenplus.core.Difficulty
import com.sevenplus.core.KeyValueStore
import com.sevenplus.core.Profile
import com.sevenplus.core.Readiness
import com.sevenplus.core.ReminderSettings
import com.sevenplus.core.Session
import com.sevenplus.core.Workout
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

// Shared interfaces between the app's areas. Signatures here are fixed; add new members only
// if needed and say so. Implementations: data/, health/, reminders/ (data & system),
// billing/, challenges/, crash/, reps/ (services), ui/ (screens).

/** Workout history, profile and custom workouts (iOS SessionStore). */
interface SessionRepository {
    /** Newest first. */
    val sessions: StateFlow<List<Session>>
    /** Null until onboarding is finished. */
    val profile: StateFlow<Profile?>
    /** Newest first. */
    val customWorkouts: StateFlow<List<Workout>>
    suspend fun add(session: Session)
    suspend fun rate(sessionId: String, difficulty: Difficulty)
    suspend fun setProfile(profile: Profile?)
    /** Insert or update by id. */
    suspend fun saveCustom(workout: Workout)
    suspend fun deleteCustom(id: String)
    suspend fun eraseAll()
}

data class AppSettings(
    val reminders: ReminderSettings = ReminderSettings(),
    val healthEnabled: Boolean = false,
    val crashReportsEnabled: Boolean = false,
    val countReps: Boolean = false,
    val upsellShown: Boolean = false,
    val displayName: String = "Me",
    /** Random id for challenges; never empty once the repository is loaded. */
    val userId: String = "",
    /** Debug builds only: pretend Premium is active. */
    val debugPremium: Boolean = false,
)

/** Small device settings and daily check-ins (iOS UserDefaults keys). */
interface SettingsRepository {
    val settings: StateFlow<AppSettings>
    suspend fun update(transform: (AppSettings) -> AppSettings)
    /** "yyyy-MM-dd" -> check-in, roughly the last 60 days. */
    val readiness: StateFlow<Map<String, Readiness>>
    suspend fun recordReadiness(date: LocalDate, readiness: Readiness)
    /** Backing store for the on-device challenge service. */
    val keyValueStore: KeyValueStore
    /** Clears everything, then writes a fresh random userId. */
    suspend fun eraseAll()
}

enum class HealthAvailability { AVAILABLE, NOT_INSTALLED, NOT_SUPPORTED }

/** Health Connect (iOS HealthManager). Only used when AppSettings.healthEnabled. */
interface HealthService {
    val availability: HealthAvailability
    /** Health Connect permission strings SevenPlus asks for. */
    val permissions: Set<String>
    /** Use with rememberLauncherForActivityResult(...).launch(permissions). */
    fun permissionContract(): ActivityResultContract<Set<String>, Set<String>>
    suspend fun hasPermissions(): Boolean
    /** One HIIT exercise session with its active calories. */
    suspend fun saveWorkout(startMillis: Long, endMillis: Long, kcal: Int)
    suspend fun latestWeightKg(): Double?
    /** Deletes what SevenPlus wrote; false if Health Connect refused. */
    suspend fun deleteSavedData(): Boolean
}

/** Schedules (or cancels, when disabled) the weekly reminder notifications. */
interface ReminderScheduler {
    fun apply(settings: ReminderSettings)
    fun canPostNotifications(): Boolean
}

data class ProductInfo(
    val id: String,
    val title: String,
    /** Formatted, e.g. "CA$9.99". */
    val price: String,
    /** "month" or "year". */
    val period: String,
    val freeTrialDays: Int? = null,
)

/** Google Play subscriptions (iOS PremiumStore). Product ids match iOS. */
interface PremiumService {
    val isPremium: StateFlow<Boolean>
    val products: StateFlow<List<ProductInfo>>
    val lastError: StateFlow<String?>
    /** Connect to Play and load products/entitlements. Safe to call more than once. */
    fun start()
    /** True when the purchase went through. */
    suspend fun purchase(activity: Activity, productId: String): Boolean
    suspend fun restore()

    companion object {
        val PRODUCT_IDS = listOf("com.sevenplus.premium.monthly", "com.sevenplus.premium.yearly")
    }
}

/** Challenges for this user (iOS ChallengeStore). Uses SettingsRepository for userId/displayName. */
interface ChallengeRepository {
    val isOnline: Boolean
    val challenges: StateFlow<List<Challenge>>
    val error: StateFlow<String?>
    suspend fun refresh(sessions: List<Session>)
    suspend fun create(title: String, target: Int, days: Int, sessions: List<Session>)
    suspend fun join(code: String, sessions: List<Session>): Boolean
    /** Updates this user's count in every challenge; call after each completed workout. */
    suspend fun reportProgress(sessions: List<Session>)
    /** Removes this user from online leaderboards. Throws if the server can't be reached. */
    suspend fun leaveAll()
    /** Forget challenges locally (after erase). */
    fun clearLocal()
}

/** Opt-in Sentry (iOS CrashReporter). */
interface CrashReporting {
    /** True when this build has a DSN. Hide the switch otherwise. */
    val isConfigured: Boolean
    /** Starts or stops reporting immediately. */
    fun setEnabled(enabled: Boolean)
}

/** Camera rep counting panel for the workout player (iOS RepCounterView + PoseCamera). */
interface RepCounterUi {
    /** Handles camera permission itself. Counts only while [active]. */
    @Composable
    fun Panel(exerciseId: String, active: Boolean, modifier: Modifier)
}

sealed interface EraseOutcome {
    data class Erased(val healthDeleted: Boolean) : EraseOutcome
    /** Nothing was deleted; the challenge server couldn't be reached. */
    data class RemoteFailed(val message: String) : EraseOutcome
}

/** Profile -> Your data: export and erase (iOS ExportFile + DataEraser). */
interface YourDataService {
    /** Writes the JSON export and returns a content:// Uri to share. */
    suspend fun writeExport(): Uri
    /** Same order as iOS: leave online challenges, Health, settings, reminders, then history. */
    suspend fun erase(alsoHealth: Boolean, skipRemote: Boolean): EraseOutcome
}
