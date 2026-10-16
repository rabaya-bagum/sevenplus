import XCTest
@testable import SevenPlus

final class CaloriesTests: XCTestCase {
    func testEstimate() {
        // 8 MET × 70 kg × 0.1 h = 56 kcal
        XCTAssertEqual(Calories.estimate(activeSeconds: 360, weightKg: nil), 56)
        XCTAssertEqual(Calories.estimate(activeSeconds: 360, weightKg: 90), 72)
        XCTAssertEqual(Calories.estimate(activeSeconds: 0, weightKg: 80), 0)
    }

    func testActiveSecondsExcludesRestAndGetReady() {
        let w = ExerciseLibrary.classic                               // 12 × (30 work + 10 rest), 5 s ready
        XCTAssertEqual(Calories.activeSeconds(workout: w, elapsed: 3), 0, "still getting ready")
        XCTAssertEqual(Calories.activeSeconds(workout: w, elapsed: 20), 15)
        XCTAssertEqual(Calories.activeSeconds(workout: w, elapsed: 40), 30, "first rest not counted")
        XCTAssertEqual(Calories.activeSeconds(workout: w, elapsed: 50), 35)
        XCTAssertEqual(Calories.activeSeconds(workout: w, elapsed: 475), 360, "whole workout = 12 × 30")
        XCTAssertEqual(Calories.activeSeconds(workout: w, elapsed: 10_000), 360)
    }

    func testOldSessionsWithoutCaloriesStillDecode() throws {
        let json = #"[{"id":"\#(UUID())","workoutId":"w","date":0,"durationSeconds":60,"completed":true,"skipped":[]}]"#
        let sessions = try JSONDecoder().decode([Session].self, from: Data(json.utf8))
        XCTAssertNil(sessions.first?.calories)
    }
}
