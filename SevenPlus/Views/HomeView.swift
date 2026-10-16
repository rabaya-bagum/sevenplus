import SwiftUI

struct HomeView: View {
    @EnvironmentObject var store: SessionStore
    @State private var playing: Workout?
    @State private var minutes: Int?
    @State private var editingProfile = false

    private var profile: Profile { store.profile ?? Profile() }

    private var today: Workout {
        WorkoutBuilder.build(profile: profile, minutes: minutes ?? profile.minutes,
                             dayIndex: WorkoutBuilder.dayIndex())
    }

    private var classic: Workout {
        profile.limitations.contains(.noJumping) ? ExerciseLibrary.quiet(ExerciseLibrary.classic) : ExerciseLibrary.classic
    }

    var body: some View {
        NavigationStack {
            List {
                Section {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Today").font(.subheadline).foregroundStyle(.secondary)
                        Text(today.name).font(.title2.bold())
                        Picker("Length", selection: Binding(get: { minutes ?? profile.minutes },
                                                            set: { minutes = $0 })) {
                            ForEach(WorkoutBuilder.lengths, id: \.self) { Text("\($0)m").tag($0) }
                        }
                        .pickerStyle(.segmented)
                        Text("\(today.steps.count) exercises · \(profile.level.intervals.work)s on / \(profile.level.intervals.rest)s rest")
                            .foregroundStyle(.secondary)
                        Button { playing = today } label: {
                            Text("Start").font(.headline).frame(maxWidth: .infinity).padding(.vertical, 6)
                        }
                        .buttonStyle(.borderedProminent)
                    }
                    .padding(.vertical, 6)
                }

                Section("Today's exercises") {
                    ForEach(Array(today.steps.enumerated()), id: \.offset) { i, step in
                        let ex = ExerciseLibrary.exercise(step.exerciseId)
                        Label("\(i + 1). \(ex.name)", systemImage: ex.symbol)
                    }
                }

                Section {
                    Button { playing = classic } label: {
                        Label("Classic 7-Minute circuit", systemImage: "bolt.heart")
                    }
                    LabeledContent("Streak", value: "\(store.streak) day\(store.streak == 1 ? "" : "s")")
                }

                if !store.sessions.isEmpty {
                    Section("History") {
                        ForEach(store.sessions.prefix(10)) { s in
                            HStack {
                                Text(s.date, style: .date)
                                Spacer()
                                Text(s.completed ? "\(s.durationSeconds / 60) min" : "Stopped early")
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                }
            }
            .navigationTitle("SevenPlus")
            .toolbar {
                Button { editingProfile = true } label: { Image(systemName: "person.crop.circle") }
                    .accessibilityLabel("Edit profile")
            }
            .sheet(isPresented: $editingProfile) { ProfileView() }
            .fullScreenCover(item: $playing) { w in
                PlayerView(workout: w) { playing = nil }
            }
        }
    }
}

/// Edit goal, level, default length and limitations after onboarding.
struct ProfileView: View {
    @EnvironmentObject var store: SessionStore
    @Environment(\.dismiss) private var dismiss

    private var p: Binding<Profile> {
        Binding(get: { store.profile ?? Profile() }, set: { store.profile = $0 })
    }

    var body: some View {
        NavigationStack {
            Form {
                Picker("Goal", selection: p.goal) { ForEach(Goal.allCases) { Text($0.rawValue).tag($0) } }
                Picker("Level", selection: p.level) { ForEach(Level.allCases) { Text($0.rawValue).tag($0) } }
                Picker("Default length", selection: p.minutes) {
                    ForEach(WorkoutBuilder.lengths, id: \.self) { Text("\($0) min").tag($0) }
                }
                Section("Go easy on") {
                    ForEach(Limitation.allCases) { lim in
                        Toggle(lim.rawValue, isOn: Binding(
                            get: { p.wrappedValue.limitations.contains(lim) },
                            set: { on in
                                var v = p.wrappedValue
                                if on { v.limitations.insert(lim) } else { v.limitations.remove(lim) }
                                p.wrappedValue = v
                            }))
                    }
                }
            }
            .navigationTitle("Profile")
            .toolbar { Button("Done") { dismiss() } }
        }
    }
}
