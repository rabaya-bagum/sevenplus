import SwiftUI

struct SummaryView: View {
    let session: Session
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
                stat("\(12 - session.skipped.count)/12", "Exercises")
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

            Spacer()
            Button(action: onDone) {
                Text("Done").font(.headline).frame(maxWidth: .infinity).padding(.vertical, 6)
            }
            .buttonStyle(.borderedProminent)
            .padding(.horizontal)
        }
        .padding()
    }

    private func stat(_ value: String, _ label: String) -> some View {
        VStack {
            Text(value).font(.title.bold()).monospacedDigit()
            Text(label).font(.caption).foregroundStyle(.secondary)
        }
        .accessibilityElement(children: .combine)
    }
}
