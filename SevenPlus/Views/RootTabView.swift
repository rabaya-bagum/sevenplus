import SwiftUI

struct RootTabView: View {
    @EnvironmentObject var premium: PremiumStore

    var body: some View {
        TabView {
            TodayView().tabItem { Label("Today", systemImage: "sun.max") }
            WorkoutsView().tabItem { Label("Workouts", systemImage: "figure.run") }
            ProgressTabView().tabItem { Label("Progress", systemImage: "chart.bar") }
            ProfileView().tabItem { Label("Profile", systemImage: "person.crop.circle") }
        }
        .sheet(isPresented: $premium.showPaywall) { PaywallView() }
    }
}
