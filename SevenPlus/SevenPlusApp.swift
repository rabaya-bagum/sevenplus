import SwiftUI

@main
struct SevenPlusApp: App {
    @StateObject private var store = SessionStore()
    @StateObject private var premium = PremiumStore()
    @StateObject private var reminders = ReminderScheduler()
    @StateObject private var health = HealthManager()

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
        }
    }
}
