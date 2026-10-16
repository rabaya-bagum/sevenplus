import XCTest
import CoreGraphics
@testable import SevenPlus

final class RepCounterTests: XCTestCase {
    func testAngle() {
        XCTAssertEqual(RepProfile.angle(CGPoint(x: 0, y: 1), .zero, CGPoint(x: 1, y: 0)), 90, accuracy: 0.001)
        XCTAssertEqual(RepProfile.angle(CGPoint(x: -1, y: 0), .zero, CGPoint(x: 1, y: 0)), 180, accuracy: 0.001)
        XCTAssertEqual(RepProfile.angle(.zero, .zero, CGPoint(x: 1, y: 0)), 180, "degenerate input is treated as straight")
    }

    /// Feeds a smooth up-down-up angle sequence `reps` times.
    private func feed(_ counter: inout RepCounter, reps: Int, low: Double, high: Double) {
        for _ in 0..<reps {
            for a in stride(from: high, through: low, by: -5) { counter.update(angle: a) }
            for a in stride(from: low, through: high, by: 5) { counter.update(angle: a) }
        }
    }

    func testCountsFullSquats() {
        var c = RepCounter(down: 100, up: 160)
        feed(&c, reps: 5, low: 80, high: 175)
        XCTAssertEqual(c.count, 5)
    }

    func testIgnoresShallowReps() {
        var c = RepCounter(down: 100, up: 160)
        feed(&c, reps: 5, low: 120, high: 175)        // never deep enough
        XCTAssertEqual(c.count, 0)
    }

    func testJitterAroundThresholdDoesNotDoubleCount() {
        var c = RepCounter(down: 100, up: 160)
        for a in [170.0, 140, 110, 90, 80, 95, 85, 100, 90, 120, 150, 165, 158, 167, 162, 175, 158, 163] {
            c.update(angle: a)
        }
        XCTAssertEqual(c.count, 1)
    }

    func testReset() {
        var c = RepCounter(down: 100, up: 160)
        feed(&c, reps: 2, low: 80, high: 175)
        c.reset()
        XCTAssertEqual(c.count, 0)
        XCTAssertFalse(c.isDown)
    }

    func testSupportedExercisesExist() {
        for id in RepProfile.byExercise.keys {
            XCTAssertNotNil(ExerciseTraits.byId[id], "\(id) is not in the library")
        }
    }
}
