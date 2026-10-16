import XCTest
import SwiftData
@testable import SevenPlus

@MainActor
final class YourDataTests: XCTestCase {
    private let t0 = Date(timeIntervalSince1970: 1_760_000_000)   // whole seconds survive ISO 8601

    private func session(_ days: Int) -> Session {
        Session(id: UUID(), workoutId: "classic-7", date: t0.addingTimeInterval(TimeInterval(days * 86_400)),
                durationSeconds: 475, completed: true, skipped: ["plank"], difficulty: .justRight, calories: 48)
    }

    func testExportRoundTripsEveryField() throws {
        let challenge = Challenge(id: "c1", code: "ABC234", title: "Ten in two weeks", target: 10,
                                  start: t0, end: t0.addingTimeInterval(14 * 86_400),
                                  participants: [.init(id: "me", name: "Naz", completed: 3)])
        let file = ExportFile(exportedAt: t0,
                              profile: Profile(goal: .getStrong, level: .advanced, minutes: 15, limitations: [.knees]),
                              sessions: [session(0), session(1)],
                              customWorkouts: [CustomWorkoutEditor.makeWorkout(id: "w1", name: "Legs", exerciseIds: ["squat", "lunge"],
                                                                               work: 40, rest: 20, rounds: 2)],
                              reminders: ReminderSettings(enabled: true, hour: 7, minute: 30, weekdays: [2, 4]),
                              readiness: ["2026-10-15": Readiness(energy: .low, soreness: .some)],
                              challenges: [challenge], displayName: "Naz")
        let data = try file.encoded()
        XCTAssertEqual(try ExportFile.decoder.decode(ExportFile.self, from: data), file)

        let text = try XCTUnwrap(String(data: data, encoding: .utf8))
        XCTAssertTrue(text.contains("\"version\" : 1"))
        XCTAssertTrue(text.contains("2025-10-09T"), "dates are readable ISO 8601")
    }

    func testExportFileIsWrittenWithDateInName() throws {
        let url = try ExportFile(exportedAt: t0, profile: nil, sessions: [], customWorkouts: [],
                                 reminders: ReminderSettings(), readiness: [:], challenges: [], displayName: "Me")
            .writeTemporaryFile(now: t0)
        defer { try? FileManager.default.removeItem(at: url) }
        XCTAssertTrue(url.lastPathComponent.hasPrefix("SevenPlus-data-2025-10-"))
        XCTAssertTrue(FileManager.default.fileExists(atPath: url.path))
    }

    func testEraseAllClearsDatabaseAndSurvivesRelaunch() {
        let container = AppSchema.container(inMemory: true)
        let defaults = UserDefaults(suiteName: "erase-\(UUID())")!
        let store = SessionStore(container: container, defaults: defaults, legacyFile: nil)
        store.profile = Profile(minutes: 10)
        store.add(session(0))
        store.saveCustom(CustomWorkoutEditor.makeWorkout(id: "w1", name: "A", exerciseIds: ["squat"], work: 30, rest: 10, rounds: 1))

        store.eraseAll()
        XCTAssertNil(store.profile)
        XCTAssertTrue(store.sessions.isEmpty)
        XCTAssertTrue(store.customWorkouts.isEmpty)

        let relaunched = SessionStore(container: container, defaults: defaults, legacyFile: nil)
        XCTAssertNil(relaunched.profile, "onboarding shows again")
        XCTAssertTrue(relaunched.sessions.isEmpty)
        XCTAssertTrue(relaunched.customWorkouts.isEmpty)
    }

    func testStoredKeysRemovesUserDataButNotOtherKeys() {
        let d = UserDefaults(suiteName: "keys-\(UUID())")!
        for key in StoredKeys.all { d.set("x", forKey: key) }
        d.set(true, forKey: "debugUnlock")
        ReadinessLog.record(Readiness(energy: .ok, soreness: .none), defaults: d)
        XCTAssertEqual(ReadinessLog.all(defaults: d).count, 1)

        StoredKeys.removeAll(from: d)
        for key in StoredKeys.all { XCTAssertNil(d.object(forKey: key), key) }
        XCTAssertTrue(ReadinessLog.all(defaults: d).isEmpty)
        XCTAssertTrue(d.bool(forKey: "debugUnlock"), "developer setting is kept")
    }

    func testRemoveParticipantFromLocalChallenges() async throws {
        let service = LocalChallengeService(defaults: UserDefaults(suiteName: "leave-\(UUID())")!)
        var shared = Challenge.make(title: "Shared", target: 5, days: 7, owner: .init(id: "me", name: "Me", completed: 1))
        shared.participants.append(.init(id: "friend", name: "Friend", completed: 2))
        let solo = Challenge.make(title: "Solo", target: 5, days: 7, owner: .init(id: "me", name: "Me", completed: 0))
        try await service.create(shared)
        try await service.create(solo)

        try await service.removeParticipant(userId: "me")
        let mine = try await service.list(for: "me")
        XCTAssertTrue(mine.isEmpty)
        let friends = try await service.list(for: "friend")
        XCTAssertEqual(friends.map(\.title), ["Shared"], "friend keeps the shared challenge; empty one is gone")
        XCTAssertEqual(friends.first?.participants.map(\.id), ["friend"])
    }

    func testChallengeStoreResetGivesNewIdentity() {
        let d = UserDefaults(suiteName: "identity-\(UUID())")!
        let challenges = ChallengeStore(service: LocalChallengeService(defaults: d), defaults: d)
        let old = challenges.userId
        challenges.resetLocal(defaults: d)
        XCTAssertNotEqual(challenges.userId, old)
        XCTAssertEqual(d.string(forKey: "userId"), challenges.userId)
        XCTAssertEqual(challenges.displayName, "Me")
    }
}
