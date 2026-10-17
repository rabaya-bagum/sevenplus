package com.sevenplus.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sevenplus.app.contracts.LocalAppGraph
import com.sevenplus.app.ui.screens.ChallengeDetailScreen
import com.sevenplus.app.ui.screens.ChallengesScreen
import com.sevenplus.app.ui.screens.CustomWorkoutEditorScreen
import com.sevenplus.app.ui.screens.ExerciseDetailScreen
import com.sevenplus.app.ui.screens.OnboardingScreen
import com.sevenplus.app.ui.screens.PaywallScreen
import com.sevenplus.app.ui.screens.PlayerScreen
import com.sevenplus.app.ui.screens.ProfileScreen
import com.sevenplus.app.ui.screens.ProgressScreen
import com.sevenplus.app.ui.screens.TodayScreen
import com.sevenplus.app.ui.screens.WorkoutsScreen
import com.sevenplus.app.ui.screens.YourDataScreen
import com.sevenplus.app.ui.theme.SevenPlusTheme
import com.sevenplus.core.Workout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** App-wide UI state: the full-screen player and the paywall (iOS fullScreenCover/sheet). */
class AppUiState(val scope: CoroutineScope) {
    var playing by mutableStateOf<Workout?>(null)
    var showPaywall by mutableStateOf(false)
    /** Set when a completed workout should show the upsell after the player closes. */
    var upsellPending = false
    lateinit var nav: NavHostController
    fun play(w: Workout) { playing = w }
}

val LocalAppUi = staticCompositionLocalOf<AppUiState> { error("AppUiState not provided") }

object Routes {
    const val TODAY = "today"
    const val WORKOUTS = "workouts"
    const val PROGRESS = "progress"
    const val PROFILE = "profile"
    const val EXERCISE = "exercise/{id}"
    const val CHALLENGES = "challenges"
    const val CHALLENGE = "challenge/{id}"
    const val YOUR_DATA = "yourdata"
    const val EDITOR = "editor/{id}"
    fun exercise(id: String) = "exercise/$id"
    fun challenge(id: String) = "challenge/$id"
    /** "new" for a new workout. */
    fun editor(id: String?) = "editor/${id ?: "new"}"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.TODAY, "Today", Icons.Filled.WbSunny),
    Tab(Routes.WORKOUTS, "Workouts", Icons.AutoMirrored.Filled.DirectionsRun),
    Tab(Routes.PROGRESS, "Progress", Icons.Filled.BarChart),
    Tab(Routes.PROFILE, "Profile", Icons.Filled.AccountCircle),
)

/** Root of the app: onboarding until a profile exists, then the four tabs. (Screens area.) */
@Composable
fun SevenPlusRoot() {
    val graph = LocalAppGraph.current
    SevenPlusTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            LaunchedEffect(Unit) { graph.premium.start() }
            val scope = rememberCoroutineScope()
            val ui = remember { AppUiState(scope) }
            val profile by graph.sessions.profile.collectAsState()
            CompositionLocalProvider(LocalAppUi provides ui) {
                if (profile == null) {
                    OnboardingScreen { p -> scope.launch { graph.sessions.setProfile(p) } }
                } else {
                    MainTabs(ui)
                }
                val playing = ui.playing
                if (playing != null) {
                    BackHandler { /* use End */ }
                    PlayerScreen(playing, onClose = {
                        ui.playing = null
                        if (ui.upsellPending) {
                            ui.upsellPending = false
                            scope.launch { graph.settings.update { it.copy(upsellShown = true) } }
                            ui.showPaywall = true
                        }
                    })
                } else if (ui.showPaywall) {
                    BackHandler { ui.showPaywall = false }
                    PaywallScreen(onDismiss = { ui.showPaywall = false })
                }
            }
        }
    }
}

@Composable
private fun MainTabs(ui: AppUiState) {
    val nav = rememberNavController()
    ui.nav = nav
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route
    Scaffold(
        bottomBar = {
            if (tabs.any { it.route == current }) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = current == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            NavHost(nav, startDestination = Routes.TODAY) {
                composable(Routes.TODAY) { TodayScreen() }
                composable(Routes.WORKOUTS) { WorkoutsScreen() }
                composable(Routes.PROGRESS) { ProgressScreen() }
                composable(Routes.PROFILE) { ProfileScreen() }
                composable(Routes.EXERCISE) { ExerciseDetailScreen(it.arguments?.getString("id").orEmpty()) }
                composable(Routes.CHALLENGES) { ChallengesScreen() }
                composable(Routes.CHALLENGE) { ChallengeDetailScreen(it.arguments?.getString("id").orEmpty()) }
                composable(Routes.YOUR_DATA) { YourDataScreen() }
                composable(Routes.EDITOR) {
                    val id = it.arguments?.getString("id")
                    CustomWorkoutEditorScreen(if (id == "new") null else id)
                }
            }
        }
    }
}
