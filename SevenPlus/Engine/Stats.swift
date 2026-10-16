import Foundation

/// Progress numbers derived from session history. Pure and testable.
enum Stats {
    struct Day: Identifiable, Equatable {
        let date: Date          // start of day
        let minutes: Int
        var id: Date { date }
    }

    /// Minutes trained on each of the last `days` days, oldest first, including today.
    static func dailyMinutes(_ sessions: [Session], days: Int = 7,
                             today: Date = Date(), calendar: Calendar = .current) -> [Day] {
        let start = calendar.startOfDay(for: today)
        var seconds: [Date: Int] = [:]
        for s in sessions { seconds[calendar.startOfDay(for: s.date), default: 0] += s.durationSeconds }
        return (0..<days).reversed().map { back in
            let d = calendar.date(byAdding: .day, value: -back, to: start)!
            return Day(date: d, minutes: (seconds[d] ?? 0) / 60)
        }
    }

    static func totalMinutes(_ sessions: [Session]) -> Int {
        sessions.reduce(0) { $0 + $1.durationSeconds } / 60
    }

    /// Longest run of consecutive days with a completed workout.
    static func bestStreak(_ sessions: [Session], calendar: Calendar = .current) -> Int {
        let days = Set(sessions.filter(\.completed).map { calendar.startOfDay(for: $0.date) }).sorted()
        var best = 0, run = 0
        var previous: Date?
        for d in days {
            if let p = previous, calendar.date(byAdding: .day, value: 1, to: p) == d { run += 1 } else { run = 1 }
            best = max(best, run)
            previous = d
        }
        return best
    }
}
