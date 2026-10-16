import Foundation

enum Goal: String, Codable, CaseIterable, Identifiable {
    case getFit = "Get fit", loseWeight = "Lose weight", getStrong = "Get stronger"
    var id: String { rawValue }
}

enum Level: String, Codable, CaseIterable, Identifiable {
    case beginner = "Beginner", intermediate = "Intermediate", advanced = "Advanced"
    var id: String { rawValue }

    /// Work / rest seconds per exercise.
    var intervals: (work: Int, rest: Int) {
        switch self {
        case .beginner: return (30, 15)
        case .intermediate: return (30, 10)
        case .advanced: return (40, 10)
        }
    }
}

/// Body areas a user wants to protect. `noJumping` is the same as quiet mode.
enum Limitation: String, Codable, CaseIterable, Identifiable, Hashable {
    case knees = "Knees", wrists = "Wrists", back = "Lower back", noJumping = "No jumping (quiet)"
    var id: String { rawValue }
}

enum BodyArea: String, Codable { case cardio, lower, upper, core }

struct Profile: Codable, Equatable {
    var goal: Goal = .getFit
    var level: Level = .beginner
    var minutes: Int = 7                 // one of WorkoutBuilder.lengths
    var limitations: Set<Limitation> = []
}
