import SwiftUI

/// Ready-made circuits plus the full exercise library, searchable and grouped by body area.
struct WorkoutsView: View {
    @EnvironmentObject var store: SessionStore
    @State private var search = ""
    @State private var playing: Workout?

    private var limits: Set<Limitation> { store.profile?.limitations ?? [] }

    private var classic: Workout {
        limits.contains(.noJumping) ? ExerciseLibrary.quiet(ExerciseLibrary.classic) : ExerciseLibrary.classic
    }

    private func exercises(in area: BodyArea) -> [Exercise] {
        ExerciseLibrary.browsable
            .filter { ExerciseTraits.of($0.id).area == area }
            .filter { search.isEmpty || $0.name.localizedCaseInsensitiveContains(search) }
    }

    var body: some View {
        NavigationStack {
            List {
                if search.isEmpty {
                    Section("Circuits") {
                        Button { playing = classic } label: {
                            VStack(alignment: .leading, spacing: 4) {
                                Label(classic.name, systemImage: "bolt.heart").font(.headline)
                                Text("12 exercises · 7 min · free forever").font(.subheadline).foregroundStyle(.secondary)
                            }
                        }
                    }
                }
                ForEach(BodyArea.allCases) { area in
                    let list = exercises(in: area)
                    if !list.isEmpty {
                        Section(area.title) {
                            ForEach(list) { ex in
                                NavigationLink(value: ex) { ExerciseRow(exercise: ex, limits: limits) }
                            }
                        }
                    }
                }
            }
            .navigationTitle("Workouts")
            .searchable(text: $search, prompt: "Search exercises")
            .navigationDestination(for: Exercise.self) { ExerciseDetailView(exercise: $0) }
            .overlay {
                if !search.isEmpty && BodyArea.allCases.allSatisfy({ exercises(in: $0).isEmpty }) {
                    ContentUnavailableView.search(text: search)
                }
            }
            .fullScreenCover(item: $playing) { w in
                PlayerView(workout: w) { playing = nil }
            }
        }
    }
}

struct ExerciseRow: View {
    let exercise: Exercise
    let limits: Set<Limitation>

    private var conflicts: Set<Limitation> { ExerciseTraits.of(exercise.id).stresses.intersection(limits) }

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: exercise.symbol).font(.title2).frame(width: 36)
                .foregroundStyle(.tint).accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 2) {
                Text(exercise.name)
                if exercise.impact == .high {
                    Text("High impact").font(.caption).foregroundStyle(.secondary)
                }
            }
            Spacer()
            if !conflicts.isEmpty {
                Image(systemName: "exclamationmark.triangle.fill").foregroundStyle(.yellow)
                    .accessibilityLabel("Not in your daily workouts")
            }
        }
    }
}
