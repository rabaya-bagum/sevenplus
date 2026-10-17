package com.sevenplus.app.challenges

import com.sevenplus.core.Session

object SessionFixture {
    /** [n] completed sessions in the last few minutes, plus one incomplete. */
    fun completed(n: Int): List<Session> {
        val now = System.currentTimeMillis()
        return List(n) { Session("s$it", "w", now - 1000L * (it + 1), 420, true) } +
            Session("x", "w", now - 500, 60, false)
    }
}
