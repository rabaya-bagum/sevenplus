import SwiftUI

/// Edit goal, level, default length and limitations after onboarding.
struct ProfileView: View {
    @EnvironmentObject var store: SessionStore

    private var p: Binding<Profile> {
        Binding(get: { store.profile ?? Profile() }, set: { store.profile = $0 })
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Training") {
                    Picker("Goal", selection: p.goal) { ForEach(Goal.allCases) { Text($0.rawValue).tag($0) } }
                    Picker("Level", selection: p.level) { ForEach(Level.allCases) { Text($0.rawValue).tag($0) } }
                    Picker("Default length", selection: p.minutes) {
                        ForEach(WorkoutBuilder.lengths, id: \.self) { Text("\($0) min").tag($0) }
                    }
                }
                Section {
                    ForEach(Limitation.allCases) { lim in
                        Toggle(lim.rawValue, isOn: Binding(
                            get: { p.wrappedValue.limitations.contains(lim) },
                            set: { on in
                                var v = p.wrappedValue
                                if on { v.limitations.insert(lim) } else { v.limitations.remove(lim) }
                                p.wrappedValue = v
                            }))
                    }
                } header: {
                    Text("Go easy on")
                } footer: {
                    Text("Daily workouts leave out moves that load these areas.")
                }
            }
            .navigationTitle("Profile")
        }
    }
}
