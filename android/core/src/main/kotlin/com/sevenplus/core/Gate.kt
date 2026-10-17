package com.sevenplus.core

/** What the free tier includes. Port of iOS `Gate`. */
object Gate {
    val FREE_LENGTHS: Set<Int> = setOf(5, 7)
    const val FREE_CUSTOM_WORKOUTS = 3
    const val UPSELL_AFTER_COMPLETED = 3

    fun canUseLength(length: Int, premium: Boolean): Boolean = premium || length in FREE_LENGTHS
    fun canCreateCustom(existing: Int, premium: Boolean): Boolean = premium || existing < FREE_CUSTOM_WORKOUTS
    fun shouldUpsell(completedCount: Int, premium: Boolean, alreadyShown: Boolean): Boolean =
        !premium && !alreadyShown && completedCount == UPSELL_AFTER_COMPLETED
}

/** Calorie estimate for bodyweight HIIT. Port of iOS `Calories`. */
object Calories {
    const val MET = 8.0
    const val DEFAULT_WEIGHT_KG = 70.0
    /** kcal = MET x weight x hours of active work. */
    fun estimate(activeSeconds: Int, weightKg: Double?): Int {
        val kg = weightKg ?: DEFAULT_WEIGHT_KG
        // Swift .rounded(): half away from zero.
        val x = MET * kg * activeSeconds.toDouble() / 3600
        return (if (x < 0) -Math.floor(-x + 0.5) else Math.floor(x + 0.5)).toInt()
    }
    /** Seconds spent working (not resting or getting ready). */
    fun activeSeconds(workout: Workout, elapsed: Int, getReady: Int = 5): Int {
        var t = maxOf(0, elapsed - getReady)
        var active = 0
        for (step in workout.steps) {
            val w = minOf(t, step.workSeconds)
            active += w
            t -= w
            if (t <= 0) break
            t = maxOf(0, t - step.restSeconds)
        }
        return active
    }
}
