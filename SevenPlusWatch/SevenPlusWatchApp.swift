import SwiftUI

@main
struct SevenPlusWatchApp: App {
    @StateObject private var model = WatchModel()

    var body: some Scene {
        WindowGroup {
            WatchPlayerView().environmentObject(model)
        }
    }
}
