import Foundation

enum ExerciseLibrary {
    static let all: [Exercise] = [
        Exercise(id: "jumping-jacks", name: "Jumping Jacks", cue: "Land softly, arms all the way up.", impact: .high, symbol: "figure.mixed.cardio", quietSwapId: "step-jacks"),
        Exercise(id: "wall-sit", name: "Wall Sit", cue: "Knees over ankles, back flat on the wall.", impact: .low, symbol: "figure.cooldown"),
        Exercise(id: "push-up", name: "Push-up", cue: "Straight line from head to heels.", impact: .low, symbol: "figure.strengthtraining.functional"),
        Exercise(id: "crunch", name: "Abdominal Crunch", cue: "Lift with your core, not your neck.", impact: .low, symbol: "figure.core.training"),
        Exercise(id: "step-up", name: "Step-up onto Chair", cue: "Drive through the front heel.", impact: .low, symbol: "figure.step.training"),
        Exercise(id: "squat", name: "Squat", cue: "Hips back, chest up.", impact: .low, symbol: "figure.strengthtraining.traditional"),
        Exercise(id: "triceps-dip", name: "Triceps Dip on Chair", cue: "Elbows point straight back.", impact: .low, symbol: "figure.arms.open"),
        Exercise(id: "plank", name: "Plank", cue: "Squeeze glutes, don't let hips sag.", impact: .low, symbol: "figure.core.training"),
        Exercise(id: "high-knees", name: "High Knees", cue: "Knees to hip height, stay on your toes.", impact: .high, symbol: "figure.run", quietSwapId: "marching"),
        Exercise(id: "lunge", name: "Lunge", cue: "Back knee drops straight down.", impact: .low, symbol: "figure.walk"),
        Exercise(id: "push-up-rotation", name: "Push-up and Rotation", cue: "Rotate through the whole body.", impact: .low, symbol: "figure.flexibility"),
        Exercise(id: "side-plank", name: "Side Plank", cue: "Stack your feet, lift your hips.", impact: .low, symbol: "figure.pilates"),
        Exercise(id: "burpee", name: "Burpee", cue: "Chest to floor, then jump and reach up.", impact: .high, symbol: "figure.cross.training", quietSwapId: "step-back-burpee"),
        Exercise(id: "mountain-climber", name: "Mountain Climber", cue: "Hips level, drive knees to chest.", impact: .high, symbol: "figure.highintensity.intervaltraining", quietSwapId: "slow-climber"),
        Exercise(id: "glute-bridge", name: "Glute Bridge", cue: "Push through heels, squeeze at the top.", impact: .low, symbol: "figure.flexibility"),
        Exercise(id: "bird-dog", name: "Bird-Dog", cue: "Opposite arm and leg, keep your back flat.", impact: .low, symbol: "figure.yoga"),
        Exercise(id: "calf-raise", name: "Calf Raise", cue: "Rise slowly onto your toes, lower with control.", impact: .low, symbol: "figure.stand"),
        Exercise(id: "superman", name: "Superman", cue: "Lift arms and legs, look at the floor.", impact: .low, symbol: "figure.pool.swim"),
        // Quiet-mode swaps
        Exercise(id: "step-jacks", name: "Step Jacks", cue: "Step out side to side, arms overhead.", impact: .low, symbol: "figure.mixed.cardio"),
        Exercise(id: "marching", name: "Fast March", cue: "Pump your arms, knees high.", impact: .low, symbol: "figure.walk"),
        Exercise(id: "step-back-burpee", name: "Step-back Burpee", cue: "Step back to plank, step in, stand tall.", impact: .low, symbol: "figure.cross.training"),
        Exercise(id: "slow-climber", name: "Slow Mountain Climber", cue: "Step one knee in at a time, hips steady.", impact: .low, symbol: "figure.core.training"),
    ]

    static func exercise(_ id: String) -> Exercise {
        all.first { $0.id == id }!
    }

    /// The standard 12-exercise, 30 s on / 10 s rest circuit. Free forever.
    static let classic = Workout(
        id: "classic-7",
        name: "Classic 7-Minute",
        steps: ["jumping-jacks", "wall-sit", "push-up", "crunch", "step-up", "squat",
                "triceps-dip", "plank", "high-knees", "lunge", "push-up-rotation", "side-plank"]
            .map { WorkoutStep(exerciseId: $0, workSeconds: 30, restSeconds: 10) }
    )

    /// Replaces high-impact moves with their quiet alternatives.
    static func quiet(_ workout: Workout) -> Workout {
        Workout(id: workout.id + "-quiet", name: workout.name + " (Quiet)", steps: workout.steps.map { step in
            let ex = exercise(step.exerciseId)
            guard ex.impact == .high, let swap = ex.quietSwapId else { return step }
            return WorkoutStep(exerciseId: swap, workSeconds: step.workSeconds, restSeconds: step.restSeconds)
        })
    }
}
