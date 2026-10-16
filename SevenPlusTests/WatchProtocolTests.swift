import XCTest
@testable import SevenPlus

@MainActor
final class WatchProtocolTests: XCTestCase {
    func testCountsDownLocallyUnlessPaused() {
        let sent = Date(timeIntervalSince1970: 100)
        var s = WatchProtocol.PlayerState(stage: .work, title: "Squat", next: nil, remaining: 20,
                                          stepIndex: 2, stepCount: 12, isPaused: false, sentAt: sent)
        XCTAssertEqual(s.remaining(at: sent.addingTimeInterval(5)), 15)
        XCTAssertEqual(s.remaining(at: sent.addingTimeInterval(60)), 0, "never negative")
        s.isPaused = true
        XCTAssertEqual(s.remaining(at: sent.addingTimeInterval(5)), 20)
    }

    func testStateFromEngineAndRoundTrip() throws {
        var clock = Date(timeIntervalSince1970: 0)
        let engine = WorkoutEngine(workout: ExerciseLibrary.classic, now: { clock })
        engine.start()
        clock += 37                                     // resting after exercise 1
        engine.tick()
        let s = WatchProtocol.PlayerState.from(engine)
        XCTAssertEqual(s.stage, .rest)
        XCTAssertEqual(s.next, "Wall Sit")
        XCTAssertEqual(s.remaining, 8)
        XCTAssertEqual(s.stepCount, 12)

        let data = try XCTUnwrap(WatchProtocol.encode(s))
        XCTAssertEqual(WatchProtocol.decode(WatchProtocol.PlayerState.self, data), s)
        XCTAssertNil(WatchProtocol.decode(WatchProtocol.PlayerState.self, "not data"))
    }
}
