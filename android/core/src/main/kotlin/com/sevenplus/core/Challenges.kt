package com.sevenplus.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.Instant
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

    fun isActive(nowMillis: Long = System.currentTimeMillis()): Boolean = nowMillis >= startMillis && nowMillis < endMillis
    fun daysLeft(today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): Int {
        val endDay = Instant.ofEpochMilli(endMillis).atZone(zone).toLocalDate()
        return maxOf(0, java.time.temporal.ChronoUnit.DAYS.between(today, endDay).toInt())
    }
    /** Highest completed first; ties keep join order. */
    val leaderboard: List<Participant> get() = participants.sortedByDescending { it.completed } // stable

    companion object {
        /** Starts at the beginning of today, lasts [days] days. */
        fun make(title: String, target: Int, days: Int, owner: Participant,
                 today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): Challenge {
            val s = today.atStartOfDay(zone)
            return Challenge(
                id = java.util.UUID.randomUUID().toString().uppercase(), code = InviteCode.generate(),
                title = title, target = target,
                startMillis = s.toInstant().toEpochMilli(),
                endMillis = s.plusDays(days.toLong()).toInstant().toEpochMilli(),
                participants = listOf(owner),
            )
        }
        /** Completed sessions inside the window. */
        fun progress(sessions: List<Session>, challenge: Challenge): Int =
            sessions.count { it.completed && it.dateMillis >= challenge.startMillis && it.dateMillis < challenge.endMillis }
    }
}

object InviteCode {
    /** No 0/O or 1/I. */
    const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    fun generate(random: kotlin.random.Random = kotlin.random.Random.Default): String =
        String(CharArray(6) { ALPHABET[random.nextInt(ALPHABET.length)] })
    /** Uppercases, drops spaces and dashes; null unless 6 valid characters. */
    fun normalize(raw: String): String? {
        val code = raw.uppercase().filter { !it.isWhitespace() && it != '-' }
        return if (code.length == 6 && code.all { it in ALPHABET }) code else null
    }
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
    private val key = "localChallenges"
    private val serializer = ListSerializer(Challenge.serializer())

    private var all: List<Challenge>
        get() = store.getString(key)?.let { runCatching { Json.decodeFromString(serializer, it) }.getOrNull() } ?: emptyList()
        set(value) { store.putString(key, Json.encodeToString(serializer, value)) }

    override suspend fun list(userId: String): List<Challenge> =
        all.filter { c -> c.participants.any { it.id == userId } }

    override suspend fun create(challenge: Challenge) { all = all + challenge }
    /** Throws NotConfigured when the code isn't a local challenge. */
    override suspend fun join(code: String, participant: Challenge.Participant): Challenge {
        var c = all.firstOrNull { it.code == code } ?: throw ChallengeException.NotConfigured()
        if (c.participants.none { it.id == participant.id }) c = c.copy(participants = c.participants + participant)
        save(c)
        return c
    }
    override suspend fun report(challengeId: String, participant: Challenge.Participant) {
        val c = all.firstOrNull { it.id == challengeId } ?: return
        val i = c.participants.indexOfFirst { it.id == participant.id }
        if (i < 0) return
        save(c.copy(participants = c.participants.toMutableList().also { it[i] = participant }))
    }
    /** Challenges left empty are deleted. */
    override suspend fun removeParticipant(userId: String) {
        all = all.map { c -> c.copy(participants = c.participants.filter { it.id != userId }) }
            .filter { it.participants.isNotEmpty() }
    }

    private fun save(c: Challenge) {
        val list = all.toMutableList()
        val i = list.indexOfFirst { it.id == c.id }
        if (i >= 0) list[i] = c else list.add(c)
        all = list
    }
}
