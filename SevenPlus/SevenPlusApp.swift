import SwiftUI

@main
struct SevenPlusApp: App {
    @StateObject private var store = SessionStore()

    var body: some Scene {
        WindowGroup {
            Group {
                if store.profile == nil {
                    OnboardingView { store.profile = $0 }
                } else {
                    HomeView()
                }
            }
            .environmentObject(store)
        }
    }
}
