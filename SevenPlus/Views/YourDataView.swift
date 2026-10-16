import SwiftUI

/// See what's stored, export it as JSON, or erase all of it.
struct YourDataView: View {
    @EnvironmentObject var store: SessionStore
    @EnvironmentObject var reminders: ReminderScheduler
    @EnvironmentObject var health: HealthManager
    @EnvironmentObject var challenges: ChallengeStore
    @EnvironmentObject var crash: CrashReporter

    @State private var exportURL: URL?
    @State private var exportError: String?
    @State private var confirmErase = false
    @State private var alsoHealth = true
    @State private var erasing = false
    @State private var remoteFailure: String?

    /// Rebuild the export file when the data changes.
    private var exportKey: String {
        "\(store.sessions.count)-\(store.customWorkouts.count)-\(challenges.challenges.count)-\(store.profile.hashValue)"
    }

    var body: some View {
        Form {
            Section {
                LabeledContent("Workouts logged", value: "\(store.sessions.count)")
                LabeledContent("Custom workouts", value: "\(store.customWorkouts.count)")
                LabeledContent("Check-ins", value: "\(ReadinessLog.all().count)")
                LabeledContent("Challenges", value: "\(challenges.challenges.count)")
            } header: {
                Text("Stored on this iPhone")
            } footer: {
                Text(challenges.service.isOnline
                     ? "SevenPlus has no account. Everything stays on this iPhone, except your name and workout count in challenges you share with friends."
                     : "SevenPlus has no account. Everything stays on this iPhone.")
            }

            if crash.isConfigured {
                Section {
                    Toggle("Share crash reports", isOn: $crash.enabled)
                } footer: {
                    Text("Off by default. When SevenPlus crashes, sends the developer where the code failed, your iPhone model and iOS version. Never your name, workouts or health data.")
                }
            }

            Section {
                if let url = exportURL {
                    ShareLink(item: url) { Label("Export my data", systemImage: "square.and.arrow.up") }
                } else if let error = exportError {
                    Text(error).foregroundStyle(.red)
                } else {
                    ProgressView()
                }
            } footer: {
                Text("A readable JSON file with your profile, workout history, custom workouts, check-ins, reminder times and challenges.")
            }

            Section {
                if health.isAvailable {
                    Toggle("Also delete workouts saved to Apple Health", isOn: $alsoHealth)
                }
                Button(role: .destructive) { confirmErase = true } label: {
                    if erasing {
                        HStack { ProgressView(); Text("Erasing…") }
                    } else {
                        Text("Erase all my data")
                    }
                }
                .disabled(erasing)
            } footer: {
                Text("This can't be undone. Your Premium subscription isn't affected; manage it in Settings → your name → Subscriptions. Anything left in Apple Health can be removed in the Health app → Sources → SevenPlus.")
            }
        }
        .toolbar {
            if let privacy = LegalLinks.privacyPolicy {
                Link("Privacy Policy", destination: privacy)
            }
        }
        .navigationTitle("Your data")
        .task(id: exportKey) { makeExport() }
        .confirmationDialog("Erase all SevenPlus data?", isPresented: $confirmErase, titleVisibility: .visible) {
            Button("Erase everything", role: .destructive) { Task { await erase(skipRemote: false) } }
        } message: {
            Text("Your history, profile, custom workouts, check-ins, reminders and challenges will be deleted, and you'll start again from setup.")
        }
        .alert("Couldn't reach the challenge server",
               isPresented: Binding(get: { remoteFailure != nil }, set: { if !$0 { remoteFailure = nil } })) {
            Button("Erase on this iPhone only", role: .destructive) { Task { await erase(skipRemote: true) } }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Nothing was deleted. Try again when you're online. If you erase here only, your name and progress stay on friends' leaderboards.")
        }
    }

    private func makeExport() {
        do {
            exportURL = try ExportFile.current(store: store, reminders: reminders, challenges: challenges)
                .writeTemporaryFile()
            exportError = nil
        } catch {
            exportURL = nil
            exportError = "Couldn't prepare the export: \(error.localizedDescription)"
        }
    }

    private func erase(skipRemote: Bool) async {
        erasing = true
        let outcome = await DataEraser.run(skipRemote: skipRemote, alsoHealth: alsoHealth && health.isAvailable,
                                           store: store, reminders: reminders, health: health, challenges: challenges,
                                           crash: crash)
        erasing = false
        if case .remoteFailed(let message) = outcome { remoteFailure = message }
        if let url = exportURL { try? FileManager.default.removeItem(at: url) }
    }
}
