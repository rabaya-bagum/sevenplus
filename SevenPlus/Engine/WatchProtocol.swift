import Foundation

/// Messages between the iPhone and Apple Watch apps. This file belongs to BOTH
/// targets (see README → Apple Watch).
enum WatchProtocol {
    /// What the phone's player is showing right now.
    struct PlayerState: Codable, Equatable {
        enum Stage: String, Codable { case idle, getReady, work, rest, finished }
        var stage: Stage
        var title: String           // exercise name, "Rest", "Get ready"
        var next: String?           // upcoming exercise during rest
        var remaining: Int          // seconds left in this stage
        var stepIndex: Int          // 0-based
        var stepCount: Int
        var isPaused: Bool
        var sentAt: Date            // lets the watch count down between updates

        static let idle = PlayerState(stage: .idle, title: "Start on iPhone", next: nil, remaining: 0,
                                      stepIndex: 0, stepCount: 0, isPaused: false, sentAt: .distantPast)

        /// Seconds left now, counting down locally from when the phone sent it.
        func remaining(at now: Date) -> Int {
            guard !isPaused, stage == .work || stage == .rest || stage == .getReady else { return remaining }
            return max(0, remaining - Int(now.timeIntervalSince(sentAt)))
        }
    }

    enum Command: String, Codable { case pause, resume, skip, end }

    static let stateKey = "state"
    static let commandKey = "command"
    static let heartRateKey = "heartRate"

    static func encode<T: Encodable>(_ value: T) -> Data? { try? JSONEncoder().encode(value) }
    static func decode<T: Decodable>(_ type: T.Type, _ data: Any?) -> T? {
        guard let data = data as? Data else { return nil }
        return try? JSONDecoder().decode(type, from: data)
    }
}
