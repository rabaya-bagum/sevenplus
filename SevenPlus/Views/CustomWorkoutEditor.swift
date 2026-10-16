import SwiftUI

/// Build or edit your own circuit: pick exercises, order them, set work/rest and rounds.
struct CustomWorkoutEditor: View {
    @EnvironmentObject var store: SessionStore
    @Environment(\.dismiss) private var dismiss

    private let id: String
    @State private var name: String
    @State private var exerciseIds: [String]
    @State private var work: Int
    @State private var rest: Int
    @State private var rounds: Int
    @State private var picking = false

    init(existing: Workout? = nil) {
        id = existing?.id ?? "custom-\(UUID().uuidString)"
        _name = State(initialValue: existing?.name ?? "")
        let steps = existing?.steps ?? []
        let r = CustomWorkoutEditor.detectRounds(steps.map(\.exerciseId))
        _rounds = State(initialValue: r)
        _exerciseIds = State(initialValue: Array(steps.map(\.exerciseId).prefix(max(0, steps.count / r))))
        _work = State(initialValue: steps.first?.workSeconds ?? 30)
        _rest = State(initialValue: steps.first?.restSeconds ?? 10)
    }

    /// Builds the saved workout: the exercise list repeated `rounds` times.
    static func makeWorkout(id: String, name: String, exerciseIds: [String], work: Int, rest: Int, rounds: Int) -> Workout {
        let ids = (0..<max(1, rounds)).flatMap { _ in exerciseIds }
        return Workout(id: id, name: name.trimmingCharacters(in: .whitespaces).isEmpty ? "My workout" : name,
                       steps: ids.map { WorkoutStep(exerciseId: $0, workSeconds: work, restSeconds: rest) })
    }

    /// Recovers how many times a saved list repeats, so editing shows the original list.
    static func detectRounds(_ ids: [String]) -> Int {
        guard !ids.isEmpty else { return 1 }
        for r in stride(from: ids.count, through: 2, by: -1) where ids.count % r == 0 {
            let size = ids.count / r
            let first = Array(ids.prefix(size))
            if (0..<r).allSatisfy({ Array(ids[($0 * size)..<(($0 + 1) * size)]) == first }) { return r }
        }
        return 1
    }

    private var draft: Workout {
        Self.makeWorkout(id: id, name: name, exerciseIds: exerciseIds, work: work, rest: rest, rounds: rounds)
    }

    private var durationText: String {
        let seconds = Timeline(workout: draft, getReadySeconds: 0).totalSeconds
        return "\(seconds / 60) min \(seconds % 60) s"
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Name", text: $name)
                }
                Section {
                    Stepper("Work: \(work) s", value: $work, in: 10...90, step: 5)
                    Stepper("Rest: \(rest) s", value: $rest, in: 0...60, step: 5)
                    Stepper("Rounds: \(rounds)", value: $rounds, in: 1...5)
                } footer: {
                    Text(exerciseIds.isEmpty ? "Add at least one exercise." : "Total \(durationText)")
                }
                Section("Exercises") {
                    ForEach(Array(exerciseIds.enumerated()), id: \.offset) { i, exId in
                        let ex = ExerciseLibrary.exercise(exId)
                        Label("\(i + 1). \(ex.name)", systemImage: ex.symbol)
                    }
                    .onMove { exerciseIds.move(fromOffsets: $0, toOffset: $1) }
                    .onDelete { exerciseIds.remove(atOffsets: $0) }
                    Button { picking = true } label: { Label("Add exercise", systemImage: "plus.circle") }
                }
            }
            .navigationTitle(name.isEmpty ? "New workout" : name)
            .navigationBarTitleDisplayMode(.inline)
            .environment(\.editMode, .constant(.active))
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { store.saveCustom(draft); dismiss() }
                        .disabled(exerciseIds.isEmpty)
                }
            }
            .sheet(isPresented: $picking) {
                ExercisePicker { exerciseIds.append($0.id) }
            }
        }
    }
}

/// Library list for adding to a custom workout. Stays open so several can be added.
struct ExercisePicker: View {
    let onPick: (Exercise) -> Void
    @EnvironmentObject var store: SessionStore
    @Environment(\.dismiss) private var dismiss
    @State private var added = 0

    var body: some View {
        NavigationStack {
            List {
                ForEach(BodyArea.allCases) { area in
                    Section(area.title) {
                        ForEach(ExerciseLibrary.browsable.filter { ExerciseTraits.of($0.id).area == area }) { ex in
                            Button {
                                onPick(ex)
                                added += 1
                            } label: {
                                ExerciseRow(exercise: ex, limits: store.profile?.limitations ?? [])
                            }
                            .foregroundStyle(.primary)
                        }
                    }
                }
            }
            .navigationTitle(added == 0 ? "Add exercises" : "\(added) added")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { Button("Done") { dismiss() } }
        }
    }
}
