import XCTest
@testable import SevenPlus

final class ReminderTests: XCTestCase {
    func testTriggersOnePerWeekdayWhenEnabled() {
        var s = ReminderSettings(enabled: true, hour: 7, minute: 30, weekdays: [6, 2, 4])
        XCTAssertEqual(s.triggerComponents.map(\.weekday), [2, 4, 6])
        XCTAssertTrue(s.triggerComponents.allSatisfy { $0.hour == 7 && $0.minute == 30 })
        s.enabled = false
        XCTAssertTrue(s.triggerComponents.isEmpty)
    }

    func testSaveAndLoad() {
        let d = UserDefaults(suiteName: "reminders-\(UUID())")!
        XCTAssertEqual(ReminderSettings.load(d), ReminderSettings(), "defaults when nothing saved")
        let s = ReminderSettings(enabled: true, hour: 6, minute: 15, weekdays: [1, 7])
        s.save(d)
        XCTAssertEqual(ReminderSettings.load(d), s)
    }
}
