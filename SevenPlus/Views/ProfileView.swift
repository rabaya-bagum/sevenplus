import SwiftUI

/// Edit goal, level, default length and limitations after onboarding.
struct ProfileView: View {
    @EnvironmentObject var store: SessionStore
    @EnvironmentObject var premium: PremiumStore
    @EnvironmentObject var reminders: ReminderScheduler
    @EnvironmentObject var health: HealthManager

    private var reminderTime: Binding<Date> {
        Binding(
            get: { Calendar.current.date(from: DateComponents(hour: reminders.settings.hour,
                                                               minute: reminders.settings.minute)) ?? Date() },
            set: { d in
                let c = Calendar.current.dateComponents([.hour, .minute], from: d)
                reminders.settings.hour = c.hour ?? 18
                reminders.settings.minute = c.minute ?? 0
            })
    }

    private var p: Binding<Profile> {
        Binding(get: { store.profile ?? Profile() }, set: { store.profile = $0 })
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Training") {
                    Picker("Goal", selection: p.goal) { ForEach(Goal.allCases) { Text($0.title).tag($0) } }
                    Picker("Level", selection: p.level) { ForEach(Level.allCases) { Text($0.rawValue).tag($0) } }
                    Picker("Default length", selection: p.minutes) {
                        ForEach(WorkoutBuilder.lengths, id: \.self) { Text("\($0) min").tag($0) }
                    }
                }
                Section {
                    ForEach(Limitation.allCases) { lim in
                        Toggle(lim.rawValue, isOn: Binding(
                            get: { p.wrappedValue.limitations.contains(lim) },
                            set: { on in
                                var v = p.wrappedValue
                                if on { v.limitations.insert(lim) } else { v.limitations.remove(lim) }
                                p.wrappedValue = v
                            }))
                    }
                } header: {
                    Text("Go easy on")
                } footer: {
                    Text("Daily workouts leave out moves that load these areas.")
                }
                Section {
                    Toggle("Workout reminders", isOn: Binding(
                        get: { reminders.settings.enabled },
                        set: { on in Task { await reminders.setEnabled(on) } }))
                    if reminders.settings.enabled {
                        DatePicker("Time", selection: reminderTime, displayedComponents: .hourAndMinute)
                        HStack {
                            ForEach(1...7, id: \.self) { day in
                                let on = reminders.settings.weekdays.contains(day)
                                Button {
                                    if on { reminders.settings.weekdays.remove(day) }
                                    else { reminders.settings.weekdays.insert(day) }
                                } label: {
                                    Text(Calendar.current.veryShortWeekdaySymbols[day - 1])
                                        .frame(maxWidth: .infinity, minHeight: 32)
                                        .background(on ? Color.accentColor : Color.secondary.opacity(0.15),
                                                    in: Circle())
                                        .foregroundStyle(on ? .white : .primary)
                                }
                                .buttonStyle(.plain)
                                .accessibilityLabel(Calendar.current.weekdaySymbols[day - 1])
                                .accessibilityAddTraits(on ? .isSelected : [])
                            }
                        }
                    }
                } header: {
                    Text("Reminders")
                } footer: {
                    if reminders.permissionDenied {
                        Text("Notifications are off for SevenPlus. Turn them on in Settings → Notifications.")
                    }
                }
                if health.isAvailable {
                    Section {
                        Toggle("Save workouts to Apple Health", isOn: Binding(
                            get: { health.enabled },
                            set: { on in Task { await health.setEnabled(on) } }))
                    } footer: {
                        Text(health.lastError ?? "Calories are estimated from your weight in Health, or 70 kg if it isn't shared.")
                    }
                }
                Section("Premium") {
                    if premium.isPremium {
                        Label("Premium active", systemImage: "checkmark.seal.fill").foregroundStyle(.green)
                    } else {
                        Button("See Premium") { premium.showPaywall = true }
                    }
                    Button("Restore purchases") { Task { await premium.restore() } }
                }
                Section {
                    NavigationLink { YourDataView() } label: {
                        Label("Your data", systemImage: "lock.shield")
                    }
                } footer: {
                    Text("Export or erase everything SevenPlus stores.")
                }
                #if DEBUG
                Section("Developer") {
                    Toggle("Unlock Premium (debug only)", isOn: $premium.debugUnlock)
                }
                #endif
            }
            .navigationTitle("Profile")
        }
    }
}
