import SwiftUI

struct TodayView: View {
    @EnvironmentObject var store: SessionStore
    @EnvironmentObject var premium: PremiumStore
    @State private var playing: Workout?
    @State private var minutes: Int?

    private var profile: Profile { store.profile ?? Profile() }

    /// The chosen length, falling back to a free one when the user isn't Premium.
    private var effectiveMinutes: Int {
        let m = minutes ?? profile.minutes
        return Gate.canUse(length: m, premium: premium.isPremium) ? m : 7
    }

    private var today: Workout {
        WorkoutBuilder.build(profile: profile, minutes: effectiveMinutes,
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
                        Picker("Length", selection: Binding(
                            get: { effectiveMinutes },
                            set: { m in
                                if Gate.canUse(length: m, premium: premium.isPremium) { minutes = m }
                                else { premium.showPaywall = true }
                            })) {
                            ForEach(WorkoutBuilder.lengths, id: \.self) { m in
                                Text(Gate.canUse(length: m, premium: premium.isPremium) ? "\(m)m" : "\(m)m 🔒").tag(m)
                            }
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
            .fullScreenCover(item: $playing, onDismiss: { premium.presentPendingUpsell() }) { w in
                PlayerView(workout: w) { playing = nil }
            }
        }
    }
}
