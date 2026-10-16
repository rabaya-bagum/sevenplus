import SwiftUI

struct SummaryView: View {
    let session: Session
    let totalExercises: Int
    let onDone: () -> Void
    @EnvironmentObject var store: SessionStore
    @State private var rating: Difficulty?

    var body: some View {
        VStack(spacing: 28) {
            Spacer()
            Image(systemName: "checkmark.seal.fill").font(.system(size: 80)).foregroundStyle(.green)
                .accessibilityHidden(true)
            Text("Workout complete").font(.largeTitle.bold())

            HStack(spacing: 32) {
                stat("\(session.durationSeconds / 60):\(String(format: "%02d", session.durationSeconds % 60))", "Time")
                stat("\(store.streak)", "Day streak")
                stat("\(totalExercises - session.skipped.count)/\(totalExercises)", "Exercises")
                if let kcal = session.calories {
                    stat("\(kcal)", "kcal (est.)")
                }
            }

            VStack(spacing: 12) {
                Text("How did that feel?").font(.headline)
                HStack {
                    ForEach(Difficulty.allCases, id: \.self) { d in
                        Button(d.rawValue) {
                            rating = d
                            store.rate(session.id, d)
                        }
                        .buttonStyle(.bordered)
                        .tint(rating == d ? .accentColor : .gray)
                    }
                }
                Text("Your next workout adapts to this.").font(.footnote).foregroundStyle(.secondary)
            }

            ShareLink(item: shareText) {
                Label("Share", systemImage: "square.and.arrow.up")
            }

            Spacer()
            Button(action: onDone) {
                Text("Done").font(.headline).frame(maxWidth: .infinity).padding(.vertical, 6)
            }
            .buttonStyle(.borderedProminent)
            .padding(.horizontal)
        }
        .padding()
    }

    private var shareText: String {
        let mins = max(1, session.durationSeconds / 60)
        let streak = store.streak > 1 ? " \(store.streak)-day streak!" : ""
        return "Just finished a \(mins)-minute workout with SevenPlus.\(streak)"
    }

    private func stat(_ value: String, _ label: String) -> some View {
        VStack {
            Text(value).font(.title.bold()).monospacedDigit()
            Text(label).font(.caption).foregroundStyle(.secondary)
        }
        .accessibilityElement(children: .combine)
    }
}
