package com.sevenplus.app.reps

import com.google.mlkit.vision.pose.PoseLandmark as L
import com.sevenplus.core.RepCounter
import com.sevenplus.core.RepProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PoseMappingTest {
    private fun p(x: Double, y: Double, c: Double = 0.9) = Point2(x, y, c)

    @Test fun rightAngleKnee() {
        val pts = mapOf(L.LEFT_HIP to p(0.0, 0.0), L.LEFT_KNEE to p(0.0, 1.0), L.LEFT_ANKLE to p(1.0, 1.0))
        assertEquals(90.0, PoseMapping.jointAngle(pts, RepProfile.Joint.KNEE)!!, 1e-6)
        assertNull(PoseMapping.jointAngle(pts, RepProfile.Joint.ELBOW))
    }

    @Test fun picksMoreConfidentSideAndDropsLowConfidence() {
        val pts = mapOf(
            L.LEFT_SHOULDER to p(0.0, 0.0, 0.5), L.LEFT_ELBOW to p(0.0, 1.0, 0.5), L.LEFT_WRIST to p(1.0, 1.0, 0.5),
            L.RIGHT_SHOULDER to p(0.0, 0.0), L.RIGHT_ELBOW to p(0.0, 1.0), L.RIGHT_WRIST to p(0.0, 2.0),
        )
        assertEquals(180.0, PoseMapping.jointAngle(pts, RepProfile.Joint.ELBOW)!!, 1e-6)
        val low = pts.mapValues { it.value.copy(confidence = 0.3) }
        assertNull(PoseMapping.jointAngle(low, RepProfile.Joint.ELBOW))
    }

    @Test fun squatSequenceCountsReps() {
        val spec = RepProfile.BY_EXERCISE.getValue("squat")
        val counter = RepCounter(spec.down, spec.up)
        fun knee(deg: Double): Map<Int, Point2> {
            val r = Math.toRadians(deg)
            return mapOf(L.RIGHT_HIP to p(0.0, -1.0), L.RIGHT_KNEE to p(0.0, 0.0),
                L.RIGHT_ANKLE to p(Math.sin(r), -Math.cos(r)))
        }
        repeat(2) { listOf(180.0, 80.0, 80.0, 80.0, 180.0, 180.0, 180.0).forEach {
            counter.update(PoseMapping.jointAngle(knee(it), spec.joint)!!) } }
        assertEquals(2, counter.count)
    }
}
