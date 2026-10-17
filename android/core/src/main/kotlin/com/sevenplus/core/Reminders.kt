package com.sevenplus.core

import kotlinx.serialization.Serializable

/** When to remind the user to work out. Weekdays use the iOS numbering: 1 = Sunday ... 7 = Saturday. */
@Serializable
data class ReminderSettings(
    val enabled: Boolean = false,
    val hour: Int = 18,
    val minute: Int = 0,
    val weekdays: Set<Int> = setOf(2, 3, 4, 5, 6),
) {
    companion object {
        val MESSAGES = listOf(
            "Seven minutes. You've got this.",
            "Quick workout? Your streak is waiting.",
            "Time to move. Pick 5 or 7 minutes.",
        )
    }
}
