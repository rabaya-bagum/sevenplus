import Foundation

/// How much to change the level's base intervals, learned from difficulty ratings.
struct Adaptation: Equatable {
    var workDelta = 0
    var restDelta = 0

    static let workRange = -10...20
    static let restRange = -5...10
    static let step = 5

    /// Rules v1 (from the spec), applied oldest rating first:
    /// - "Too hard": 5 s less work, 5 s more rest.
    /// - "Too easy" twice in a row: 5 s more work, 5 s less rest.
    /// - "Just right" keeps things as they are and resets the easy count.
    static func from(ratings: [Difficulty]) -> Adaptation {
        var a = Adaptation()
        var easyRun = 0
        for r in ratings {
            switch r {
            case .tooHard:
                easyRun = 0
                a.workDelta = clamp(a.workDelta - step, workRange)
                a.restDelta = clamp(a.restDelta + step, restRange)
            case .tooEasy:
                easyRun += 1
                if easyRun == 2 {
                    easyRun = 0
                    a.workDelta = clamp(a.workDelta + step, workRange)
                    a.restDelta = clamp(a.restDelta - step, restRange)
                }
            case .justRight:
                easyRun = 0
            }
        }
        return a
    }

    /// Ratings from rated sessions, oldest first. Only the last 10 count, so old
    /// feedback fades out.
    static func from(sessions: [Session]) -> Adaptation {
        let rated = sessions.filter { $0.difficulty != nil }.sorted { $0.date < $1.date }
        return from(ratings: rated.suffix(10).compactMap(\.difficulty))
    }

    func apply(to base: (work: Int, rest: Int)) -> (work: Int, rest: Int) {
        (Self.clamp(base.work + workDelta, 15...60), Self.clamp(base.rest + restDelta, 5...30))
    }

    var summary: String? {
        guard self != Adaptation() else { return nil }
        func signed(_ v: Int) -> String { v > 0 ? "+\(v)" : "\(v)" }
        return "Adjusted from your feedback: work \(signed(workDelta)) s, rest \(signed(restDelta)) s"
    }

    private static func clamp(_ v: Int, _ r: ClosedRange<Int>) -> Int { min(max(v, r.lowerBound), r.upperBound) }
}

/// A 2-tap daily check-in.
struct Readiness: Codable, Equatable {
    enum Energy: Int, Codable, CaseIterable { case low = 1, ok = 2, high = 3 }
    enum Soreness: Int, Codable, CaseIterable { case none = 1, some = 2, very = 3 }

    var energy: Energy
    var soreness: Soreness

    /// Low energy drops one length step (never below 5 min); low energy and very
    /// sore together go straight to 5 minutes.
    func adjustedLength(_ minutes: Int, lengths: [Int] = WorkoutBuilder.lengths) -> Int {
        if energy == .low && soreness == .very { return lengths.first ?? 5 }
        guard energy == .low, let i = lengths.firstIndex(of: minutes), i > 0 else { return minutes }
        return lengths[i - 1]
    }

    /// Very sore adds 5 s of rest.
    var extraRest: Int { soreness == .very ? 5 : 0 }

    var note: String? {
        switch (energy, soreness) {
        case (.low, .very): return "Taking it easy today: a short, gentle session."
        case (.low, _): return "Low energy, so today's workout is a bit shorter."
        case (_, .very): return "Extra rest between exercises today."
        case (.high, .none): return "Feeling great. Go get it!"
        default: return nil
        }
    }
}

/// Stores today's check-in on this device, keyed by calendar day.
enum ReadinessLog {
    private static let key = "readinessLog"

    private static func dayKey(_ date: Date, _ cal: Calendar) -> String {
        let c = cal.dateComponents([.year, .month, .day], from: date)
        return String(format: "%04d-%02d-%02d", c.year!, c.month!, c.day!)
    }

    static func today(_ date: Date = Date(), calendar: Calendar = .current,
                      defaults: UserDefaults = .standard) -> Readiness? {
        guard let data = defaults.data(forKey: key),
              let log = try? JSONDecoder().decode([String: Readiness].self, from: data) else { return nil }
        return log[dayKey(date, calendar)]
    }

    /// Every saved check-in, keyed "yyyy-MM-dd".
    static func all(defaults: UserDefaults = .standard) -> [String: Readiness] {
        defaults.data(forKey: key).flatMap { try? JSONDecoder().decode([String: Readiness].self, from: $0) } ?? [:]
    }

    static func clear(defaults: UserDefaults = .standard) { defaults.removeObject(forKey: key) }

    static func record(_ r: Readiness, date: Date = Date(), calendar: Calendar = .current,
                       defaults: UserDefaults = .standard) {
        var log = (defaults.data(forKey: key)).flatMap { try? JSONDecoder().decode([String: Readiness].self, from: $0) } ?? [:]
        log[dayKey(date, calendar)] = r
        if log.count > 60 {   // keep roughly two months
            log = Dictionary(uniqueKeysWithValues: log.sorted { $0.key > $1.key }.prefix(60).map { ($0.key, $0.value) })
        }
        if let data = try? JSONEncoder().encode(log) { defaults.set(data, forKey: key) }
    }
}
