import SwiftUI

struct WatchPlayerView: View {
    @EnvironmentObject var model: WatchModel

    var body: some View {
        TimelineView(.periodic(from: .now, by: 1)) { context in
            let s = model.state
            VStack(spacing: 6) {
                if s.stage == .idle {
                    Image(systemName: "iphone").font(.largeTitle).foregroundStyle(.secondary)
                    Text("Start a workout on your iPhone").multilineTextAlignment(.center).font(.footnote)
                } else {
                    if s.stepCount > 0 && s.stage != .getReady {
                        Text("\(s.stepIndex + 1) of \(s.stepCount)").font(.caption2).foregroundStyle(.secondary)
                    }
                    Text(s.title).font(.headline).lineLimit(2).multilineTextAlignment(.center)
                    Text("\(s.remaining(at: context.date))")
                        .font(.system(size: 44, weight: .bold, design: .rounded))
                        .monospacedDigit()
                        .foregroundStyle(s.stage == .rest ? .green : .orange)
                    if let next = s.next {
                        Text("Next: \(next)").font(.caption2).lineLimit(1)
                    }
                    if let bpm = model.heartRate {
                        Label("\(bpm)", systemImage: "heart.fill").font(.caption).foregroundStyle(.red)
                    }
                    if s.stage != .finished {
                        HStack {
                            Button { model.send(s.isPaused ? .resume : .pause) } label: {
                                Image(systemName: s.isPaused ? "play.fill" : "pause.fill")
                            }
                            .accessibilityLabel(s.isPaused ? "Resume" : "Pause")
                            Button { model.send(.skip) } label: { Image(systemName: "forward.end.fill") }
                                .accessibilityLabel("Skip")
                        }
                    }
                }
            }
        }
    }
}
