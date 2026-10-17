package com.sevenplus.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Everything SevenPlus keeps about the user, as one readable JSON file. Port of iOS `ExportFile`. */
@Serializable
data class ExportFile(
    val version: Int = CURRENT_VERSION,
    val exportedAtMillis: Long,
    val profile: Profile?,
    val sessions: List<Session>,
    val customWorkouts: List<Workout>,
    val reminders: ReminderSettings,
    /** "yyyy-MM-dd" -> check-in */
    val readiness: Map<String, Readiness>,
    val challenges: List<Challenge>,
    val displayName: String,
) {
    /** Pretty-printed JSON. */
    fun encode(): String = JSON.encodeToString(serializer(), this)

    companion object {
        const val CURRENT_VERSION = 1
        fun decode(json: String): ExportFile = JSON.decodeFromString(serializer(), json)

        private val JSON = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }
    }
}
