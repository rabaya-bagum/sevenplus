package com.sevenplus.app.reps

import com.google.mlkit.vision.pose.PoseLandmark
import com.sevenplus.core.RepProfile

/** One detected landmark in image coordinates. */
data class Point2(val x: Double, val y: Double, val confidence: Double)

/** Pure landmark -> joint angle mapping (port of iOS PoseCamera.jointAngle). */
object PoseMapping {
    const val MIN_CONFIDENCE = 0.3

    /** Hip-knee-ankle or shoulder-elbow-wrist, left and right. */
    fun sides(joint: RepProfile.Joint): List<Triple<Int, Int, Int>> = when (joint) {
        RepProfile.Joint.KNEE -> listOf(
            Triple(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE),
            Triple(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE),
        )
        RepProfile.Joint.ELBOW -> listOf(
            Triple(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST),
            Triple(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST),
        )
    }

    /** Angle on the side the camera sees best; null when no side is confident enough. */
    fun jointAngle(points: Map<Int, Point2>, joint: RepProfile.Joint): Double? {
        var best: Pair<Double, Double>? = null
        for ((a, b, c) in sides(joint)) {
            val pa = points[a] ?: continue
            val pb = points[b] ?: continue
            val pc = points[c] ?: continue
            val conf = minOf(pa.confidence, pb.confidence, pc.confidence)
            if (conf <= MIN_CONFIDENCE) continue
            val angle = RepProfile.angle(pa.x, pa.y, pb.x, pb.y, pc.x, pc.y)
            if (best == null || conf > best.first) best = conf to angle
        }
        return best?.second
    }
}
