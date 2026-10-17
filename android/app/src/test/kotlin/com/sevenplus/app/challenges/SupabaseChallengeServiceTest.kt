package com.sevenplus.app.challenges

import com.sevenplus.core.Challenge
import com.sevenplus.core.ChallengeException
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class SupabaseChallengeServiceTest {
    private lateinit var server: MockWebServer
    private lateinit var service: SupabaseChallengeService

    private val row = """[{"id":"C1","code":"ABC234","title":"Ten","target":10,
        "start":"2026-10-01T00:00:00+00:00","end":"2026-10-08T00:00:00.123Z",
        "participants":[{"id":"u1","name":"Me","completed":2}],"participant_ids":["u1"],"extra":1}]"""

    @Before fun setUp() {
        server = MockWebServer(); server.start()
        service = SupabaseChallengeService(server.url("/").toString(), "KEY")
    }
    @After fun tearDown() { server.close() }

    private fun ok(body: String = "") = server.enqueue(MockResponse.Builder().code(200).body(body).build())

    @Test fun listSendsFilterAndParses() = runTest {
        ok(row)
        val list = service.list("u1")
        val req = server.takeRequest()
        assertEquals("GET", req.method)
        assertEquals("/rest/v1/challenges", req.url.encodedPath)
        assertEquals("cs.{u1}", req.url.queryParameter("participant_ids"))
        assertEquals("end.desc", req.url.queryParameter("order"))
        assertEquals("KEY", req.headers["apikey"])
        assertEquals("Bearer KEY", req.headers["Authorization"])
        assertEquals(1, list.size)
        assertEquals(java.time.Instant.parse("2026-10-01T00:00:00Z").toEpochMilli(), list[0].startMillis)
        assertEquals(java.time.Instant.parse("2026-10-08T00:00:00.123Z").toEpochMilli(), list[0].endMillis)
        assertEquals(2, list[0].participants[0].completed)
    }

    @Test fun createPostsRow() = runTest {
        ok()
        val c = Challenge("C2", "XYZ234", "T", 5, 0, 86_400_000, listOf(Challenge.Participant("u1", "Me", 0)))
        service.create(c)
        val req = server.takeRequest()
        assertEquals("POST", req.method)
        assertEquals("return=minimal", req.headers["Prefer"])
        val body = req.body!!.utf8()
        assertTrue(body, body.contains("\"participant_ids\":[\"u1\"]"))
        assertTrue(body, body.contains("\"start\":\"1970-01-01T00:00:00Z\""))
        assertTrue(body, body.contains("\"end\":\"1970-01-02T00:00:00Z\""))
    }

    @Test fun joinAppendsAndPatches() = runTest {
        ok(row); ok()
        val c = service.join("ABC234", Challenge.Participant("u2", "Friend", 0))
        assertEquals(listOf("u1", "u2"), c.participants.map { it.id })
        assertEquals("eq.ABC234", server.takeRequest().url.queryParameter("code"))
        val patch = server.takeRequest()
        assertEquals("PATCH", patch.method)
        assertEquals("eq.C1", patch.url.queryParameter("id"))
        assertTrue(patch.body!!.utf8().contains("\"participant_ids\":[\"u1\",\"u2\"]"))
    }

    @Test fun joinUnknownCodeIsNotFound() = runTest {
        ok("[]")
        try { service.join("ABC234", Challenge.Participant("u2", "F", 0)); fail() }
        catch (e: ChallengeException.NotFound) { assertEquals("No challenge with that code.", e.message) }
    }

    @Test fun httpErrorIsNetworkWithBody() = runTest {
        server.enqueue(MockResponse.Builder().code(401).body("bad key").build())
        try { service.list("u1"); fail() }
        catch (e: ChallengeException.Network) { assertEquals("Couldn't reach the server: bad key", e.message) }
    }

    @Test fun reportSkipsUnchanged() = runTest {
        ok(row)
        service.report("C1", Challenge.Participant("u1", "Me", 2))
        assertEquals(1, server.requestCount)
        ok(row); ok()
        service.report("C1", Challenge.Participant("u1", "Me", 3))
        assertEquals(3, server.requestCount)
    }

    @Test fun unreachableIsNetwork() = runTest {
        val dead = SupabaseChallengeService("http://127.0.0.1:1", "K")
        try { dead.list("u"); fail() } catch (_: ChallengeException.Network) {}
    }
}
