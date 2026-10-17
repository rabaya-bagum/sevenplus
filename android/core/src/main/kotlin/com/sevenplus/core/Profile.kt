package com.sevenplus.core

import kotlinx.serialization.Serializable

@Serializable
enum class Goal(val label: String) { GET_FIT("Get fit"), LOSE_WEIGHT("Lose weight"), GET_STRONG("Get stronger") }

@Serializable
enum class Level(val label: String, val work: Int, val rest: Int) {
    BEGINNER("Beginner", 30, 15),
    INTERMEDIATE("Intermediate", 30, 10),
    ADVANCED("Advanced", 40, 10),
}

/** Body areas a user wants to protect. NO_JUMPING is quiet mode. */
@Serializable
enum class Limitation(val label: String) {
    KNEES("Knees"), WRISTS("Wrists"), BACK("Lower back"), NO_JUMPING("No jumping (quiet)")
}

@Serializable
enum class BodyArea(val title: String) { CARDIO("Cardio"), LOWER("Lower body"), UPPER("Upper body"), CORE("Core") }

@Serializable
data class Profile(
    val goal: Goal = Goal.GET_FIT,
    val level: Level = Level.BEGINNER,
    /** One of WorkoutBuilder.LENGTHS. */
    val minutes: Int = 7,
    val limitations: Set<Limitation> = emptySet(),
)
