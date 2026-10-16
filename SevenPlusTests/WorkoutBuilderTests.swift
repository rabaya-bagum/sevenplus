import XCTest
@testable import SevenPlus

final class WorkoutBuilderTests: XCTestCase {

    func testEveryExerciseHasTraits() {
        for ex in ExerciseLibrary.all {
            XCTAssertNotNil(ExerciseTraits.byId[ex.id], "\(ex.id) missing traits")
        }
    }

    func testExerciseCountMatchesLength() {
        // beginner 30/15: 7 min → (420 + 15) / 45 = 9
        let p = Profile(goal: .getFit, level: .beginner, minutes: 7, limitations: [])
        XCTAssertEqual(WorkoutBuilder.build(profile: p).steps.count, 9)
        // intermediate 30/10: 7 min → 10; advanced 40/10: 20 min → 24
        XCTAssertEqual(WorkoutBuilder.build(profile: Profile(level: .intermediate, minutes: 7)).steps.count, 10)
        XCTAssertEqual(WorkoutBuilder.build(profile: Profile(level: .advanced, minutes: 20)).steps.count, 24)
    }

    func testActualDurationIsCloseToRequested() {
        for level in Level.allCases {
            for mins in WorkoutBuilder.lengths {
                let w = WorkoutBuilder.build(profile: Profile(level: level, minutes: mins))
                let seconds = Timeline(workout: w, getReadySeconds: 0).totalSeconds
                XCTAssertLessThanOrEqual(abs(seconds - mins * 60), 50, "\(level) \(mins)m took \(seconds)s")
            }
        }
    }

    func testRespectsEveryLimitation() {
        for lim in Limitation.allCases {
            let w = WorkoutBuilder.build(profile: Profile(minutes: 20, limitations: [lim]))
            for step in w.steps {
                XCTAssertFalse(ExerciseTraits.of(step.exerciseId).stresses.contains(lim),
                               "\(step.exerciseId) used despite \(lim)")
            }
        }
    }

    func testAllLimitationsStillGivesAWorkout() {
        let w = WorkoutBuilder.build(profile: Profile(minutes: 10, limitations: Set(Limitation.allCases)))
        XCTAssertFalse(w.steps.isEmpty)
        for step in w.steps {
            XCTAssertTrue(ExerciseTraits.of(step.exerciseId).stresses.isEmpty)
        }
    }

    func testAlternatesBodyAreas() {
        let w = WorkoutBuilder.build(profile: Profile(minutes: 10))
        let areas = w.steps.map { ExerciseTraits.of($0.exerciseId).area }
        for (a, b) in zip(areas, areas.dropFirst()) { XCTAssertNotEqual(a, b) }
    }

    func testSameDaySameWorkoutAndDaysVary() {
        let p = Profile(minutes: 7)
        XCTAssertEqual(WorkoutBuilder.build(profile: p, dayIndex: 3), WorkoutBuilder.build(profile: p, dayIndex: 3))
        XCTAssertNotEqual(WorkoutBuilder.build(profile: p, dayIndex: 3).steps,
                          WorkoutBuilder.build(profile: p, dayIndex: 4).steps)
    }
}
