import XCTest
@testable import SevenPlus

final class ChallengeTests: XCTestCase {
    let cal = Calendar(identifier: .gregorian)
    let start = Date(timeIntervalSince1970: 1_760_000_000)

    private func session(_ offsetDays: Double, completed: Bool = true) -> Session {
        Session(id: UUID(), workoutId: "w", date: start.addingTimeInterval(offsetDays * 86_400),
                durationSeconds: 420, completed: completed, skipped: [], difficulty: nil)
    }

    func testWindowAndProgress() {
        let c = Challenge.make(title: "T", target: 5, days: 7,
                               owner: .init(id: "a", name: "A", completed: 0), start: start, calendar: cal)
        XCTAssertEqual(c.end, cal.date(byAdding: .day, value: 7, to: cal.startOfDay(for: start)))
        let sessions = [session(0.1), session(1), session(2, completed: false), session(-3), session(8)]
        XCTAssertEqual(Challenge.progress(of: sessions, in: c), 2)
        XCTAssertTrue(c.isActive(at: start.addingTimeInterval(3600)))
        XCTAssertFalse(c.isActive(at: start.addingTimeInterval(8 * 86_400)))
    }

    func testLeaderboardOrder() {
        var c = Challenge.make(title: "T", target: 5, days: 7, owner: .init(id: "a", name: "A", completed: 2))
        c.participants += [.init(id: "b", name: "B", completed: 4), .init(id: "c", name: "C", completed: 2)]
        XCTAssertEqual(c.leaderboard.map(\.id), ["b", "a", "c"], "ties keep join order")
    }

    func testInviteCodes() {
        var rng = SystemRandomNumberGenerator()
        for _ in 0..<50 {
            let code = InviteCode.generate(using: &rng)
            XCTAssertEqual(InviteCode.normalize(code), code)
            XCTAssertFalse(code.contains("O") || code.contains("0") || code.contains("I") || code.contains("1"))
        }
        XCTAssertEqual(InviteCode.normalize(" ab2-cd3 "), "AB2CD3", "spaces and dashes ignored")
        XCTAssertNil(InviteCode.normalize("ab2-cd"), "too short")
        XCTAssertEqual(InviteCode.normalize("abc-def"), "ABCDEF")
        XCTAssertNil(InviteCode.normalize("ABCDE0"), "0 is not in the alphabet")
    }

    func testLocalServiceCreateListReport() async throws {
        let service = LocalChallengeService(defaults: UserDefaults(suiteName: "ch-\(UUID())")!)
        var me = Challenge.Participant(id: "me", name: "Me", completed: 0)
        let c = Challenge.make(title: "T", target: 3, days: 7, owner: me)
        try await service.create(c)
        let mine = try await service.list(for: "me")
        XCTAssertEqual(mine.map(\.id), [c.id])
        let others = try await service.list(for: "someone-else")
        XCTAssertTrue(others.isEmpty)
        me.completed = 2
        try await service.report(challengeId: c.id, participant: me)
        let updated = try await service.list(for: "me")
        XCTAssertEqual(updated.first?.participants.first?.completed, 2)
    }
}
