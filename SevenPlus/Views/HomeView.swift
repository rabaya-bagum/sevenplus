import SwiftUI

struct HomeView: View {
    @EnvironmentObject var store: SessionStore
    @State private var playing: Workout?

    private var workout: Workout {
        store.quietMode ? ExerciseLibrary.quiet(ExerciseLibrary.classic) : ExerciseLibrary.classic
    }

    var body: some View {
        NavigationStack {
            List {
                Section {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Today").font(.subheadline).foregroundStyle(.secondary)
                        Text(workout.name).font(.title2.bold())
                        Text("\(workout.steps.count) exercises · \(workout.totalSeconds / 60) min · no equipment")
                            .foregroundStyle(.secondary)
                        Button { playing = workout } label: {
                            Text("Start").font(.headline).frame(maxWidth: .infinity).padding(.vertical, 6)
                        }
                        .buttonStyle(.borderedProminent)
                        .accessibilityHint("Starts the workout with a 5 second countdown")
                    }
                    .padding(.vertical, 6)
                }

                Section {
                    Toggle("Quiet mode (no jumping)", isOn: $store.quietMode)
                    LabeledContent("Streak", value: "\(store.streak) day\(store.streak == 1 ? "" : "s")")
                }

                Section("Exercises") {
                    ForEach(Array(workout.steps.enumerated()), id: \.offset) { i, step in
                        let ex = ExerciseLibrary.exercise(step.exerciseId)
                        Label("\(i + 1). \(ex.name)", systemImage: ex.symbol)
                    }
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
            .fullScreenCover(item: $playing) { w in
                PlayerView(workout: w) { playing = nil }
            }
        }
    }
}
