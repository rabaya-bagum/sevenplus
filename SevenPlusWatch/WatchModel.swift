import Foundation
import WatchConnectivity
import HealthKit

/// Watch side: shows the phone's player, sends button presses back, and runs a
/// HealthKit workout session while a workout is active to stream heart rate.
@MainActor
final class WatchModel: NSObject, ObservableObject {
    @Published private(set) var state = WatchProtocol.PlayerState.idle
    @Published private(set) var heartRate: Int?

    private let health = HKHealthStore()
    private var workoutSession: HKWorkoutSession?
    private var builder: HKLiveWorkoutBuilder?

    override init() {
        super.init()
        if WCSession.isSupported() {
            WCSession.default.delegate = self
            WCSession.default.activate()
        }
    }

    func send(_ command: WatchProtocol.Command) {
        guard WCSession.default.isReachable else { return }
        WCSession.default.sendMessage([WatchProtocol.commandKey: command.rawValue], replyHandler: nil)
    }

    fileprivate func receive(_ new: WatchProtocol.PlayerState) {
        let wasActive = isActive(state.stage)
        state = new
        if !wasActive && isActive(new.stage) { Task { await startHeartRate() } }
        if wasActive && !isActive(new.stage) { stopHeartRate() }
    }

    private func isActive(_ stage: WatchProtocol.PlayerState.Stage) -> Bool {
        stage == .getReady || stage == .work || stage == .rest
    }

    // MARK: - Heart rate

    private func startHeartRate() async {
        guard HKHealthStore.isHealthDataAvailable(), workoutSession == nil else { return }
        let hr = HKQuantityType(.heartRate)
        try? await health.requestAuthorization(toShare: [], read: [hr])

        let config = HKWorkoutConfiguration()
        config.activityType = .highIntensityIntervalTraining
        config.locationType = .indoor
        do {
            let session = try HKWorkoutSession(healthStore: health, configuration: config)
            let builder = session.associatedWorkoutBuilder()
            builder.dataSource = HKLiveWorkoutDataSource(healthStore: health, workoutConfiguration: config)
            builder.delegate = self
            workoutSession = session
            self.builder = builder
            session.startActivity(with: Date())
            try await builder.beginCollection(at: Date())
        } catch {
            workoutSession = nil
            builder = nil
        }
    }

    /// The phone saves the workout to Health, so the watch session is discarded.
    private func stopHeartRate() {
        workoutSession?.end()
        builder?.discardWorkout()
        workoutSession = nil
        builder = nil
        heartRate = nil
    }

    fileprivate func updateHeartRate(_ bpm: Int) {
        heartRate = bpm
        guard WCSession.default.isReachable else { return }
        WCSession.default.sendMessage([WatchProtocol.heartRateKey: bpm], replyHandler: nil)
    }
}

extension WatchModel: WCSessionDelegate {
    nonisolated func session(_ session: WCSession, activationDidCompleteWith state: WCSessionActivationState, error: Error?) {
        let data = session.receivedApplicationContext[WatchProtocol.stateKey]
        if let s = WatchProtocol.decode(WatchProtocol.PlayerState.self, data) {
            Task { @MainActor in self.receive(s) }
        }
    }

    nonisolated func session(_ session: WCSession, didReceiveApplicationContext context: [String: Any]) {
        if let s = WatchProtocol.decode(WatchProtocol.PlayerState.self, context[WatchProtocol.stateKey]) {
            Task { @MainActor in self.receive(s) }
        }
    }

    nonisolated func session(_ session: WCSession, didReceiveMessage message: [String: Any]) {
        if let s = WatchProtocol.decode(WatchProtocol.PlayerState.self, message[WatchProtocol.stateKey]) {
            Task { @MainActor in self.receive(s) }
        }
    }
}

extension WatchModel: HKLiveWorkoutBuilderDelegate {
    nonisolated func workoutBuilderDidCollectEvent(_ workoutBuilder: HKLiveWorkoutBuilder) {}

    nonisolated func workoutBuilder(_ workoutBuilder: HKLiveWorkoutBuilder, didCollectDataOf collectedTypes: Set<HKSampleType>) {
        let hr = HKQuantityType(.heartRate)
        guard collectedTypes.contains(hr),
              let value = workoutBuilder.statistics(for: hr)?.mostRecentQuantity()?
                .doubleValue(for: HKUnit.count().unitDivided(by: .minute())) else { return }
        let bpm = Int(value.rounded())
        Task { @MainActor in self.updateHeartRate(bpm) }
    }
}
