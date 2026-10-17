package com.sevenplus.core

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ChallengeTest {
    private fun session(offsetDays: Double, completed: Boolean = true) =
        T.session(T.NOW_MILLIS + (offsetDays * T.DAY_MILLIS).toLong(), completed = completed)

    @Test fun windowAndProgress() {
        val c = Challenge.make("T", 5, 7, Challenge.Participant("a", "A", 0), T.TODAY, T.ZONE)
        assertEquals(T.TODAY.atStartOfDay(T.ZONE).toInstant().toEpochMilli(), c.startMillis)
        assertEquals(T.TODAY.plusDays(7).atStartOfDay(T.ZONE).toInstant().toEpochMilli(), c.endMillis)
        val sessions = listOf(session(0.1), session(1.0), session(2.0, completed = false), session(-3.0), session(8.0))
        assertEquals(2, Challenge.progress(sessions, c))
        assertTrue(c.isActive(T.NOW_MILLIS + 3_600_000))
        assertFalse(c.isActive(T.NOW_MILLIS + 8 * T.DAY_MILLIS))
        assertEquals(7, c.daysLeft(T.TODAY, T.ZONE))
        assertEquals(0, c.daysLeft(T.TODAY.plusDays(9), T.ZONE))
    }

    @Test fun leaderboardOrder() {
        var c = Challenge.make("T", 5, 7, Challenge.Participant("a", "A", 2))
        c = c.copy(participants = c.participants + listOf(Challenge.Participant("b", "B", 4), Challenge.Participant("c", "C", 2)))
        assertEquals("ties keep join order", listOf("b", "a", "c"), c.leaderboard.map { it.id })
    }

    @Test fun inviteCodes() {
        val rng = Random(7)
        repeat(50) {
            val code = InviteCode.generate(rng)
            assertEquals(code, InviteCode.normalize(code))
            assertFalse(code.contains("O") || code.contains("0") || code.contains("I") || code.contains("1"))
        }
        assertEquals("AB2CD3", InviteCode.normalize(" ab2-cd3 "))
        assertNull("too short", InviteCode.normalize("ab2-cd"))
        assertEquals("ABCDEF", InviteCode.normalize("abc-def"))
        assertNull("0 is not in the alphabet", InviteCode.normalize("ABCDE0"))
    }

    @Test fun localServiceCreateListReport() = runTest {
        val store = InMemoryKeyValueStore()
        val service = LocalChallengeService(store)
        var me = Challenge.Participant("me", "Me", 0)
        val c = Challenge.make("T", 3, 7, me)
        service.create(c)
        assertEquals(listOf(c.id), service.list("me").map { it.id })
        assertTrue(service.list("someone-else").isEmpty())
        me = me.copy(completed = 2)
        service.report(c.id, me)
        assertEquals(2, service.list("me").first().participants.first().completed)
        assertTrue(store.getString("localChallenges") != null)
        // Persists across instances.
        assertEquals(listOf(c.id), LocalChallengeService(store).list("me").map { it.id })
    }

    @Test fun localServiceJoinAndRemove() = runTest {
        val service = LocalChallengeService(InMemoryKeyValueStore())
        val c = Challenge.make("T", 3, 7, Challenge.Participant("me", "Me", 0))
        service.create(c)
        val joined = service.join(c.code, Challenge.Participant("you", "You", 0))
        assertEquals(listOf("me", "you"), joined.participants.map { it.id })
        assertEquals(2, service.join(c.code, Challenge.Participant("you", "You", 0)).participants.size)
        try {
            service.join("ZZZZZZ", Challenge.Participant("x", "X", 0)); throw AssertionError("expected NotConfigured")
        } catch (e: ChallengeException.NotConfigured) { }
        service.removeParticipant("me")
        assertEquals(listOf(c.id), service.list("you").map { it.id })
        service.removeParticipant("you")
        assertTrue(service.list("you").isEmpty())
    }
}
