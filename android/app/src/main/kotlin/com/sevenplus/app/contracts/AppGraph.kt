package com.sevenplus.app.contracts

import androidx.compose.runtime.staticCompositionLocalOf

/** Everything the screens need. Built once in SevenPlusApplication. */
class AppGraph(
    val sessions: SessionRepository,
    val settings: SettingsRepository,
    val health: HealthService,
    val reminders: ReminderScheduler,
    val premium: PremiumService,
    val challenges: ChallengeRepository,
    val crash: CrashReporting,
    val reps: RepCounterUi,
    val yourData: YourDataService,
)

val LocalAppGraph = staticCompositionLocalOf<AppGraph> { error("AppGraph not provided") }
