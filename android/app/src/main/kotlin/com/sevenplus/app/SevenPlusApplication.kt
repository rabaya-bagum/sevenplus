package com.sevenplus.app

import android.app.Application
import com.sevenplus.app.billing.PlayBillingPremiumService
import com.sevenplus.app.challenges.AppChallengeRepository
import com.sevenplus.app.challenges.challengeService
import com.sevenplus.app.contracts.AppGraph
import com.sevenplus.app.crash.SentryCrashReporting
import com.sevenplus.app.data.AndroidYourDataService
import com.sevenplus.app.data.DataStoreSettingsRepository
import com.sevenplus.app.data.JsonSessionRepository
import com.sevenplus.app.health.HealthConnectService
import com.sevenplus.app.reminders.AlarmReminderScheduler
import com.sevenplus.app.reps.MlKitRepCounterUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SevenPlusApplication : Application() {
    lateinit var graph: AppGraph
        private set

    /** Lives as long as the process. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        graph = buildGraph()
    }

    private fun buildGraph(): AppGraph {
        val sessions = JsonSessionRepository(this, appScope)
        val settings = DataStoreSettingsRepository(this, appScope)
        val health = HealthConnectService(this)
        val reminders = AlarmReminderScheduler(this)
        val debugPremium = settings.settings.map { BuildConfig.DEBUG && it.debugPremium }
            .stateIn(appScope, SharingStarted.Eagerly, false)
        val premium = PlayBillingPremiumService(this, appScope, debugPremium)
        val service = challengeService(settings)
        val challenges = AppChallengeRepository(service, settings, appScope)
        val crash = SentryCrashReporting(this, settings)
        return AppGraph(
            sessions = sessions,
            settings = settings,
            health = health,
            reminders = reminders,
            premium = premium,
            challenges = challenges,
            crash = crash,
            reps = MlKitRepCounterUi(),
            yourData = AndroidYourDataService(this, sessions, settings, health, reminders, challenges, crash),
        )
    }
}
