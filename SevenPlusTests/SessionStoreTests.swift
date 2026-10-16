import XCTest
import SwiftData
@testable import SevenPlus

@MainActor
final class SessionStoreTests: XCTestCase {
    private var defaults: UserDefaults!
    private var suiteName: String!
    private var legacyFile: URL!

    override func setUp() async throws {
        suiteName = "SessionStoreTests-\(UUID())"
        defaults = UserDefaults(suiteName: suiteName)
        legacyFile = FileManager.default.temporaryDirectory.appendingPathComponent("legacy-\(UUID()).json")
    }

    override func tearDown() async throws {
        defaults.removePersistentDomain(forName: suiteName)
        try? FileManager.default.removeItem(at: legacyFile)
    }

    private func makeStore(_ container: ModelContainer) -> SessionStore {
        SessionStore(container: container, defaults: defaults, legacyFile: legacyFile)
    }

    private func session(daysAgo: Int = 0, completed: Bool = true) -> Session {
        Session(id: UUID(), workoutId: "classic-7",
                date: Date().addingTimeInterval(TimeInterval(-86_400 * daysAgo)),
                durationSeconds: 475, completed: completed, skipped: [], difficulty: nil)
    }

    func testSessionsAndRatingSurviveRelaunch() {
        let container = AppSchema.container(inMemory: true)
        let store = makeStore(container)
        let older = session(daysAgo: 1), newer = session()
        store.add(older)
        store.add(newer)
        store.rate(newer.id, .tooHard)

        let relaunched = makeStore(container)
        XCTAssertEqual(relaunched.sessions.map(\.id), [newer.id, older.id], "newest first")
        XCTAssertEqual(relaunched.sessions.first?.difficulty, .tooHard)
        XCTAssertEqual(relaunched.streak, 2)
    }

    func testProfileSurvivesRelaunchAndCanBeCleared() {
        let container = AppSchema.container(inMemory: true)
        let store = makeStore(container)
        XCTAssertNil(store.profile)
        store.profile = Profile(goal: .getStrong, level: .advanced, minutes: 15, limitations: [.knees])

        XCTAssertEqual(makeStore(container).profile?.limitations, [.knees])
        XCTAssertEqual(makeStore(container).profile?.minutes, 15)

        store.profile = nil
        XCTAssertNil(makeStore(container).profile)
    }

    func testImportsPhase0DataOnceAndRemovesIt() throws {
        let old = [session(daysAgo: 2), session(daysAgo: 1)]
        try JSONEncoder().encode(old).write(to: legacyFile)
        defaults.set(try JSONEncoder().encode(Profile(level: .intermediate)), forKey: "profile")

        let container = AppSchema.container(inMemory: true)
        let store = makeStore(container)
        XCTAssertEqual(Set(store.sessions.map(\.id)), Set(old.map(\.id)))
        XCTAssertEqual(store.profile?.level, .intermediate)
        XCTAssertFalse(FileManager.default.fileExists(atPath: legacyFile.path), "old file removed")
        XCTAssertNil(defaults.data(forKey: "profile"), "old profile removed")

        // A second launch must not duplicate anything.
        XCTAssertEqual(makeStore(container).sessions.count, 2)
    }
}
