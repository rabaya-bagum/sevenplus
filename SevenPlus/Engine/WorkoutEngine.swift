import Foundation
import Combine

/// Where the workout is at a given moment.
enum Phase: Equatable {
    case getReady(remaining: Int)
    case work(index: Int, remaining: Int)
    case rest(index: Int, remaining: Int)   // rest *after* step `index`
    case finished
}

/// Pure timeline: maps elapsed seconds to a phase. No clocks, fully unit-testable.
struct Timeline {
    let workout: Workout
    let getReadySeconds: Int

    init(workout: Workout, getReadySeconds: Int = 5) {
        self.workout = workout
        self.getReadySeconds = getReadySeconds
    }

    /// Total length; the rest after the final exercise is dropped.
    var totalSeconds: Int {
        getReadySeconds + workout.totalSeconds - (workout.steps.last?.restSeconds ?? 0)
    }

    func phase(at elapsed: Int) -> Phase {
        var t = elapsed
        if t < getReadySeconds { return .getReady(remaining: getReadySeconds - t) }
        t -= getReadySeconds
        for (i, step) in workout.steps.enumerated() {
            if t < step.workSeconds { return .work(index: i, remaining: step.workSeconds - t) }
            t -= step.workSeconds
            let isLast = i == workout.steps.count - 1
            if isLast { return .finished }
            if t < step.restSeconds { return .rest(index: i, remaining: step.restSeconds - t) }
            t -= step.restSeconds
        }
        return .finished
    }

    /// Elapsed second at which exercise `index` starts working.
    func startOfWork(_ index: Int) -> Int {
        getReadySeconds + workout.steps.prefix(index).reduce(0) { $0 + $1.workSeconds + $1.restSeconds }
    }
}

/// Drives the timeline from wall-clock time, so it stays correct after the
/// app is backgrounded or the phone is locked.
@MainActor
final class WorkoutEngine: ObservableObject {
    @Published private(set) var phase: Phase
    @Published private(set) var isPaused = false
    @Published private(set) var skipped: [String] = []

    let timeline: Timeline
    private var startDate: Date?
    private var offset: TimeInterval = 0     // accumulated elapsed before current run
    private var ticker: AnyCancellable?
    private let now: () -> Date
    var onPhaseChange: ((Phase) -> Void)?

    init(workout: Workout, now: @escaping () -> Date = Date.init) {
        self.timeline = Timeline(workout: workout)
        self.now = now
        self.phase = timeline.phase(at: 0)
    }

    var elapsed: Int {
        let running = startDate.map { now().timeIntervalSince($0) } ?? 0
        return Int(offset + running)
    }

    var progress: Double { min(1, Double(elapsed) / Double(timeline.totalSeconds)) }

    func start() {
        guard startDate == nil else { return }
        startDate = now()
        isPaused = false
        onPhaseChange?(phase)
        ticker = Timer.publish(every: 0.25, on: .main, in: .common).autoconnect()
            .sink { [weak self] _ in self?.tick() }
    }

    func pause() {
        guard let s = startDate else { return }
        offset += now().timeIntervalSince(s)
        startDate = nil
        isPaused = true
        ticker = nil
    }

    func resume() { start() }

    /// Jump to the next exercise (records the current one as skipped if mid-work).
    func skip() {
        let next: Int
        switch phase {
        case .getReady: next = 0
        case .work(let i, _):
            skipped.append(timeline.workout.steps[i].exerciseId)
            next = i + 1
        case .rest(let i, _): next = i + 1
        case .finished: return
        }
        let target = next >= timeline.workout.steps.count ? timeline.totalSeconds : timeline.startOfWork(next)
        offset = TimeInterval(target)
        if startDate != nil { startDate = now() }
        tick()
    }

    func tick() {
        let p = timeline.phase(at: elapsed)
        guard p != phase else { return }
        let changedStage = !sameStage(p, phase)
        phase = p
        if changedStage { onPhaseChange?(p) }
        if p == .finished { ticker = nil; startDate = nil }
    }

    private func sameStage(_ a: Phase, _ b: Phase) -> Bool {
        switch (a, b) {
        case (.getReady, .getReady), (.finished, .finished): return true
        case let (.work(i, _), .work(j, _)): return i == j
        case let (.rest(i, _), .rest(j, _)): return i == j
        default: return false
        }
    }
}
