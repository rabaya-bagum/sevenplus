import XCTest
@testable import SevenPlus

@MainActor
final class CustomAndSwapTests: XCTestCase {

    func testSwapOptionsSameAreaSafeAndLowImpactFirst() {
        let opts = WorkoutBuilder.swapOptions(for: "jumping-jacks", limits: [.wrists])
        XCTAssertFalse(opts.isEmpty)
        XCTAssertFalse(opts.map(\.id).contains("jumping-jacks"))
        for ex in opts {
            XCTAssertEqual(ExerciseTraits.of(ex.id).area, .cardio)
            XCTAssertFalse(ExerciseTraits.of(ex.id).stresses.contains(.wrists))
        }
        XCTAssertEqual(opts.first?.impact, .low)
    }

    func testSwapDuringWorkKeepsTimingAndRecordsSwap() {
        var clock = Date(timeIntervalSince1970: 0)
        let engine = WorkoutEngine(workout: ExerciseLibrary.classic, now: { clock })
        engine.start()
        clock += 15                                   // 10 s into jumping jacks
        engine.tick()
        XCTAssertEqual(engine.phase, .work(index: 0, remaining: 20))
        engine.swap(to: "step-jacks")
        XCTAssertEqual(engine.workout.steps[0].exerciseId, "step-jacks")
        XCTAssertEqual(engine.phase, .work(index: 0, remaining: 20), "timing unchanged")
        XCTAssertEqual(engine.timeline.totalSeconds, Timeline(workout: ExerciseLibrary.classic).totalSeconds)
        XCTAssertEqual(engine.swaps, ["jumping-jacks": "step-jacks"])

        engine.swap(to: "marching")                   // swapping again keeps the original key
        XCTAssertEqual(engine.swaps, ["jumping-jacks": "marching"])
    }

    func testSwapDuringRestReplacesNextExercise() {
        var clock = Date(timeIntervalSince1970: 0)
        let engine = WorkoutEngine(workout: ExerciseLibrary.classic, now: { clock })
        engine.start()
        clock += 37                                   // resting after exercise 1
        engine.tick()
        XCTAssertEqual(engine.swapTargetIndex, 1)
        engine.swap(to: "glute-bridge")
        XCTAssertEqual(engine.workout.steps[1].exerciseId, "glute-bridge")
        XCTAssertEqual(engine.workout.steps[0].exerciseId, "jumping-jacks")
    }

    func testCustomWorkoutRoundsRoundTrip() {
        let w = CustomWorkoutEditor.makeWorkout(id: "c1", name: "  ", exerciseIds: ["squat", "plank"],
                                                work: 40, rest: 20, rounds: 3)
        XCTAssertEqual(w.name, "My workout")
        XCTAssertEqual(w.steps.count, 6)
        XCTAssertEqual(w.steps.map(\.exerciseId), ["squat", "plank", "squat", "plank", "squat", "plank"])
        XCTAssertEqual(CustomWorkoutEditor.detectRounds(w.steps.map(\.exerciseId)), 3)
        XCTAssertEqual(CustomWorkoutEditor.detectRounds(["squat", "plank", "lunge"]), 1)
        XCTAssertEqual(CustomWorkoutEditor.detectRounds([]), 1)
    }

    func testStoreSavesUpdatesAndDeletesCustomWorkouts() {
        let container = AppSchema.container(inMemory: true)
        let defaults = UserDefaults(suiteName: "custom-\(UUID())")!
        let store = SessionStore(container: container, defaults: defaults, legacyFile: nil)
        let w = CustomWorkoutEditor.makeWorkout(id: "c1", name: "Legs", exerciseIds: ["squat"], work: 30, rest: 10, rounds: 1)
        store.saveCustom(w)
        store.saveCustom(Workout(id: "c1", name: "Legs day", steps: w.steps))   // update, not duplicate

        let relaunched = SessionStore(container: container, defaults: defaults, legacyFile: nil)
        XCTAssertEqual(relaunched.customWorkouts.map(\.name), ["Legs day"])
        relaunched.deleteCustom("c1")
        XCTAssertTrue(SessionStore(container: container, defaults: defaults, legacyFile: nil).customWorkouts.isEmpty)
    }
}
