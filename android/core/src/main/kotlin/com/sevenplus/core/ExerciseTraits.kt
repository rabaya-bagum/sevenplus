package com.sevenplus.core

import com.sevenplus.core.BodyArea.CARDIO
import com.sevenplus.core.BodyArea.CORE
import com.sevenplus.core.BodyArea.LOWER
import com.sevenplus.core.BodyArea.UPPER
import com.sevenplus.core.Limitation.BACK
import com.sevenplus.core.Limitation.KNEES
import com.sevenplus.core.Limitation.NO_JUMPING
import com.sevenplus.core.Limitation.WRISTS

/** Facts the workout builder needs about each exercise. */
data class ExerciseTraits(
    val area: BodyArea,
    /** Limitations this move is NOT safe for. */
    val stresses: Set<Limitation>,
    /** Only used as a quiet-mode substitute. */
    val swapOnly: Boolean = false,
) {
    companion object {
        val byId: Map<String, ExerciseTraits> = mapOf(
            "jumping-jacks" to ExerciseTraits(CARDIO, setOf(KNEES, NO_JUMPING)),
            "high-knees" to ExerciseTraits(CARDIO, setOf(KNEES, NO_JUMPING)),
            "burpee" to ExerciseTraits(CARDIO, setOf(KNEES, WRISTS, NO_JUMPING)),
            "mountain-climber" to ExerciseTraits(CARDIO, setOf(WRISTS, NO_JUMPING)),
            "wall-sit" to ExerciseTraits(LOWER, setOf(KNEES)),
            "step-up" to ExerciseTraits(LOWER, setOf(KNEES)),
            "squat" to ExerciseTraits(LOWER, setOf(KNEES)),
            "lunge" to ExerciseTraits(LOWER, setOf(KNEES)),
            "glute-bridge" to ExerciseTraits(LOWER, emptySet()),
            "calf-raise" to ExerciseTraits(LOWER, emptySet()),
            "push-up" to ExerciseTraits(UPPER, setOf(WRISTS)),
            "triceps-dip" to ExerciseTraits(UPPER, setOf(WRISTS)),
            "push-up-rotation" to ExerciseTraits(UPPER, setOf(WRISTS)),
            "crunch" to ExerciseTraits(CORE, setOf(BACK)),
            "plank" to ExerciseTraits(CORE, setOf(WRISTS)),
            "side-plank" to ExerciseTraits(CORE, setOf(WRISTS)),
            "bird-dog" to ExerciseTraits(CORE, setOf(WRISTS)),
            "superman" to ExerciseTraits(CORE, setOf(BACK)),
            "step-jacks" to ExerciseTraits(CARDIO, emptySet(), swapOnly = true),
            "marching" to ExerciseTraits(CARDIO, emptySet(), swapOnly = true),
            "step-back-burpee" to ExerciseTraits(CARDIO, setOf(WRISTS), swapOnly = true),
            "slow-climber" to ExerciseTraits(CARDIO, setOf(WRISTS), swapOnly = true),
        )

        fun of(id: String): ExerciseTraits = byId.getValue(id)
    }
}
