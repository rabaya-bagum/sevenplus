import Foundation
#if canImport(Sentry)
import Sentry
#endif

/// Opt-in crash reporting through Sentry.
///
/// Two switches must both be on before anything is sent:
/// 1. The build has a DSN (`SentryDSN` in Info.plist). Without it the toggle is hidden.
/// 2. The user turned on "Share crash reports" (off by default).
///
/// Reports contain the crash stack, device model and iOS version. No names,
/// workout history, health data or IP address are attached.
@MainActor
final class CrashReporter: ObservableObject {
    static let enabledKey = "crashReportsEnabled"

    @Published var enabled: Bool {
        didSet {
            defaults.set(enabled, forKey: Self.enabledKey)
            apply()
        }
    }

    let dsn: String?
    private let defaults: UserDefaults

    init(bundle: Bundle = .main, defaults: UserDefaults = .standard) {
        self.defaults = defaults
        dsn = Self.dsn(from: bundle.object(forInfoDictionaryKey: "SentryDSN"))
        enabled = defaults.bool(forKey: Self.enabledKey)
        apply()
    }

    /// Whether this build can send reports at all (DSN present and SDK linked).
    var isConfigured: Bool {
        #if canImport(Sentry)
        return dsn != nil
        #else
        return false
        #endif
    }

    /// A usable DSN, or nil for missing, empty or unexpanded `$(…)` values.
    nonisolated static func dsn(from value: Any?) -> String? {
        guard let s = (value as? String)?.trimmingCharacters(in: .whitespaces),
              !s.isEmpty, !s.contains("$("), s.hasPrefix("https://") else { return nil }
        return s
    }

    nonisolated static func shouldRun(enabled: Bool, dsn: String?) -> Bool { enabled && dsn != nil }

    /// Turned off by "Erase all my data".
    func resetLocal() { enabled = false }

    private func apply() {
        #if canImport(Sentry)
        let run = Self.shouldRun(enabled: enabled, dsn: dsn)
        if run && !SentrySDK.isEnabled, let dsn {
            SentrySDK.start { options in
                options.dsn = dsn
                options.sendDefaultPii = false           // no IP, user or device name
                options.enableNetworkBreadcrumbs = false // challenge URLs contain the user's random id
                #if DEBUG
                options.environment = "debug"
                #else
                options.environment = "production"
                #endif
            }
        } else if !run && SentrySDK.isEnabled {
            SentrySDK.close()
        }
        #endif
    }
}
