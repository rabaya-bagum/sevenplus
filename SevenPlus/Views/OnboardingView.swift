import SwiftUI

/// Four quick steps: goal, level, time per day, limitations. Under a minute.
struct OnboardingView: View {
    let onFinish: (Profile) -> Void
    @State private var draft = Profile()
    @State private var step = 0

    private let titles = ["What's your goal?", "How fit are you now?", "How long per day?", "Anything to go easy on?"]

    var body: some View {
        VStack(alignment: .leading, spacing: 24) {
            ProgressView(value: Double(step + 1), total: 4)
            Text(titles[step]).font(.largeTitle.bold())

            Group {
                switch step {
                case 0: choices(Goal.allCases, selected: draft.goal, title: \.title) { draft.goal = $0 }
                case 1: choices(Level.allCases, selected: draft.level, title: \.rawValue) { draft.level = $0 }
                case 2: lengthPicker
                default: limitationPicker
                }
            }

            Spacer()

            HStack {
                if step > 0 {
                    Button("Back") { step -= 1 }
                }
                Spacer()
                Button(step == 3 ? "Start training" : "Next") {
                    if step == 3 { onFinish(draft) } else { step += 1 }
                }
                .buttonStyle(.borderedProminent)
                .font(.headline)
            }
        }
        .padding(24)
        .animation(.default, value: step)
    }

    private func choices<T: Identifiable & Equatable>(
        _ items: [T], selected: T, title: KeyPath<T, String>, pick: @escaping (T) -> Void
    ) -> some View {
        VStack(spacing: 12) {
            ForEach(items) { item in
                Button { pick(item) } label: {
                    HStack {
                        Text(item[keyPath: title]).font(.title3)
                        Spacer()
                        if item == selected { Image(systemName: "checkmark.circle.fill") }
                    }
                    .padding()
                    .frame(maxWidth: .infinity)
                    .background(item == selected ? Color.accentColor.opacity(0.15) : Color.secondary.opacity(0.08),
                                in: RoundedRectangle(cornerRadius: 12))
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(item == selected ? .isSelected : [])
            }
        }
    }

    private var lengthPicker: some View {
        VStack(alignment: .leading, spacing: 12) {
            Picker("Minutes", selection: $draft.minutes) {
                ForEach(WorkoutBuilder.lengths, id: \.self) { Text("\($0) min").tag($0) }
            }
            .pickerStyle(.segmented)
            Text("You can change this any day.").foregroundStyle(.secondary)
        }
    }

    private var limitationPicker: some View {
        VStack(alignment: .leading, spacing: 12) {
            ForEach(Limitation.allCases) { lim in
                Toggle(lim.rawValue, isOn: Binding(
                    get: { draft.limitations.contains(lim) },
                    set: { on in if on { draft.limitations.insert(lim) } else { draft.limitations.remove(lim) } }
                ))
            }
            Text("We'll leave out moves that load these areas.").foregroundStyle(.secondary)
        }
    }
}
