import Foundation

enum Impact: String, Codable { case low, high }

struct Exercise: Identifiable, Codable, Hashable {
    let id: String
    let name: String
    let cue: String          // one-line form cue spoken by the coach
    let impact: Impact
    let symbol: String       // SF Symbol placeholder until videos exist
    var quietSwapId: String? // low-impact alternative for quiet mode
}

struct WorkoutStep: Codable, Hashable {
    let exerciseId: String
    let workSeconds: Int
    let restSeconds: Int
}

struct Workout: Identifiable, Codable, Hashable {
    let id: String
    let name: String
    let steps: [WorkoutStep]
    var totalSeconds: Int { steps.reduce(0) { $0 + $1.workSeconds + $1.restSeconds } }

    /// Same workout with step `index` doing a different exercise (timing unchanged).
    func replacing(at index: Int, with exerciseId: String) -> Workout {
        var newSteps = steps
        let old = newSteps[index]
        newSteps[index] = WorkoutStep(exerciseId: exerciseId, workSeconds: old.workSeconds, restSeconds: old.restSeconds)
        return Workout(id: id, name: name, steps: newSteps)
    }
}

enum Difficulty: String, Codable, CaseIterable {
    case tooEasy = "Too easy", justRight = "Just right", tooHard = "Too hard"
}

struct Session: Identifiable, Codable {
    let id: UUID
    let workoutId: String
    let date: Date
    let durationSeconds: Int
    let completed: Bool
    let skipped: [String]
    var difficulty: Difficulty?
}
