import Foundation

/// Builds the day's workout from the user's profile. Pure and deterministic:
/// the same profile and day always give the same workout.
enum WorkoutBuilder {
    static let lengths = [5, 7, 10, 15, 20]

    /// Exercises that are safe for every one of the user's limitations.
    static func safePool(for limits: Set<Limitation>) -> [Exercise] {
        ExerciseLibrary.all.filter { ex in
            ExerciseTraits.of(ex.id).stresses.isDisjoint(with: limits)
        }
    }

    /// Moves that work the same body area, are safe for the user, and differ from `exerciseId`.
    /// Low-impact options come first.
    static func swapOptions(for exerciseId: String, limits: Set<Limitation>) -> [Exercise] {
        let area = ExerciseTraits.of(exerciseId).area
        return safePool(for: limits)
            .filter { $0.id != exerciseId && ExerciseTraits.of($0.id).area == area }
            .sorted { ($0.impact == .low ? 0 : 1, $0.name) < ($1.impact == .low ? 0 : 1, $1.name) }
    }

    /// `intervals` overrides the level's work/rest (used by the adaptive engine).
    static func build(profile: Profile, minutes: Int? = nil, dayIndex: Int = 0,
                      intervals: (work: Int, rest: Int)? = nil) -> Workout {
        let mins = minutes ?? profile.minutes
        let (work, rest) = intervals ?? profile.level.intervals
        let count = max(3, (mins * 60 + rest) / (work + rest))  // final rest is dropped

        let pool = safePool(for: profile.limitations)
        // Prefer full moves; fall back to swap-only moves for an area that has none left.
        let full = pool.filter { !ExerciseTraits.of($0.id).swapOnly }
        let order: [BodyArea] = [.cardio, .lower, .upper, .core]
        var queues: [BodyArea: [Exercise]] = [:]
        for area in order {
            var list = full.filter { ExerciseTraits.of($0.id).area == area }
            if list.isEmpty { list = pool.filter { ExerciseTraits.of($0.id).area == area } }
            if !list.isEmpty {
                let shift = dayIndex % list.count          // rotate for day-to-day variety
                list = Array(list[shift...] + list[..<shift])
            }
            queues[area] = list
        }
        let areas = order.filter { !(queues[$0] ?? []).isEmpty }
        guard !areas.isEmpty else { return ExerciseLibrary.classic }

        var steps: [WorkoutStep] = []
        var round = 0
        while steps.count < count {
            for area in areas where steps.count < count {
                let list = queues[area]!
                let ex = list[round % list.count]
                steps.append(WorkoutStep(exerciseId: ex.id, workSeconds: work, restSeconds: rest))
            }
            round += 1
        }
        return Workout(id: "daily-\(dayIndex)-\(mins)", name: "\(mins)-Minute \(profile.goal.rawValue.capitalized)", steps: steps)
    }

    /// Days since 1 Jan 2001 in the user's calendar, used to vary the daily workout.
    static func dayIndex(for date: Date = Date(), calendar: Calendar = .current) -> Int {
        calendar.dateComponents([.day], from: Date(timeIntervalSinceReferenceDate: 0), to: calendar.startOfDay(for: date)).day ?? 0
    }
}
