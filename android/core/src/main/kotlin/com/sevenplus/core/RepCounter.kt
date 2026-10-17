package com.sevenplus.core

/**
 * Counts reps from a joint angle with hysteresis: below [down] then back above [up] = 1 rep.
 * Angles are smoothed (alpha 0.5). Port of iOS `RepCounter`.
 */
class RepCounter(val down: Double, val up: Double) {
    val count: Int get() = TODO("core")
    val isDown: Boolean get() = TODO("core")
    /** Returns true when a rep completes. */
    fun update(angle: Double): Boolean = TODO("core")
    fun reset(): Unit = TODO("core")
}

object RepProfile {
    enum class Joint { KNEE, ELBOW }
    data class Spec(val joint: Joint, val down: Double, val up: Double)

    val BY_EXERCISE: Map<String, Spec> = mapOf(
        "squat" to Spec(Joint.KNEE, 100.0, 160.0),
        "lunge" to Spec(Joint.KNEE, 110.0, 160.0),
        "push-up" to Spec(Joint.ELBOW, 95.0, 155.0),
        "triceps-dip" to Spec(Joint.ELBOW, 100.0, 155.0),
        "calf-raise" to Spec(Joint.KNEE, 150.0, 172.0),
    )

    fun supports(exerciseId: String) = exerciseId in BY_EXERCISE

    /** Angle at b formed by a-b-c in degrees (0..180); 180 for degenerate input. */
    fun angle(ax: Double, ay: Double, bx: Double, by: Double, cx: Double, cy: Double): Double = TODO("core")
}
