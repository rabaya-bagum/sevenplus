package com.sevenplus.app.ui

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sevenplus.app.contracts.LocalAppGraph
import com.sevenplus.app.contracts.fakeAppGraph
import com.sevenplus.core.Goal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class OnboardingFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun onboardingSavesProfileAndShowsTabs() {
        val graph = fakeAppGraph()
        compose.setContent { CompositionLocalProvider(LocalAppGraph provides graph) { SevenPlusRoot() } }
        compose.onNodeWithText("What's your goal?").assertExists()
        compose.onNodeWithText(Goal.LOSE_WEIGHT.title).performClick()
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("How fit are you now?").assertExists()
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("How long per day?").assertExists()
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("Start training").performClick()
        compose.waitForIdle()
        assertEquals(Goal.LOSE_WEIGHT, graph.sessions.profile.value?.goal)
        compose.onNodeWithText("Workouts").assertExists()
        assertTrue(compose.onAllNodesWithText("Start").fetchSemanticsNodes().isNotEmpty())
    }

    @Test fun lockedLengthOpensPaywall() {
        val graph = fakeAppGraph(com.sevenplus.core.Profile())
        compose.setContent { CompositionLocalProvider(LocalAppGraph provides graph) { SevenPlusRoot() } }
        compose.onNodeWithText("15m").performClick()
        compose.onNodeWithText("SevenPlus Premium").assertExists()
        compose.onNodeWithText("Terms of Use").assertExists()
    }
}
