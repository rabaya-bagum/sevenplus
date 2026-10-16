import Foundation
import SwiftData

/// App data facade. Views use plain `Session` / `Profile` values; storage is
/// SwiftData underneath. On first launch after upgrading it imports the
/// Phase 0 JSON history and the UserDefaults profile, then removes them.
@MainActor
final class SessionStore: ObservableObject {
    @Published private(set) var sessions: [Session] = []

    /// nil until onboarding is finished.
    @Published var profile: Profile? = nil {
        didSet { saveProfile() }
    }

    let context: ModelContext

    init(container: ModelContainer = AppSchema.container(),
         defaults: UserDefaults = .standard,
         legacyFile: URL? = SessionStore.defaultLegacyFile) {
        context = ModelContext(container)
        context.autosaveEnabled = false
        Self.importLegacy(into: context, defaults: defaults, file: legacyFile)
        reload()
    }

    // MARK: - Sessions

    func add(_ s: Session) {
        context.insert(SessionRecord(s))
        persist()
        sessions.insert(s, at: 0)
    }

    func rate(_ id: UUID, _ d: Difficulty) {
        let descriptor = FetchDescriptor<SessionRecord>(predicate: #Predicate { $0.id == id })
        guard let record = try? context.fetch(descriptor).first else { return }
        record.difficultyRaw = d.rawValue
        persist()
        if let i = sessions.firstIndex(where: { $0.id == id }) { sessions[i].difficulty = d }
    }

    /// Consecutive days (ending today or yesterday) with a completed workout.
    var streak: Int { Self.streak(for: sessions.filter(\.completed).map(\.date)) }

    nonisolated static func streak(for dates: [Date], today: Date = Date(), calendar: Calendar = .current) -> Int {
        let days = Set(dates.map { calendar.startOfDay(for: $0) })
        var day = calendar.startOfDay(for: today)
        if !days.contains(day) { day = calendar.date(byAdding: .day, value: -1, to: day)! }
        var count = 0
        while days.contains(day) {
            count += 1
            day = calendar.date(byAdding: .day, value: -1, to: day)!
        }
        return count
    }

    // MARK: - Loading and saving

    private func reload() {
        let byNewest = FetchDescriptor<SessionRecord>(sortBy: [SortDescriptor(\.date, order: .reverse)])
        sessions = ((try? context.fetch(byNewest)) ?? []).map(\.value)
        if let record = try? context.fetch(FetchDescriptor<ProfileRecord>()).first {
            profile = try? JSONDecoder().decode(Profile.self, from: record.data)
        }
    }

    private func saveProfile() {
        let existing = (try? context.fetch(FetchDescriptor<ProfileRecord>())) ?? []
        guard let p = profile, let data = try? JSONEncoder().encode(p) else {
            for record in existing { context.delete(record) }
            persist()
            return
        }
        if let record = existing.first {
            guard record.data != data else { return }   // reload() sets profile; skip a no-op write
            record.data = data
            record.updatedAt = Date()
        } else {
            context.insert(ProfileRecord(data: data))
        }
        persist()
    }

    private func persist() {
        do { try context.save() } catch { assertionFailure("Save failed: \(error)") }
    }

    // MARK: - One-time import of Phase 0 data

    nonisolated static var defaultLegacyFile: URL {
        FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("sessions.json")
    }

    static func importLegacy(into context: ModelContext, defaults: UserDefaults, file: URL?) {
        var changed = false

        if let file, let data = try? Data(contentsOf: file),
           let old = try? JSONDecoder().decode([Session].self, from: data) {
            let existing = Set(((try? context.fetch(FetchDescriptor<SessionRecord>())) ?? []).map(\.id))
            for s in old where !existing.contains(s.id) { context.insert(SessionRecord(s)) }
            changed = true
        }

        if let data = defaults.data(forKey: "profile"),
           (try? context.fetchCount(FetchDescriptor<ProfileRecord>())) == 0,
           (try? JSONDecoder().decode(Profile.self, from: data)) != nil {
            context.insert(ProfileRecord(data: data))
            changed = true
        }

        guard changed else { return }
        do {
            try context.save()
            // Only remove the old data once it is safely in the database.
            if let file { try? FileManager.default.removeItem(at: file) }
            defaults.removeObject(forKey: "profile")
            defaults.removeObject(forKey: "quietMode")
        } catch {
            assertionFailure("Legacy import failed: \(error)")
        }
    }
}
