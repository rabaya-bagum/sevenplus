import SwiftUI

struct TodayView: View {
    @EnvironmentObject var store: SessionStore
    @State private var playing: Workout?
    @State private var minutes: Int?

    private var profile: Profile { store.profile ?? Profile() }

    private var today: Workout {
        WorkoutBuilder.build(profile: profile, minutes: minutes ?? profile.minutes,
                             dayIndex: WorkoutBuilder.dayIndex())
    }

    var body: some View {
        NavigationStack {
            List {
                Section {
                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            Text("Today").font(.subheadline).foregroundStyle(.secondary)
                            Spacer()
                            Label("\(store.streak)", systemImage: "flame.fill")
                                .font(.subheadline.bold()).foregroundStyle(.orange)
                                .accessibilityLabel("\(store.streak) day streak")
                        }
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
                        NavigationLink(value: ex) {
                            Label("\(i + 1). \(ex.name)", systemImage: ex.symbol)
                        }
                    }
                }
            }
            .navigationTitle("SevenPlus")
            .navigationDestination(for: Exercise.self) { ExerciseDetailView(exercise: $0) }
            .fullScreenCover(item: $playing) { w in
                PlayerView(workout: w) { playing = nil }
            }
        }
    }
}
