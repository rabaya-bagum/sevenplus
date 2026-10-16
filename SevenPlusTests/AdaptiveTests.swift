import XCTest
@testable import SevenPlus

final class AdaptiveTests: XCTestCase {

    func testNoRatingsNoChange() {
        XCTAssertEqual(Adaptation.from(ratings: []), Adaptation())
        XCTAssertNil(Adaptation().summary)
    }

    func testTooHardEasesImmediately() {
        let a = Adaptation.from(ratings: [.tooHard])
        XCTAssertEqual(a, Adaptation(workDelta: -5, restDelta: 5))
        XCTAssertEqual(a.apply(to: (30, 10)).work, 25)
        XCTAssertEqual(a.apply(to: (30, 10)).rest, 15)
    }

    func testTooEasyNeedsTwoInARow() {
        XCTAssertEqual(Adaptation.from(ratings: [.tooEasy]), Adaptation())
        XCTAssertEqual(Adaptation.from(ratings: [.tooEasy, .justRight, .tooEasy]), Adaptation(), "run was broken")
        XCTAssertEqual(Adaptation.from(ratings: [.tooEasy, .tooEasy]), Adaptation(workDelta: 5, restDelta: -5))
    }

    func testDeltasAreBounded() {
        let hard = Adaptation.from(ratings: Array(repeating: .tooHard, count: 10))
        XCTAssertEqual(hard.workDelta, Adaptation.workRange.lowerBound)
        XCTAssertEqual(hard.restDelta, Adaptation.restRange.upperBound)
        let easy = Adaptation.from(ratings: Array(repeating: .tooEasy, count: 20))
        XCTAssertEqual(easy.workDelta, Adaptation.workRange.upperBound)
        XCTAssertEqual(easy.restDelta, Adaptation.restRange.lowerBound)
        // Final intervals never leave safe limits.
        XCTAssertGreaterThanOrEqual(easy.apply(to: (30, 10)).rest, 5)
        XCTAssertGreaterThanOrEqual(hard.apply(to: (30, 15)).work, 15)
    }

    func testUsesOnlyLastTenRatedSessionsOldestFirst() {
        let base = Date(timeIntervalSince1970: 1_000_000)
        func s(_ day: Int, _ d: Difficulty?) -> Session {
            Session(id: UUID(), workoutId: "w", date: base.addingTimeInterval(TimeInterval(day * 86_400)),
                    durationSeconds: 400, completed: true, skipped: [], difficulty: d)
        }
        // 10 old "too hard" ratings followed by 10 newer "just right": only the newer ones count.
        let sessions = (0..<10).map { s($0, .tooHard) } + (10..<20).map { s($0, .justRight) } + [s(25, nil)]
        XCTAssertEqual(Adaptation.from(sessions: sessions.shuffled()), Adaptation())
    }

    func testReadinessLength() {
        XCTAssertEqual(Readiness(energy: .low, soreness: .none).adjustedLength(10), 7)
        XCTAssertEqual(Readiness(energy: .low, soreness: .none).adjustedLength(5), 5)
        XCTAssertEqual(Readiness(energy: .low, soreness: .very).adjustedLength(20), 5)
        XCTAssertEqual(Readiness(energy: .high, soreness: .very).adjustedLength(15), 15)
        XCTAssertEqual(Readiness(energy: .ok, soreness: .very).extraRest, 5)
        XCTAssertEqual(Readiness(energy: .ok, soreness: .some).extraRest, 0)
    }

    func testReadinessLogIsPerDay() {
        let d = UserDefaults(suiteName: "readiness-\(UUID())")!
        let cal = Calendar(identifier: .gregorian)
        let day1 = Date(timeIntervalSince1970: 1_760_000_000)
        let day2 = cal.date(byAdding: .day, value: 1, to: day1)!
        XCTAssertNil(ReadinessLog.today(day1, calendar: cal, defaults: d))
        ReadinessLog.record(Readiness(energy: .low, soreness: .some), date: day1, calendar: cal, defaults: d)
        XCTAssertEqual(ReadinessLog.today(day1, calendar: cal, defaults: d)?.energy, .low)
        XCTAssertNil(ReadinessLog.today(day2, calendar: cal, defaults: d), "new day asks again")
    }

    func testBuilderUsesIntervalOverride() {
        let w = WorkoutBuilder.build(profile: Profile(level: .beginner, minutes: 7), intervals: (25, 20))
        XCTAssertTrue(w.steps.allSatisfy { $0.workSeconds == 25 && $0.restSeconds == 20 })
        XCTAssertEqual(w.steps.count, (420 + 20) / 45)
    }
}
