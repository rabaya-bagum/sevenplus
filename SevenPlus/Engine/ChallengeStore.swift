import Foundation

/// Keeps the user's challenges and reports their progress.
@MainActor
final class ChallengeStore: ObservableObject {
    @Published private(set) var challenges: [Challenge] = []
    @Published var error: String?
    @Published var displayName: String {
        didSet { UserDefaults.standard.set(displayName, forKey: "displayName") }
    }

    let service: ChallengeService
    let userId: String

    init(service: ChallengeService? = nil, defaults: UserDefaults = .standard) {
        if let service {
            self.service = service
        } else if let online = SupabaseChallengeService.fromBundle() {
            self.service = online
        } else {
            self.service = LocalChallengeService(defaults: defaults)
        }
        if let id = defaults.string(forKey: "userId") {
            userId = id
        } else {
            let id = UUID().uuidString
            defaults.set(id, forKey: "userId")
            userId = id
        }
        displayName = defaults.string(forKey: "displayName") ?? "Me"
    }

    var me: Challenge.Participant { Challenge.Participant(id: userId, name: displayName, completed: 0) }

    func refresh(sessions: [Session]) async {
        do {
            challenges = try await service.list(for: userId)
            await reportProgress(sessions: sessions)
        } catch {
            self.error = error.localizedDescription
        }
    }

    func create(title: String, target: Int, days: Int, sessions: [Session]) async {
        var withProgress = Challenge.make(title: title.isEmpty ? "\(target) workouts in \(days) days" : title,
                                          target: target, days: days, owner: me)
        withProgress.participants[0].completed = Challenge.progress(of: sessions, in: withProgress)
        do {
            try await service.create(withProgress)
            challenges.insert(withProgress, at: 0)
        } catch {
            self.error = error.localizedDescription
        }
    }

    func join(code raw: String, sessions: [Session]) async -> Bool {
        guard let code = InviteCode.normalize(raw) else {
            error = "Invite codes are 6 letters and numbers."
            return false
        }
        do {
            let c = try await service.join(code: code, as: me)
            if !challenges.contains(where: { $0.id == c.id }) { challenges.insert(c, at: 0) }
            await reportProgress(sessions: sessions)
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    /// Updates this user's count in every challenge whose window includes recent workouts.
    func reportProgress(sessions: [Session]) async {
        for (i, c) in challenges.enumerated() {
            guard let mine = c.participants.firstIndex(where: { $0.id == userId }) else { continue }
            var updated = c.participants[mine]
            updated.completed = Challenge.progress(of: sessions, in: c)
            updated.name = displayName
            guard updated != c.participants[mine] else { continue }
            challenges[i].participants[mine] = updated
            try? await service.report(challengeId: c.id, participant: updated)
        }
    }
}
