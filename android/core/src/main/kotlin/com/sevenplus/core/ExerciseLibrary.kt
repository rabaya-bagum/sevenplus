package com.sevenplus.core

import com.sevenplus.core.Impact.HIGH
import com.sevenplus.core.Impact.LOW

/** Same 22 exercises and ids as the iOS app (ids also name the bundled 3D clips). */
object ExerciseLibrary {
    val all: List<Exercise> = listOf(
        Exercise("jumping-jacks", "Jumping Jacks", "Land softly, arms all the way up.", HIGH, "step-jacks"),
        Exercise("wall-sit", "Wall Sit", "Knees over ankles, back flat on the wall.", LOW),
        Exercise("push-up", "Push-up", "Straight line from head to heels.", LOW),
        Exercise("crunch", "Abdominal Crunch", "Lift with your core, not your neck.", LOW),
        Exercise("step-up", "Step-up onto Chair", "Drive through the front heel.", LOW),
        Exercise("squat", "Squat", "Hips back, chest up.", LOW),
        Exercise("triceps-dip", "Triceps Dip on Chair", "Elbows point straight back.", LOW),
        Exercise("plank", "Plank", "Squeeze glutes, don't let hips sag.", LOW),
        Exercise("high-knees", "High Knees", "Knees to hip height, stay on your toes.", HIGH, "marching"),
        Exercise("lunge", "Lunge", "Back knee drops straight down.", LOW),
        Exercise("push-up-rotation", "Push-up and Rotation", "Rotate through the whole body.", LOW),
        Exercise("side-plank", "Side Plank", "Stack your feet, lift your hips.", LOW),
        Exercise("burpee", "Burpee", "Chest to floor, then jump and reach up.", HIGH, "step-back-burpee"),
        Exercise("mountain-climber", "Mountain Climber", "Hips level, drive knees to chest.", HIGH, "slow-climber"),
        Exercise("glute-bridge", "Glute Bridge", "Push through heels, squeeze at the top.", LOW),
        Exercise("bird-dog", "Bird-Dog", "Opposite arm and leg, keep your back flat.", LOW),
        Exercise("calf-raise", "Calf Raise", "Rise slowly onto your toes, lower with control.", LOW),
        Exercise("superman", "Superman", "Lift arms and legs, look at the floor.", LOW),
        // Quiet-mode swaps
        Exercise("step-jacks", "Step Jacks", "Step out side to side, arms overhead.", LOW),
        Exercise("marching", "Fast March", "Pump your arms, knees high.", LOW),
        Exercise("step-back-burpee", "Step-back Burpee", "Step back to plank, step in, stand tall.", LOW),
        Exercise("slow-climber", "Slow Mountain Climber", "Step one knee in at a time, hips steady.", LOW),
    )

    private val byId = all.associateBy { it.id }

    fun exercise(id: String): Exercise = byId.getValue(id)

    /** Exercises shown in the library (quiet-mode substitutes appear on their parent's page). */
    val browsable: List<Exercise> get() = all.filter { !ExerciseTraits.of(it.id).swapOnly }

    /** The standard 12-exercise, 30 s on / 10 s rest circuit. Free forever. */
    val classic = Workout(
        id = "classic-7",
        name = "Classic 7-Minute",
        steps = listOf(
            "jumping-jacks", "wall-sit", "push-up", "crunch", "step-up", "squat",
            "triceps-dip", "plank", "high-knees", "lunge", "push-up-rotation", "side-plank",
        ).map { WorkoutStep(it, 30, 10) },
    )

    /** Replaces high-impact moves with their quiet alternatives. */
    fun quiet(workout: Workout): Workout = Workout(
        id = workout.id + "-quiet",
        name = workout.name + " (Quiet)",
        steps = workout.steps.map { step ->
            val ex = exercise(step.exerciseId)
            val swap = ex.quietSwapId
            if (ex.impact == HIGH && swap != null) step.copy(exerciseId = swap) else step
        },
    )
}
