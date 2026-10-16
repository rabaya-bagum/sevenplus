import SwiftUI

@main
struct SevenPlusApp: App {
    @StateObject private var store = SessionStore()
    @StateObject private var premium = PremiumStore()

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
        }
    }
}
