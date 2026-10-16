import SwiftUI

struct PlayerView: View {
    let workout: Workout
    let onClose: () -> Void

    @EnvironmentObject var store: SessionStore
    @StateObject private var engine: WorkoutEngine
    @State private var coach = Coach()
    @State private var finishedSession: Session?
    @State private var showingSwaps = false
    @Environment(\.scenePhase) private var scenePhase

    init(workout: Workout, onClose: @escaping () -> Void) {
        self.workout = workout
        self.onClose = onClose
        _engine = StateObject(wrappedValue: WorkoutEngine(workout: workout))
    }

    var body: some View {
        Group {
            if let s = finishedSession {
                SummaryView(session: s, totalExercises: engine.workout.steps.count, onDone: onClose)
            } else {
                player
            }
        }
        .onAppear {
            UIApplication.shared.isIdleTimerDisabled = true
            engine.onPhaseChange = { p in
                coach.announce(p, workout: engine.workout)
                if p == .finished { finish(completed: true) }
            }
            engine.start()
        }
        .onDisappear { UIApplication.shared.isIdleTimerDisabled = false }
        .onChange(of: scenePhase) { _, new in
            if new == .active { engine.tick() }  // catch up after backgrounding
        }
    }

    private var player: some View {
        VStack(spacing: 24) {
            HStack {
                Button("End") { finish(completed: false) }
                Spacer()
                Text(stepLabel).font(.subheadline).foregroundStyle(.secondary)
            }
            .padding(.horizontal)

            ProgressView(value: engine.progress).padding(.horizontal)

            Spacer()

            Text(title).font(.largeTitle.bold()).multilineTextAlignment(.center)
            if let ex = currentExercise {
                Image(systemName: ex.symbol).font(.system(size: 96)).foregroundStyle(tint)
                    .accessibilityHidden(true)
                Text(ex.cue).font(.title3).foregroundStyle(.secondary).multilineTextAlignment(.center)
            }

            ZStack {
                Circle().stroke(.quaternary, lineWidth: 14)
                Circle().trim(from: 0, to: ringFraction)
                    .stroke(tint, style: StrokeStyle(lineWidth: 14, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .animation(.linear(duration: 0.25), value: ringFraction)
                Text("\(remaining)").font(.system(size: 64, weight: .bold, design: .rounded))
                    .monospacedDigit()
            }
            .frame(width: 200, height: 200)
            .accessibilityElement()
            .accessibilityLabel("\(remaining) seconds remaining")

            if case .rest(let i, _) = engine.phase {
                Text("Next: \(ExerciseLibrary.exercise(engine.workout.steps[i + 1].exerciseId).name)")
                    .font(.headline)
            }

            Spacer()

            HStack(spacing: 40) {
                Button { engine.isPaused ? engine.resume() : engine.pause() } label: {
                    Image(systemName: engine.isPaused ? "play.circle.fill" : "pause.circle.fill")
                        .font(.system(size: 64))
                }
                .accessibilityLabel(engine.isPaused ? "Resume" : "Pause")
                Button { engine.skip() } label: {
                    Image(systemName: "forward.end.circle").font(.system(size: 48))
                }
                .accessibilityLabel("Skip")
                Button { showingSwaps = true } label: {
                    Image(systemName: "arrow.triangle.2.circlepath.circle").font(.system(size: 48))
                }
                .accessibilityLabel(swapLabel)
                .disabled(swapOptions.isEmpty)
            }
            .padding(.bottom)
            .confirmationDialog(swapLabel, isPresented: $showingSwaps, titleVisibility: .visible) {
                ForEach(swapOptions) { ex in
                    Button(ex.name) { engine.swap(to: ex.id) }
                }
            }
        }
        .padding(.top)
    }

    // MARK: - Derived display state

    private var currentExercise: Exercise? {
        switch engine.phase {
        case .work(let i, _): return ExerciseLibrary.exercise(engine.workout.steps[i].exerciseId)
        case .getReady: return ExerciseLibrary.exercise(engine.workout.steps[0].exerciseId)
        default: return nil
        }
    }

    private var title: String {
        switch engine.phase {
        case .getReady: return "Get ready"
        case .work: return currentExercise?.name ?? ""
        case .rest: return "Rest"
        case .finished: return "Done!"
        }
    }

    private var stepLabel: String {
        switch engine.phase {
        case .work(let i, _), .rest(let i, _): return "\(i + 1) of \(engine.workout.steps.count)"
        default: return ""
        }
    }

    private var remaining: Int {
        switch engine.phase {
        case .getReady(let r), .work(_, let r), .rest(_, let r): return r
        case .finished: return 0
        }
    }

    private var phaseLength: Int {
        switch engine.phase {
        case .getReady: return engine.timeline.getReadySeconds
        case .work(let i, _): return engine.workout.steps[i].workSeconds
        case .rest(let i, _): return engine.workout.steps[i].restSeconds
        case .finished: return 1
        }
    }

    private var swapTarget: Exercise? {
        engine.swapTargetIndex.flatMap { i in
            i < engine.workout.steps.count ? ExerciseLibrary.exercise(engine.workout.steps[i].exerciseId) : nil
        }
    }

    private var swapLabel: String {
        if case .rest = engine.phase { return "Swap next exercise" }
        return "Swap \(swapTarget?.name ?? "exercise")"
    }

    private var swapOptions: [Exercise] {
        guard let target = swapTarget else { return [] }
        return WorkoutBuilder.swapOptions(for: target.id, limits: store.profile?.limitations ?? [])
    }

    private var ringFraction: Double { Double(remaining) / Double(max(1, phaseLength)) }

    private var tint: Color {
        if case .rest = engine.phase { return .green }
        return .orange
    }

    private func finish(completed: Bool) {
        guard finishedSession == nil else { return }
        engine.pause()
        let s = Session(id: UUID(), workoutId: workout.id, date: Date(),
                        durationSeconds: engine.elapsed, completed: completed,
                        skipped: engine.skipped, difficulty: nil)
        store.add(s)
        if completed { finishedSession = s } else { onClose() }
    }
}
