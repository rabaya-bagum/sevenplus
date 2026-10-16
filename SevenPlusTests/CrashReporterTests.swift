import XCTest
@testable import SevenPlus

@MainActor
final class CrashReporterTests: XCTestCase {
    func testDSNParsing() {
        XCTAssertEqual(CrashReporter.dsn(from: "https://abc@o1.ingest.sentry.io/123"), "https://abc@o1.ingest.sentry.io/123")
        XCTAssertEqual(CrashReporter.dsn(from: "  https://abc@o1.ingest.sentry.io/123 "), "https://abc@o1.ingest.sentry.io/123")
        XCTAssertNil(CrashReporter.dsn(from: nil))
        XCTAssertNil(CrashReporter.dsn(from: ""))
        XCTAssertNil(CrashReporter.dsn(from: "$(SENTRY_DSN)"), "unexpanded build setting")
        XCTAssertNil(CrashReporter.dsn(from: "abc"))
        XCTAssertNil(CrashReporter.dsn(from: 42))
    }

    func testNeedsBothOptInAndDSN() {
        XCTAssertFalse(CrashReporter.shouldRun(enabled: false, dsn: nil))
        XCTAssertFalse(CrashReporter.shouldRun(enabled: true, dsn: nil))
        XCTAssertFalse(CrashReporter.shouldRun(enabled: false, dsn: "https://x"))
        XCTAssertTrue(CrashReporter.shouldRun(enabled: true, dsn: "https://x"))
    }

    func testOffByDefaultRemembersChoiceAndResets() {
        let d = UserDefaults(suiteName: "crash-\(UUID())")!
        // The test bundle has no SentryDSN, so nothing ever starts here.
        let reporter = CrashReporter(bundle: Bundle(for: Self.self), defaults: d)
        XCTAssertFalse(reporter.enabled, "opt-in: off until the user turns it on")
        XCTAssertFalse(reporter.isConfigured)

        reporter.enabled = true
        XCTAssertTrue(CrashReporter(bundle: Bundle(for: Self.self), defaults: d).enabled)

        reporter.resetLocal()
        XCTAssertFalse(d.bool(forKey: CrashReporter.enabledKey))
    }

    func testEraseRemovesTheChoice() {
        let d = UserDefaults(suiteName: "crash-keys-\(UUID())")!
        d.set(true, forKey: CrashReporter.enabledKey)
        StoredKeys.removeAll(from: d)
        XCTAssertNil(d.object(forKey: CrashReporter.enabledKey))
    }
}
