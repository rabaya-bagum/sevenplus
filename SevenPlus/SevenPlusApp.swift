import SwiftUI

@main
struct SevenPlusApp: App {
    @StateObject private var store = SessionStore()

    var body: some Scene {
        WindowGroup {
            HomeView()
                .environmentObject(store)
        }
    }
}
