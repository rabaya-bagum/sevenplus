package com.sevenplus.core

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.ZoneId

/** "Complete N workouts in D days" with friends. Port of iOS `Challenges.swift`. */
@Serializable
data class Challenge(
    val id: String,
    /** 6-character invite code. */
    val code: String,
    val title: String,
    val target: Int,
    val startMillis: Long,
    val endMillis: Long,
    val participants: List<Participant>,
) {
    @Serializable
    data class Participant(val id: String, val name: String, val completed: Int)

    fun isActive(nowMillis: Long = System.currentTimeMillis()): Boolean = TODO("core")
    fun daysLeft(today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): Int = TODO("core")
    /** Highest completed first; ties keep join order. */
    val leaderboard: List<Participant> get() = TODO("core")

    companion object {
        /** Starts at the beginning of today, lasts [days] days. */
        fun make(title: String, target: Int, days: Int, owner: Participant,
                 today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): Challenge = TODO("core")
        /** Completed sessions inside the window. */
        fun progress(sessions: List<Session>, challenge: Challenge): Int = TODO("core")
    }
}

object InviteCode {
    /** No 0/O or 1/I. */
    const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    fun generate(random: kotlin.random.Random = kotlin.random.Random.Default): String = TODO("core")
    /** Uppercases, drops spaces and dashes; null unless 6 valid characters. */
    fun normalize(raw: String): String? = TODO("core")
}

sealed class ChallengeException(message: String) : Exception(message) {
    class NotFound : ChallengeException("No challenge with that code.")
    class Network(detail: String) : ChallengeException("Couldn't reach the server: $detail")
    class NotConfigured : ChallengeException("Joining friends' challenges needs the online service.")
}

/** Where challenges live: [LocalChallengeService] on-device, or the app's Supabase service. */
interface ChallengeService {
    val isOnline: Boolean
    suspend fun list(userId: String): List<Challenge>
    suspend fun create(challenge: Challenge)
    suspend fun join(code: String, participant: Challenge.Participant): Challenge
    suspend fun report(challengeId: String, participant: Challenge.Participant)
    /** Removes this user from every challenge (erasing data). */
    suspend fun removeParticipant(userId: String)
}

/** Simple string storage so core code can persist without Android. */
interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String?)
}

class InMemoryKeyValueStore : KeyValueStore {
    private val map = mutableMapOf<String, String>()
    override fun getString(key: String) = map[key]
    override fun putString(key: String, value: String?) { if (value == null) map.remove(key) else map[key] = value }
}

/** On-device only: you can track your own challenges; friends can't join. */
class LocalChallengeService(private val store: KeyValueStore) : ChallengeService {
    override val isOnline: Boolean get() = false
    override suspend fun list(userId: String): List<Challenge> = TODO("core")
    override suspend fun create(challenge: Challenge): Unit = TODO("core")
    /** Throws NotConfigured when the code isn't a local challenge. */
    override suspend fun join(code: String, participant: Challenge.Participant): Challenge = TODO("core")
    override suspend fun report(challengeId: String, participant: Challenge.Participant): Unit = TODO("core")
    /** Challenges left empty are deleted. */
    override suspend fun removeParticipant(userId: String): Unit = TODO("core")
}
