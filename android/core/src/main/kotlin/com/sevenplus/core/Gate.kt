package com.sevenplus.core

/** What the free tier includes. Port of iOS `Gate`. */
object Gate {
    val FREE_LENGTHS: Set<Int> = setOf(5, 7)
    const val FREE_CUSTOM_WORKOUTS = 3
    const val UPSELL_AFTER_COMPLETED = 3

    fun canUseLength(length: Int, premium: Boolean): Boolean = TODO("core")
    fun canCreateCustom(existing: Int, premium: Boolean): Boolean = TODO("core")
    fun shouldUpsell(completedCount: Int, premium: Boolean, alreadyShown: Boolean): Boolean = TODO("core")
}

/** Calorie estimate for bodyweight HIIT. Port of iOS `Calories`. */
object Calories {
    const val MET = 8.0
    const val DEFAULT_WEIGHT_KG = 70.0
    /** kcal = MET x weight x hours of active work. */
    fun estimate(activeSeconds: Int, weightKg: Double?): Int = TODO("core")
    /** Seconds spent working (not resting or getting ready). */
    fun activeSeconds(workout: Workout, elapsed: Int, getReady: Int = 5): Int = TODO("core")
}
