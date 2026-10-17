package com.sevenplus.app.data

import android.content.Context
import android.util.Log
import com.sevenplus.app.contracts.SessionRepository
import com.sevenplus.core.Difficulty
import com.sevenplus.core.Profile
import com.sevenplus.core.Session
import com.sevenplus.core.Workout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/** Workout history, profile and custom workouts in one JSON file (iOS SessionStore). */
class JsonSessionRepository(
    context: Context,
    @Suppress("unused") private val scope: CoroutineScope,
    private val dir: File = File(context.filesDir, "store"),
) : SessionRepository {

    @Serializable
    internal data class Stored(
        val version: Int = 1,
        val sessions: List<Session> = emptyList(),
        val profile: Profile? = null,
        val customWorkouts: List<Workout> = emptyList(),
    )

    private val file = File(dir, FILE_NAME)
    private val mutex = Mutex()
    private var data: Stored = load()

    private val _sessions = MutableStateFlow(data.sessions)
    private val _profile = MutableStateFlow(data.profile)
    private val _custom = MutableStateFlow(data.customWorkouts)
    override val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()
    override val profile: StateFlow<Profile?> = _profile.asStateFlow()
    override val customWorkouts: StateFlow<List<Workout>> = _custom.asStateFlow()

    private fun load(): Stored = try {
        if (file.exists()) json.decodeFromString(Stored.serializer(), file.readText()) else Stored()
    } catch (e: Exception) {
        Log.w(TAG, "Couldn't read ${file.name}; starting empty", e)
        file.renameTo(File(dir, "$FILE_NAME.corrupt"))
        Stored()
    }

    private suspend fun mutate(transform: (Stored) -> Stored) = mutex.withLock {
        val next = transform(data)
        data = next
        _sessions.value = next.sessions
        _profile.value = next.profile
        _custom.value = next.customWorkouts
        withContext(Dispatchers.IO) { write(next) }
    }

    private fun write(stored: Stored) {
        try {
            dir.mkdirs()
            val tmp = File(dir, "$FILE_NAME.tmp")
            tmp.writeText(json.encodeToString(Stored.serializer(), stored))
            if (!tmp.renameTo(file)) {
                file.delete()
                if (!tmp.renameTo(file)) error("rename failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't save ${file.name}", e)
        }
    }

    override suspend fun add(session: Session) = mutate { d ->
        d.copy(sessions = (listOf(session) + d.sessions.filter { it.id != session.id }).sortedByDescending { it.dateMillis })
    }

    override suspend fun rate(sessionId: String, difficulty: Difficulty) = mutate { d ->
        d.copy(sessions = d.sessions.map { if (it.id == sessionId) it.copy(difficulty = difficulty) else it })
    }

    override suspend fun setProfile(profile: Profile?) = mutate { it.copy(profile = profile) }

    override suspend fun saveCustom(workout: Workout) = mutate { d ->
        d.copy(customWorkouts = listOf(workout) + d.customWorkouts.filter { it.id != workout.id })
    }

    override suspend fun deleteCustom(id: String) = mutate { d ->
        d.copy(customWorkouts = d.customWorkouts.filter { it.id != id })
    }

    override suspend fun eraseAll() = mutate { Stored() }

    private companion object {
        const val TAG = "JsonSessionRepository"
        const val FILE_NAME = "sessions.json"
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }
}
