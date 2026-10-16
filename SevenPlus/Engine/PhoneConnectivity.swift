import Foundation
import WatchConnectivity

/// iPhone side of the Watch link: sends the player state, receives button
/// presses and live heart rate from the watch.
@MainActor
final class PhoneConnectivity: NSObject, ObservableObject {
    static let shared = PhoneConnectivity()

    @Published private(set) var heartRate: Int?
    /// Set by the player while it is on screen.
    var onCommand: ((WatchProtocol.Command) -> Void)?

    private var session: WCSession? { WCSession.isSupported() ? WCSession.default : nil }

    func activate() {
        guard let session, session.activationState != .activated else { return }
        session.delegate = self
        session.activate()
    }

    var watchAvailable: Bool { session?.isPaired == true && session?.isWatchAppInstalled == true }

    func send(_ state: WatchProtocol.PlayerState) {
        guard let session, session.activationState == .activated, session.isWatchAppInstalled,
              let data = WatchProtocol.encode(state) else { return }
        let payload = [WatchProtocol.stateKey: data]
        // Application context always holds the latest state, even if the watch is asleep.
        try? session.updateApplicationContext(payload)
        if session.isReachable { session.sendMessage(payload, replyHandler: nil) }
    }

    func clearHeartRate() { heartRate = nil }

    fileprivate func handle(command: String?, bpm: Int?) {
        if let command, let cmd = WatchProtocol.Command(rawValue: command) { onCommand?(cmd) }
        if let bpm { heartRate = bpm }
    }
}

extension PhoneConnectivity: WCSessionDelegate {
    nonisolated func session(_ session: WCSession, activationDidCompleteWith state: WCSessionActivationState, error: Error?) {}
    nonisolated func sessionDidBecomeInactive(_ session: WCSession) {}
    nonisolated func sessionDidDeactivate(_ session: WCSession) { session.activate() }

    nonisolated func session(_ session: WCSession, didReceiveMessage message: [String: Any]) {
        let command = message[WatchProtocol.commandKey] as? String
        let bpm = message[WatchProtocol.heartRateKey] as? Int
        Task { @MainActor in PhoneConnectivity.shared.handle(command: command, bpm: bpm) }
    }
}

extension WatchProtocol.PlayerState {
    /// Builds the watch state from the phone's engine.
    @MainActor
    static func from(_ engine: WorkoutEngine) -> WatchProtocol.PlayerState {
        let steps = engine.workout.steps
        func name(_ i: Int) -> String { ExerciseLibrary.exercise(steps[i].exerciseId).name }
        switch engine.phase {
        case .getReady(let r):
            return .init(stage: .getReady, title: "Get ready", next: steps.isEmpty ? nil : name(0), remaining: r,
                         stepIndex: 0, stepCount: steps.count, isPaused: engine.isPaused, sentAt: Date())
        case .work(let i, let r):
            return .init(stage: .work, title: name(i), next: nil, remaining: r,
                         stepIndex: i, stepCount: steps.count, isPaused: engine.isPaused, sentAt: Date())
        case .rest(let i, let r):
            return .init(stage: .rest, title: "Rest", next: name(i + 1), remaining: r,
                         stepIndex: i, stepCount: steps.count, isPaused: engine.isPaused, sentAt: Date())
        case .finished:
            return .init(stage: .finished, title: "Done!", next: nil, remaining: 0,
                         stepIndex: steps.count - 1, stepCount: steps.count, isPaused: false, sentAt: Date())
        }
    }
}
