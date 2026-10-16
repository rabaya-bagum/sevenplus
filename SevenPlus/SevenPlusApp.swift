import SwiftUI

@main
struct SevenPlusApp: App {
    // First, so a crash while loading data can still be reported (if the user opted in).
    @StateObject private var crash = CrashReporter()
    @StateObject private var store = SessionStore()
    @StateObject private var premium = PremiumStore()
    @StateObject private var reminders = ReminderScheduler()
    @StateObject private var health = HealthManager()
    @StateObject private var challenges = ChallengeStore()

    var body: some Scene {
        WindowGroup {
            Group {
                if store.profile == nil {
                    OnboardingView { store.profile = $0 }
                } else {
                    RootTabView()
                }
            }
            .environmentObject(store)
            .environmentObject(premium)
            .environmentObject(reminders)
            .environmentObject(health)
            .environmentObject(challenges)
            .environmentObject(crash)
            .onAppear { PhoneConnectivity.shared.activate() }
        }
    }
}
