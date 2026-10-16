import SwiftUI

struct TodayView: View {
    @EnvironmentObject var store: SessionStore
    @EnvironmentObject var premium: PremiumStore
    @State private var playing: Workout?
    @State private var minutes: Int?
    @State private var readiness: Readiness? = ReadinessLog.today()
    @State private var draftEnergy: Readiness.Energy?

    private var profile: Profile { store.profile ?? Profile() }

    /// The chosen length, falling back to a free one when the user isn't Premium.
    private var effectiveMinutes: Int {
        let m = minutes ?? profile.minutes
        return Gate.canUse(length: m, premium: premium.isPremium) ? m : 7
    }

    /// Adaptive difficulty is a Premium feature.
    private var adaptation: Adaptation {
        premium.isPremium ? Adaptation.from(sessions: store.sessions) : Adaptation()
    }

    /// Length after the readiness check-in (only when the user hasn't picked one by hand today).
    private var plannedMinutes: Int {
        guard minutes == nil, let r = readiness else { return effectiveMinutes }
        return r.adjustedLength(effectiveMinutes)
    }

    private var intervals: (work: Int, rest: Int) {
        let base = adaptation.apply(to: profile.level.intervals)
        return (base.work, min(30, base.rest + (readiness?.extraRest ?? 0)))
    }

    private var today: Workout {
        WorkoutBuilder.build(profile: profile, minutes: plannedMinutes,
                             dayIndex: WorkoutBuilder.dayIndex(), intervals: intervals)
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
                            get: { plannedMinutes },
                            set: { m in
                                if Gate.canUse(length: m, premium: premium.isPremium) { minutes = m }
                                else { premium.showPaywall = true }
                            })) {
                            ForEach(WorkoutBuilder.lengths, id: \.self) { m in
                                Text("\(m)m").tag(m)
                            }
                        }
                        .pickerStyle(.segmented)
                        // A lock in each segment gets truncated, so name the locked lengths once.
                        let locked = WorkoutBuilder.lengths.filter { !Gate.canUse(length: $0, premium: premium.isPremium) }
                        if !locked.isEmpty {
                            Label("\(locked.map { "\($0)" }.joined(separator: ", ")) min with Premium", systemImage: "lock.fill")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Text("\(today.steps.count) exercises · \(intervals.work)s on / \(intervals.rest)s rest")
                            .foregroundStyle(.secondary)
                        if let note = readiness?.note {
                            Label(note, systemImage: "heart.text.square").font(.subheadline)
                        }
                        if let summary = adaptation.summary {
                            Label(summary, systemImage: "slider.horizontal.3").font(.footnote).foregroundStyle(.secondary)
                        }
                        Button { playing = today } label: {
                            Text("Start").font(.headline).frame(maxWidth: .infinity).padding(.vertical, 6)
                        }
                        .buttonStyle(.borderedProminent)
                    }
                    .padding(.vertical, 6)
                }

                if readiness == nil {
                    Section("How do you feel today?") {
                        if let energy = draftEnergy {
                            Text("Any soreness?").font(.subheadline)
                            choiceRow(Readiness.Soreness.allCases, labels: ["None", "A little", "Very"]) { s in
                                let r = Readiness(energy: energy, soreness: s)
                                ReadinessLog.record(r)
                                readiness = r
                            }
                        } else {
                            Text("Energy").font(.subheadline)
                            choiceRow(Readiness.Energy.allCases, labels: ["Low", "OK", "High"]) { draftEnergy = $0 }
                        }
                    }
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
            .onAppear { readiness = ReadinessLog.today() }
            .navigationDestination(for: Exercise.self) { ExerciseDetailView(exercise: $0) }
            .fullScreenCover(item: $playing, onDismiss: { premium.presentPendingUpsell() }) { w in
                PlayerView(workout: w) { playing = nil }
            }
        }
    }

    private func choiceRow<T>(_ items: [T], labels: [String], pick: @escaping (T) -> Void) -> some View {
        HStack {
            ForEach(Array(zip(items.indices, labels)), id: \.0) { i, label in
                Button(label) { pick(items[i]) }
                    .buttonStyle(.bordered)
                    .frame(maxWidth: .infinity)
            }
        }
    }
}
