import SwiftUI

struct ExerciseDetailView: View {
    let exercise: Exercise
    @EnvironmentObject var store: SessionStore

    private var traits: ExerciseTraits { ExerciseTraits.of(exercise.id) }
    private var conflicts: [Limitation] {
        Limitation.allCases.filter { traits.stresses.contains($0) && (store.profile?.limitations.contains($0) ?? false) }
    }
    private var quietSwap: Exercise? { exercise.quietSwapId.map(ExerciseLibrary.exercise) }

    var body: some View {
        List {
            Section {
                VStack(spacing: 16) {
                    ExerciseVisual(exercise: exercise)
                        .frame(maxWidth: .infinity, maxHeight: 320)
                    Text(exercise.cue).font(.title3).multilineTextAlignment(.center)
                }
                .padding(.vertical, 8)
            }

            Section("Details") {
                LabeledContent("Body area", value: traits.area.title)
                LabeledContent("Impact", value: exercise.impact == .high ? "High (jumping)" : "Low")
                LabeledContent("Equipment", value: exercise.id.contains("dip") || exercise.id.contains("step-up") ? "Chair" : "None")
                if !traits.stresses.isEmpty {
                    LabeledContent("Loads", value: Limitation.allCases.filter(traits.stresses.contains).map(\.rawValue).joined(separator: ", "))
                }
            }

            if !conflicts.isEmpty {
                Section {
                    Label("You asked to go easy on \(conflicts.map(\.rawValue).joined(separator: " and ").lowercased()), so this move is left out of your daily workouts.",
                          systemImage: "exclamationmark.triangle.fill")
                        .foregroundStyle(.secondary)
                }
            }

            if let swap = quietSwap {
                Section("Quiet alternative") {
                    NavigationLink(value: swap) { ExerciseRow(exercise: swap, limits: store.profile?.limitations ?? []) }
                }
            }
        }
        .navigationTitle(exercise.name)
        .navigationBarTitleDisplayMode(.inline)
    }
}
