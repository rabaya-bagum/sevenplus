import Foundation
import SwiftData

/// SwiftData row for one workout session. Views keep using the plain `Session`
/// value type; `SessionStore` converts between the two.
@Model
final class SessionRecord {
    @Attribute(.unique) var id: UUID
    var workoutId: String
    var date: Date
    var durationSeconds: Int
    var completed: Bool
    var skipped: [String]
    var difficultyRaw: String?

    init(_ s: Session) {
        id = s.id
        workoutId = s.workoutId
        date = s.date
        durationSeconds = s.durationSeconds
        completed = s.completed
        skipped = s.skipped
        difficultyRaw = s.difficulty?.rawValue
    }

    var value: Session {
        Session(id: id, workoutId: workoutId, date: date, durationSeconds: durationSeconds,
                completed: completed, skipped: skipped,
                difficulty: difficultyRaw.flatMap(Difficulty.init(rawValue:)))
    }
}

/// Single-row table holding the user's profile, stored as encoded JSON so new
/// Profile fields never need a schema migration.
@Model
final class ProfileRecord {
    var data: Data
    var updatedAt: Date

    init(data: Data) {
        self.data = data
        updatedAt = Date()
    }
}

/// A workout the user built. Steps are stored as encoded JSON.
@Model
final class CustomWorkoutRecord {
    @Attribute(.unique) var id: String
    var name: String
    var stepsData: Data
    var createdAt: Date

    init(_ w: Workout) {
        id = w.id
        name = w.name
        stepsData = (try? JSONEncoder().encode(w.steps)) ?? Data()
        createdAt = Date()
    }

    func update(from w: Workout) {
        name = w.name
        stepsData = (try? JSONEncoder().encode(w.steps)) ?? Data()
    }

    var value: Workout {
        Workout(id: id, name: name, steps: (try? JSONDecoder().decode([WorkoutStep].self, from: stepsData)) ?? [])
    }
}

enum AppSchema {
    static let models: [any PersistentModel.Type] = [SessionRecord.self, ProfileRecord.self, CustomWorkoutRecord.self]

    static func container(inMemory: Bool = false) -> ModelContainer {
        let config = ModelConfiguration(isStoredInMemoryOnly: inMemory)
        do {
            return try ModelContainer(for: Schema(models), configurations: config)
        } catch {
            fatalError("Could not open the SevenPlus database: \(error)")
        }
    }
}
