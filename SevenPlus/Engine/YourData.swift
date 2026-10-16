import Foundation

/// Everything SevenPlus keeps about the user, in one readable JSON file.
struct ExportFile: Codable, Equatable {
    static let currentVersion = 1

    var version = ExportFile.currentVersion
    var exportedAt: Date
    var profile: Profile?
    var sessions: [Session]
    var customWorkouts: [Workout]
    var reminders: ReminderSettings
    var readiness: [String: Readiness]
    var challenges: [Challenge]
    var displayName: String

    static let encoder: JSONEncoder = {
        let e = JSONEncoder()
        e.outputFormatting = [.prettyPrinted, .sortedKeys]
        e.dateEncodingStrategy = .iso8601
        return e
    }()

    static let decoder: JSONDecoder = {
        let d = JSONDecoder()
        d.dateDecodingStrategy = .iso8601
        return d
    }()

    func encoded() throws -> Data { try Self.encoder.encode(self) }

    /// Writes the export to a temporary file named with today's date, ready to share.
    func writeTemporaryFile(now: Date = Date()) throws -> URL {
        let day = ISO8601DateFormatter.string(from: now, timeZone: .current, formatOptions: [.withFullDate])
        let url = FileManager.default.temporaryDirectory.appendingPathComponent("SevenPlus-data-\(day).json")
        try encoded().write(to: url, options: .atomic)
        return url
    }
}

/// Device settings SevenPlus writes outside the database. Erasing removes all of them.
enum StoredKeys {
    static let all = [
        ReminderSettings.key,   // reminderSettings
        "readinessLog",
        "localChallenges",
        "userId",
        "displayName",
        "healthEnabled",
        "upsellShown",
        "countReps",
        "crashReportsEnabled",
        "profile", "quietMode", // Phase 0 leftovers, normally already removed
    ]

    static func removeAll(from defaults: UserDefaults = .standard) {
        for key in all { defaults.removeObject(forKey: key) }
    }
}

/// Erases everything in a safe order: things only reachable with the current
/// identity first (online challenges, Health), then device data, then the
/// database last, which sends the app back to onboarding.
@MainActor
enum DataEraser {
    enum Outcome: Equatable {
        case erased(healthDeleted: Bool)
        /// Nothing was deleted; the challenge server couldn't be reached.
        case remoteFailed(String)
    }

    static func run(skipRemote: Bool, alsoHealth: Bool,
                    store: SessionStore, reminders: ReminderScheduler,
                    health: HealthManager, challenges: ChallengeStore,
                    crash: CrashReporter? = nil,
                    defaults: UserDefaults = .standard) async -> Outcome {
        if !skipRemote && challenges.service.isOnline {
            do { try await challenges.leaveAll() } catch { return .remoteFailed(error.localizedDescription) }
        }
        var healthDeleted = true
        if alsoHealth { healthDeleted = await health.deleteSavedData() }
        StoredKeys.removeAll(from: defaults)
        reminders.resetLocal()
        health.resetLocal()
        crash?.resetLocal()
        challenges.resetLocal(defaults: defaults)
        store.eraseAll()
        return .erased(healthDeleted: healthDeleted)
    }
}

extension ExportFile {
    @MainActor
    static func current(store: SessionStore, reminders: ReminderScheduler, challenges: ChallengeStore,
                        defaults: UserDefaults = .standard, now: Date = Date()) -> ExportFile {
        ExportFile(exportedAt: now, profile: store.profile, sessions: store.sessions,
                   customWorkouts: store.customWorkouts, reminders: reminders.settings,
                   readiness: ReadinessLog.all(defaults: defaults), challenges: challenges.challenges,
                   displayName: challenges.displayName)
    }
}
