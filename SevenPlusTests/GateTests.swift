import XCTest
@testable import SevenPlus

final class GateTests: XCTestCase {
    func testLengths() {
        XCTAssertTrue(Gate.canUse(length: 5, premium: false))
        XCTAssertTrue(Gate.canUse(length: 7, premium: false))
        XCTAssertFalse(Gate.canUse(length: 10, premium: false))
        XCTAssertTrue(Gate.canUse(length: 20, premium: true))
    }

    func testCustomWorkoutLimit() {
        XCTAssertTrue(Gate.canCreateCustom(existing: 2, premium: false))
        XCTAssertFalse(Gate.canCreateCustom(existing: 3, premium: false))
        XCTAssertTrue(Gate.canCreateCustom(existing: 50, premium: true))
    }

    func testUpsellOnlyOnceAfterThirdWorkoutAndNeverForPremium() {
        XCTAssertFalse(Gate.shouldUpsell(completedCount: 1, premium: false, alreadyShown: false), "never before the first")
        XCTAssertFalse(Gate.shouldUpsell(completedCount: 2, premium: false, alreadyShown: false))
        XCTAssertTrue(Gate.shouldUpsell(completedCount: 3, premium: false, alreadyShown: false))
        XCTAssertFalse(Gate.shouldUpsell(completedCount: 3, premium: false, alreadyShown: true))
        XCTAssertFalse(Gate.shouldUpsell(completedCount: 3, premium: true, alreadyShown: false))
        XCTAssertFalse(Gate.shouldUpsell(completedCount: 4, premium: false, alreadyShown: false))
    }
}
