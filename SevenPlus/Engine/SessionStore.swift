import Foundation

/// Local-first session history (JSON file in Application Support).
/// Phase 1 replaces this with SwiftData + a sync queue.
@MainActor
final class SessionStore: ObservableObject {
    @Published private(set) var sessions: [Session] = []
    @Published var quietMode = UserDefaults.standard.bool(forKey: "quietMode") {
        didSet { UserDefaults.standard.set(quietMode, forKey: "quietMode") }
    }

    private let url: URL

    init(fileName: String = "sessions.json") {
        let dir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        url = dir.appendingPathComponent(fileName)
        if let data = try? Data(contentsOf: url),
           let saved = try? JSONDecoder().decode([Session].self, from: data) {
            sessions = saved
        }
    }

    func add(_ s: Session) { sessions.insert(s, at: 0); save() }

    func rate(_ id: UUID, _ d: Difficulty) {
        guard let i = sessions.firstIndex(where: { $0.id == id }) else { return }
        sessions[i].difficulty = d
        save()
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

    private func save() {
        if let data = try? JSONEncoder().encode(sessions) { try? data.write(to: url, options: .atomic) }
    }
}
