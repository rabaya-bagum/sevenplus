package com.sevenplus.core

/**
 * Counts reps from a joint angle with hysteresis: below [down] then back above [up] = 1 rep.
 * Angles are smoothed (alpha 0.5). Port of iOS `RepCounter`.
 */
class RepCounter(val down: Double, val up: Double) {
    init { require(down < up) }

    private var _count = 0
    private var _isDown = false
    /** Smooths jitter from the pose detector. */
    private var smoothed: Double? = null
    private val smoothing = 0.5

    val count: Int get() = _count
    val isDown: Boolean get() = _isDown
    /** Returns true when a rep completes. */
    fun update(angle: Double): Boolean {
        val prev = smoothed
        val a = if (prev != null) prev + smoothing * (angle - prev) else angle
        smoothed = a
        if (!_isDown && a < down) {
            _isDown = true
        } else if (_isDown && a > up) {
            _isDown = false
            _count += 1
            return true
        }
        return false
    }

    fun reset() {
        _count = 0
        _isDown = false
        smoothed = null
    }
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
    fun angle(ax: Double, ay: Double, bx: Double, by: Double, cx: Double, cy: Double): Double {
        val v1x = ax - bx; val v1y = ay - by
        val v2x = cx - bx; val v2y = cy - by
        val dot = v1x * v2x + v1y * v2y
        val mags = Math.hypot(v1x, v1y) * Math.hypot(v2x, v2y)
        if (!(mags > 0)) return 180.0
        return Math.acos(maxOf(-1.0, minOf(1.0, dot / mags))) * 180 / Math.PI
    }
}
