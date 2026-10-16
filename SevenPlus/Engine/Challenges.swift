import Foundation

/// "Complete N workouts in D days" with friends.
struct Challenge: Codable, Identifiable, Equatable {
    struct Participant: Codable, Identifiable, Equatable {
        let id: String          // device/user id
        var name: String
        var completed: Int
    }

    let id: String
    let code: String            // 6-character invite code
    var title: String
    var target: Int             // workouts to complete
    let start: Date
    let end: Date
    var participants: [Participant]

    func isActive(at now: Date = Date()) -> Bool { now >= start && now < end }

    func daysLeft(at now: Date = Date(), calendar: Calendar = .current) -> Int {
        max(0, calendar.dateComponents([.day], from: calendar.startOfDay(for: now),
                                       to: calendar.startOfDay(for: end)).day ?? 0)
    }

    /// Highest completed first; ties keep join order.
    var leaderboard: [Participant] {
        participants.enumerated()
            .sorted { ($0.element.completed, -$0.offset) > ($1.element.completed, -$1.offset) }
            .map(\.element)
    }

    static func make(title: String, target: Int, days: Int, owner: Participant,
                     start: Date = Date(), calendar: Calendar = .current) -> Challenge {
        let s = calendar.startOfDay(for: start)
        return Challenge(id: UUID().uuidString, code: InviteCode.generate(), title: title, target: target,
                         start: s, end: calendar.date(byAdding: .day, value: days, to: s)!, participants: [owner])
    }

    /// Completed workouts that fall inside the challenge window.
    static func progress(of sessions: [Session], in challenge: Challenge) -> Int {
        sessions.filter { $0.completed && $0.date >= challenge.start && $0.date < challenge.end }.count
    }
}

enum InviteCode {
    /// No 0/O or 1/I so codes are easy to read aloud.
    static let alphabet = Array("ABCDEFGHJKLMNPQRSTUVWXYZ23456789")

    static func generate<R: RandomNumberGenerator>(using rng: inout R) -> String {
        String((0..<6).map { _ in alphabet.randomElement(using: &rng)! })
    }

    static func generate() -> String {
        var rng = SystemRandomNumberGenerator()
        return generate(using: &rng)
    }

    static func normalize(_ raw: String) -> String? {
        let code = raw.uppercased().filter { !$0.isWhitespace && $0 != "-" }
        return code.count == 6 && code.allSatisfy(alphabet.contains) ? code : nil
    }
}

enum ChallengeError: LocalizedError {
    case notFound, network(String), notConfigured
    var errorDescription: String? {
        switch self {
        case .notFound: return "No challenge with that code."
        case .network(let m): return "Couldn't reach the server: \(m)"
        case .notConfigured: return "Joining friends' challenges needs the online service. See docs/BACKEND.md."
        }
    }
}

/// Where challenges live. `LocalChallengeService` works offline on this device;
/// `SupabaseChallengeService` shares them between friends.
protocol ChallengeService {
    var isOnline: Bool { get }
    func list(for userId: String) async throws -> [Challenge]
    func create(_ c: Challenge) async throws
    func join(code: String, as participant: Challenge.Participant) async throws -> Challenge
    func report(challengeId: String, participant: Challenge.Participant) async throws
    /// Removes this user from every challenge (used when erasing data).
    func removeParticipant(userId: String) async throws
}

/// On-device only: you can track your own challenges; friends can't join.
final class LocalChallengeService: ChallengeService {
    private let defaults: UserDefaults
    private let key = "localChallenges"
    init(defaults: UserDefaults = .standard) { self.defaults = defaults }

    var isOnline: Bool { false }

    private var all: [Challenge] {
        get { defaults.data(forKey: key).flatMap { try? JSONDecoder().decode([Challenge].self, from: $0) } ?? [] }
        set { defaults.set(try? JSONEncoder().encode(newValue), forKey: key) }
    }

    func list(for userId: String) async throws -> [Challenge] {
        all.filter { c in c.participants.contains { $0.id == userId } }
    }

    func create(_ c: Challenge) async throws { all.append(c) }

    func join(code: String, as participant: Challenge.Participant) async throws -> Challenge {
        guard var c = all.first(where: { $0.code == code }) else { throw ChallengeError.notConfigured }
        if !c.participants.contains(where: { $0.id == participant.id }) { c.participants.append(participant) }
        try await save(c)
        return c
    }

    func report(challengeId: String, participant: Challenge.Participant) async throws {
        guard var c = all.first(where: { $0.id == challengeId }),
              let i = c.participants.firstIndex(where: { $0.id == participant.id }) else { return }
        c.participants[i] = participant
        try await save(c)
    }

    /// Challenges left with nobody in them are deleted.
    func removeParticipant(userId: String) async throws {
        var list = all
        for i in list.indices { list[i].participants.removeAll { $0.id == userId } }
        all = list.filter { !$0.participants.isEmpty }
    }

    private func save(_ c: Challenge) async throws {
        var list = all
        if let i = list.firstIndex(where: { $0.id == c.id }) { list[i] = c } else { list.append(c) }
        all = list
    }
}
