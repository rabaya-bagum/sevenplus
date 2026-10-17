package com.sevenplus.app.challenges

import com.sevenplus.core.Challenge
import com.sevenplus.core.ChallengeException
import com.sevenplus.core.ChallengeService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.time.Instant

/**
 * Shares challenges through Supabase (PostgREST), same table and JSON as iOS
 * SupabaseChallengeService: one `challenges` row per challenge, participants as JSON,
 * `participant_ids` for the containment filter, ISO-8601 `start`/`end`.
 */
class SupabaseChallengeService(
    url: String,
    private val anonKey: String,
    private val client: OkHttpClient = OkHttpClient(),
) : ChallengeService {
    private val base = url.trimEnd('/')
    override val isOnline: Boolean get() = true

    @Serializable
    internal data class Row(
        val id: String,
        val code: String,
        val title: String,
        val target: Int,
        val start: String,
        val end: String,
        val participants: List<Challenge.Participant>,
        @SerialName("participant_ids") val participantIds: List<String> = emptyList(),
    ) {
        fun toChallenge() = Challenge(id, code, title, target, parseDate(start), parseDate(end), participants)

        companion object {
            fun of(c: Challenge) = Row(
                c.id, c.code, c.title, c.target,
                formatDate(c.startMillis), formatDate(c.endMillis),
                c.participants, c.participants.map { it.id },
            )
        }
    }

    private val rows = ListSerializer(Row.serializer())

    private suspend fun request(
        path: String,
        method: String = "GET",
        query: List<Pair<String, String>> = emptyList(),
        body: String? = null,
        prefer: String? = null,
    ): String = withContext(Dispatchers.IO) {
        val httpUrl = try {
            "$base/rest/v1/$path".toHttpUrl().newBuilder().apply {
                query.forEach { (k, v) -> addQueryParameter(k, v) }
            }.build()
        } catch (e: IllegalArgumentException) {
            throw ChallengeException.Network(e.message ?: "Bad URL")
        }
        val req = Request.Builder().url(httpUrl)
            .header("apikey", anonKey)
            .header("Authorization", "Bearer $anonKey")
            .header("Content-Type", "application/json")
            .apply { if (prefer != null) header("Prefer", prefer) }
            .method(method, body?.toRequestBody(JSON))
            .build()
        try {
            client.newCall(req).execute().use { resp ->
                val text = resp.body.string()
                if (!resp.isSuccessful) throw ChallengeException.Network(text.ifEmpty { "HTTP error" })
                text
            }
        } catch (e: IOException) {
            throw ChallengeException.Network(e.message ?: e.javaClass.simpleName)
        }
    }

    private fun decode(text: String): List<Challenge> = try {
        json.decodeFromString(rows, text).map { it.toChallenge() }
    } catch (e: Exception) {
        throw ChallengeException.Network("Unexpected response: ${e.message}")
    }

    override suspend fun list(userId: String): List<Challenge> =
        decode(request("challenges", query = listOf("participant_ids" to "cs.{$userId}", "order" to "end.desc")))

    override suspend fun create(challenge: Challenge) {
        request("challenges", "POST", body = json.encodeToString(Row.serializer(), Row.of(challenge)), prefer = "return=minimal")
    }

    private suspend fun fetch(code: String): Challenge =
        decode(request("challenges", query = listOf("code" to "eq.$code"))).firstOrNull()
            ?: throw ChallengeException.NotFound()

    private suspend fun put(c: Challenge) {
        request("challenges", "PATCH", query = listOf("id" to "eq.${c.id}"),
            body = json.encodeToString(Row.serializer(), Row.of(c)), prefer = "return=minimal")
    }

    override suspend fun join(code: String, participant: Challenge.Participant): Challenge {
        var c = fetch(code)
        if (c.participants.none { it.id == participant.id }) {
            c = c.copy(participants = c.participants + participant)
            put(c)
        }
        return c
    }

    override suspend fun removeParticipant(userId: String) {
        for (c in list(userId)) put(c.copy(participants = c.participants.filter { it.id != userId }))
    }

    /** Read-modify-write of this user's own entry (same race as iOS; the next report corrects it). */
    override suspend fun report(challengeId: String, participant: Challenge.Participant) {
        val c = decode(request("challenges", query = listOf("id" to "eq.$challengeId"))).firstOrNull() ?: return
        val i = c.participants.indexOfFirst { it.id == participant.id }
        if (i < 0 || c.participants[i] == participant) return
        put(c.copy(participants = c.participants.toMutableList().also { it[i] = participant }))
    }

    internal companion object {
        val JSON = "application/json".toMediaType()
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        /** Like iOS: `.iso8601` without fractional seconds. */
        fun formatDate(millis: Long): String = Instant.ofEpochMilli(millis).truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString()

        /** Accepts with/without fractional seconds and with offsets (Postgres returns +00:00). */
        fun parseDate(s: String): Long = java.time.OffsetDateTime.parse(s).toInstant().toEpochMilli()
    }
}
