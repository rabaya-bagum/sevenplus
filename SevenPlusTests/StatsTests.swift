import XCTest
@testable import SevenPlus

final class StatsTests: XCTestCase {
    let cal = Calendar(identifier: .gregorian)
    let today = Date(timeIntervalSince1970: 1_760_000_000)

    private func s(_ daysAgo: Int, seconds: Int = 420, completed: Bool = true) -> Session {
        Session(id: UUID(), workoutId: "w", date: cal.date(byAdding: .day, value: -daysAgo, to: today)!,
                durationSeconds: seconds, completed: completed, skipped: [], difficulty: nil)
    }

    func testDailyMinutesLastSevenDays() {
        let days = Stats.dailyMinutes([s(0), s(0, seconds: 300), s(2), s(9)], today: today, calendar: cal)
        XCTAssertEqual(days.count, 7)
        XCTAssertEqual(days.last?.minutes, 12, "two sessions today: 7 + 5")
        XCTAssertEqual(days[4].minutes, 7, "two days ago")
        XCTAssertEqual(days.map(\.minutes).reduce(0, +), 19, "9 days ago is outside the window")
        XCTAssertEqual(days.first?.date, cal.startOfDay(for: cal.date(byAdding: .day, value: -6, to: today)!))
    }

    func testTotalMinutes() {
        XCTAssertEqual(Stats.totalMinutes([s(0), s(1, seconds: 600)]), 17)
    }

    func testBestStreakIgnoresIncompleteAndFindsLongestRun() {
        let sessions = [s(10), s(9), s(8), s(7),          // run of 4
                        s(3), s(2, completed: false), s(1), s(0)]  // broken by the stopped one
        XCTAssertEqual(Stats.bestStreak(sessions, calendar: cal), 4)
        XCTAssertEqual(Stats.bestStreak([], calendar: cal), 0)
    }

    func testLibraryHidesSwapOnlyMoves() {
        let ids = ExerciseLibrary.browsable.map(\.id)
        XCTAssertEqual(ids.count, 18)
        XCTAssertFalse(ids.contains("step-jacks"))
    }
}
