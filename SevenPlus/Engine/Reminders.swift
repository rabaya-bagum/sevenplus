import Foundation
import UserNotifications

/// When to remind the user to work out. Stored on this device only.
struct ReminderSettings: Codable, Equatable {
    var enabled = false
    var hour = 18
    var minute = 0
    /// Calendar weekdays, 1 = Sunday … 7 = Saturday.
    var weekdays: Set<Int> = [2, 3, 4, 5, 6]

    static let key = "reminderSettings"

    static func load(_ defaults: UserDefaults = .standard) -> ReminderSettings {
        guard let data = defaults.data(forKey: key),
              let s = try? JSONDecoder().decode(ReminderSettings.self, from: data) else { return ReminderSettings() }
        return s
    }

    func save(_ defaults: UserDefaults = .standard) {
        if let data = try? JSONEncoder().encode(self) { defaults.set(data, forKey: Self.key) }
    }

    /// One repeating trigger per chosen weekday. Empty when disabled.
    var triggerComponents: [DateComponents] {
        guard enabled else { return [] }
        return weekdays.sorted().map { DateComponents(hour: hour, minute: minute, weekday: $0) }
    }
}

/// Schedules local reminder notifications.
@MainActor
final class ReminderScheduler: ObservableObject {
    @Published var settings: ReminderSettings {
        didSet {
            settings.save()
            Task { await apply() }
        }
    }
    @Published private(set) var permissionDenied = false

    private let center = UNUserNotificationCenter.current()
    private static let idPrefix = "sevenplus.reminder."

    static let messages = [
        "Seven minutes. You've got this.",
        "Quick workout? Your streak is waiting.",
        "Time to move. Pick 5 or 7 minutes.",
    ]

    init() { settings = ReminderSettings.load() }

    /// Asks for permission when turning reminders on; turns them back off if refused.
    func setEnabled(_ on: Bool) async {
        guard on else { settings.enabled = false; return }
        let granted = (try? await center.requestAuthorization(options: [.alert, .sound])) ?? false
        permissionDenied = !granted
        settings.enabled = granted
    }

    func apply() async {
        let pending = await center.pendingNotificationRequests()
        center.removePendingNotificationRequests(withIdentifiers:
            pending.map(\.identifier).filter { $0.hasPrefix(Self.idPrefix) })

        for (i, comps) in settings.triggerComponents.enumerated() {
            let content = UNMutableNotificationContent()
            content.title = "SevenPlus"
            content.body = Self.messages[i % Self.messages.count]
            content.sound = .default
            let trigger = UNCalendarNotificationTrigger(dateMatching: comps, repeats: true)
            let id = Self.idPrefix + String(comps.weekday ?? 0)
            try? await center.add(UNNotificationRequest(identifier: id, content: content, trigger: trigger))
        }
    }
}
