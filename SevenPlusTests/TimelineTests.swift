import XCTest
@testable import SevenPlus

final class TimelineTests: XCTestCase {
    let tl = Timeline(workout: ExerciseLibrary.classic)   // 5 s ready, 12 × (30 + 10)

    func testTotalDropsFinalRest() {
        XCTAssertEqual(tl.totalSeconds, 5 + 12 * 40 - 10)   // 475 s
    }

    func testGetReady() {
        XCTAssertEqual(tl.phase(at: 0), .getReady(remaining: 5))
        XCTAssertEqual(tl.phase(at: 4), .getReady(remaining: 1))
    }

    func testFirstWorkAndRest() {
        XCTAssertEqual(tl.phase(at: 5), .work(index: 0, remaining: 30))
        XCTAssertEqual(tl.phase(at: 34), .work(index: 0, remaining: 1))
        XCTAssertEqual(tl.phase(at: 35), .rest(index: 0, remaining: 10))
        XCTAssertEqual(tl.phase(at: 45), .work(index: 1, remaining: 30))
    }

    func testLastExerciseThenFinished() {
        XCTAssertEqual(tl.phase(at: tl.startOfWork(11)), .work(index: 11, remaining: 30))
        XCTAssertEqual(tl.phase(at: tl.totalSeconds), .finished)
        XCTAssertEqual(tl.phase(at: 10_000), .finished)
    }

    func testQuietModeSwapsHighImpact() {
        let ids = ExerciseLibrary.quiet(ExerciseLibrary.classic).steps.map(\.exerciseId)
        XCTAssertFalse(ids.contains("jumping-jacks"))
        XCTAssertFalse(ids.contains("high-knees"))
        XCTAssertEqual(ids.count, 12)
    }

    func testLibraryIntegrity() {
        let ids = ExerciseLibrary.all.map(\.id)
        XCTAssertEqual(Set(ids).count, ids.count, "duplicate exercise id")
        XCTAssertGreaterThanOrEqual(ids.count, 20)
        for ex in ExerciseLibrary.all where ex.impact == .high {
            let swap = try? XCTUnwrap(ex.quietSwapId, "\(ex.id) has no quiet swap")
            XCTAssertEqual(ExerciseLibrary.all.first { $0.id == swap }?.impact, .low, "\(ex.id) swap missing or not low-impact")
        }
    }

    @MainActor
    func testEngineSurvivesBackgroundingAndSkip() {
        var clock = Date(timeIntervalSince1970: 0)
        let engine = WorkoutEngine(workout: ExerciseLibrary.classic, now: { clock })
        engine.start()
        clock += 50                       // e.g. phone locked for 50 s
        engine.tick()
        XCTAssertEqual(engine.phase, .work(index: 1, remaining: 25))
        engine.skip()
        XCTAssertEqual(engine.phase, .work(index: 2, remaining: 30))
        XCTAssertEqual(engine.skipped, ["wall-sit"])
        engine.pause()
        clock += 600                      // paused time does not count
        engine.tick()
        XCTAssertEqual(engine.phase, .work(index: 2, remaining: 30))
    }

    func testStreak() {
        let cal = Calendar(identifier: .gregorian)
        let today = Date(timeIntervalSince1970: 1_760_000_000)
        let d = { (n: Int) in cal.date(byAdding: .day, value: -n, to: today)! }
        XCTAssertEqual(SessionStore.streak(for: [d(0), d(1), d(2)], today: today, calendar: cal), 3)
        XCTAssertEqual(SessionStore.streak(for: [d(1), d(2)], today: today, calendar: cal), 2)   // today not done yet
        XCTAssertEqual(SessionStore.streak(for: [d(0), d(2)], today: today, calendar: cal), 1)
        XCTAssertEqual(SessionStore.streak(for: [d(3)], today: today, calendar: cal), 0)
    }
}
