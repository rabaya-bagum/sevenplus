import SwiftUI

struct ChallengesView: View {
    @EnvironmentObject var store: SessionStore
    @EnvironmentObject var challenges: ChallengeStore
    @State private var creating = false
    @State private var joinCode = ""

    var body: some View {
        List {
            Section {
                TextField("Your name on leaderboards", text: $challenges.displayName)
            } footer: {
                if !challenges.service.isOnline {
                    Text("Offline mode: challenges stay on this phone. Connect the online service to race friends (docs/BACKEND.md).")
                }
            }

            if challenges.service.isOnline {
                Section("Join a friend") {
                    HStack {
                        TextField("Invite code", text: $joinCode)
                            .textInputAutocapitalization(.characters)
                            .autocorrectionDisabled()
                        Button("Join") {
                            Task {
                                if await challenges.join(code: joinCode, sessions: store.sessions) { joinCode = "" }
                            }
                        }
                        .disabled(joinCode.count < 6)
                    }
                }
            }

            Section("Your challenges") {
                if challenges.challenges.isEmpty {
                    Text("No challenges yet.").foregroundStyle(.secondary)
                }
                ForEach(challenges.challenges) { c in
                    NavigationLink { ChallengeDetailView(challenge: c) } label: { ChallengeRow(challenge: c, me: challenges.userId) }
                }
                Button { creating = true } label: { Label("New challenge", systemImage: "plus.circle") }
            }

            if let error = challenges.error {
                Section { Text(error).foregroundStyle(.red).font(.footnote) }
            }
        }
        .navigationTitle("Challenges")
        .task { await challenges.refresh(sessions: store.sessions) }
        .refreshable { await challenges.refresh(sessions: store.sessions) }
        .sheet(isPresented: $creating) { NewChallengeView() }
    }
}

struct ChallengeRow: View {
    let challenge: Challenge
    let me: String

    var body: some View {
        let mine = challenge.participants.first { $0.id == me }?.completed ?? 0
        VStack(alignment: .leading, spacing: 6) {
            Text(challenge.title).font(.headline)
            ProgressView(value: Double(min(mine, challenge.target)), total: Double(challenge.target))
            Text(challenge.isActive() ? "\(mine)/\(challenge.target) · \(challenge.daysLeft()) days left" : "Ended · \(mine)/\(challenge.target)")
                .font(.caption).foregroundStyle(.secondary)
        }
    }
}

struct ChallengeDetailView: View {
    let challenge: Challenge
    @EnvironmentObject var challenges: ChallengeStore

    private var invite: String {
        "Join my SevenPlus challenge \"\(challenge.title)\": \(challenge.target) workouts by \(challenge.end.formatted(date: .abbreviated, time: .omitted)). Code: \(challenge.code)"
    }

    var body: some View {
        List {
            Section("Leaderboard") {
                ForEach(Array(challenge.leaderboard.enumerated()), id: \.element.id) { rank, p in
                    HStack {
                        Text("\(rank + 1)").font(.headline).frame(width: 24)
                        Text(p.id == challenges.userId ? "\(p.name) (you)" : p.name)
                        Spacer()
                        Text("\(p.completed)/\(challenge.target)").monospacedDigit()
                        if p.completed >= challenge.target {
                            Image(systemName: "checkmark.seal.fill").foregroundStyle(.green)
                                .accessibilityLabel("Completed")
                        }
                    }
                }
            }
            if challenges.service.isOnline {
                Section {
                    LabeledContent("Invite code", value: challenge.code)
                    ShareLink(item: invite) { Label("Invite friends", systemImage: "square.and.arrow.up") }
                }
            }
        }
        .navigationTitle(challenge.title)
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct NewChallengeView: View {
    @EnvironmentObject var store: SessionStore
    @EnvironmentObject var challenges: ChallengeStore
    @Environment(\.dismiss) private var dismiss
    @State private var title = ""
    @State private var target = 10
    @State private var days = 14

    var body: some View {
        NavigationStack {
            Form {
                TextField("Name (optional)", text: $title)
                Stepper("Workouts: \(target)", value: $target, in: 1...60)
                Stepper("Days: \(days)", value: $days, in: 3...60)
                Text("Workouts done today count from the start.").font(.footnote).foregroundStyle(.secondary)
            }
            .navigationTitle("New challenge")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Create") {
                        Task {
                            await challenges.create(title: title, target: target, days: days, sessions: store.sessions)
                            dismiss()
                        }
                    }
                }
            }
        }
    }
}
