package com.sevenplus.app.challenges

import com.sevenplus.app.BuildConfig
import com.sevenplus.app.contracts.ChallengeRepository
import com.sevenplus.app.contracts.SettingsRepository
import com.sevenplus.core.Challenge
import com.sevenplus.core.ChallengeService
import com.sevenplus.core.InviteCode
import com.sevenplus.core.LocalChallengeService
import com.sevenplus.core.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Supabase when the build has its keys, else on-device challenges. */
fun challengeService(settings: SettingsRepository): ChallengeService =
    if (BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank())
        SupabaseChallengeService(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY)
    else LocalChallengeService(settings.keyValueStore)

/** Port of iOS ChallengeStore. userId/displayName come from [settings]. */
class AppChallengeRepository(
    private val service: ChallengeService,
    private val settings: SettingsRepository,
    @Suppress("unused") private val scope: CoroutineScope,
    override val isOnline: Boolean = service.isOnline,
) : ChallengeRepository {
    private val _challenges = MutableStateFlow<List<Challenge>>(emptyList())
    override val challenges: StateFlow<List<Challenge>> = _challenges.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()
    private val mutex = Mutex()

    private val userId get() = settings.settings.value.userId
    private val displayName get() = settings.settings.value.displayName
    private val me get() = Challenge.Participant(userId, displayName, 0)

    private fun message(e: Throwable) = e.message ?: e.javaClass.simpleName

    override suspend fun refresh(sessions: List<Session>) {
        try {
            val list = service.list(userId)
            mutex.withLock { _challenges.value = list }
            reportProgress(sessions)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            _error.value = message(e)
        }
    }

    override suspend fun create(title: String, target: Int, days: Int, sessions: List<Session>) {
        val made = Challenge.make(if (title.isBlank()) "$target workouts in $days days" else title, target, days, me)
        val c = made.copy(participants = listOf(made.participants[0].copy(completed = Challenge.progress(sessions, made))))
        try {
            service.create(c)
            mutex.withLock { _challenges.value = listOf(c) + _challenges.value }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            _error.value = message(e)
        }
    }

    override suspend fun join(code: String, sessions: List<Session>): Boolean {
        val normalized = InviteCode.normalize(code) ?: run {
            _error.value = "Invite codes are 6 letters and numbers."
            return false
        }
        return try {
            val c = service.join(normalized, me)
            mutex.withLock {
                if (_challenges.value.none { it.id == c.id }) _challenges.value = listOf(c) + _challenges.value
            }
            reportProgress(sessions)
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            _error.value = message(e)
            false
        }
    }

    override suspend fun reportProgress(sessions: List<Session>) {
        val id = userId
        val name = displayName
        for (c in _challenges.value) {
            val i = c.participants.indexOfFirst { it.id == id }
            if (i < 0) continue
            val updated = c.participants[i].copy(completed = Challenge.progress(sessions, c), name = name)
            if (updated == c.participants[i]) continue
            mutex.withLock {
                _challenges.value = _challenges.value.map { x ->
                    if (x.id != c.id) x
                    else x.copy(participants = x.participants.map { if (it.id == id) updated else it })
                }
            }
            runCatching { service.report(c.id, updated) }
        }
    }

    override suspend fun leaveAll() {
        service.removeParticipant(userId)
        mutex.withLock { _challenges.value = emptyList() }
    }

    override fun clearLocal() {
        _challenges.value = emptyList()
        _error.value = null
    }
}
