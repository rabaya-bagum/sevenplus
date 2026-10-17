package com.sevenplus.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sevenplus.app.BuildConfig
import com.sevenplus.app.contracts.EraseOutcome
import com.sevenplus.app.contracts.HealthAvailability
import com.sevenplus.app.contracts.LocalAppGraph
import com.sevenplus.app.ui.LocalAppUi
import com.sevenplus.app.ui.common.Footer
import com.sevenplus.app.ui.common.ScreenScaffold
import com.sevenplus.app.ui.common.SectionHeader
import com.sevenplus.app.ui.common.openUrl
import com.sevenplus.app.ui.common.privacyPolicyUrl
import kotlinx.coroutines.launch

/** See what's stored, export it as JSON, or erase all of it. */
@Composable
fun YourDataScreen() {
    val graph = LocalAppGraph.current
    val ui = LocalAppUi.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessions by graph.sessions.sessions.collectAsState()
    val customs by graph.sessions.customWorkouts.collectAsState()
    val profile by graph.sessions.profile.collectAsState()
    val readiness by graph.settings.readiness.collectAsState()
    val challenges by graph.challenges.challenges.collectAsState()
    val settings by graph.settings.settings.collectAsState()
    val healthAvailable = graph.health.availability == HealthAvailability.AVAILABLE

    var exportUri by remember { mutableStateOf<Uri?>(null) }
    var exportError by remember { mutableStateOf<String?>(null) }
    var confirmErase by remember { mutableStateOf(false) }
    var alsoHealth by remember { mutableStateOf(true) }
    var erasing by remember { mutableStateOf(false) }
    var remoteFailure by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(sessions.size, customs.size, challenges.size, profile, readiness.size) {
        try {
            exportUri = graph.yourData.writeExport(); exportError = null
        } catch (e: Exception) {
            exportUri = null; exportError = "Couldn't prepare the export: ${e.message}"
        }
    }

    fun erase(skipRemote: Boolean) {
        // App-level scope: erasing clears the profile, which leaves this screen mid-call.
        ui.scope.launch {
            erasing = true
            val outcome = graph.yourData.erase(alsoHealth && healthAvailable, skipRemote)
            erasing = false
            if (outcome is EraseOutcome.RemoteFailed) remoteFailure = outcome.message
        }
    }

    val privacy = privacyPolicyUrl(BuildConfig.PRIVACY_POLICY_URL)
    ScreenScaffold("Your data", onBack = { ui.nav.popBackStack() }, actions = {
        if (privacy != null) TextButton({ ctx.openUrl(privacy) }) { Text("Privacy Policy") }
    }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState())) {
            SectionHeader("Stored on this phone")
            Detail("Workouts logged", "${sessions.size}")
            Detail("Custom workouts", "${customs.size}")
            Detail("Check-ins", "${readiness.size}")
            Detail("Challenges", "${challenges.size}")
            Footer(if (graph.challenges.isOnline)
                "SevenPlus has no account. Everything stays on this phone, except your name and workout count in challenges you share with friends."
            else "SevenPlus has no account. Everything stays on this phone.")

            if (graph.crash.isConfigured) {
                SwitchRow("Share crash reports", settings.crashReportsEnabled) { on ->
                    scope.launch { graph.settings.update { it.copy(crashReportsEnabled = on) } }
                    graph.crash.setEnabled(on)
                }
                Footer("Off by default. When SevenPlus crashes, sends the developer where the code failed, your phone model and Android version. Never your name, workouts or health data.")
            }

            Spacer(Modifier.padding(8.dp))
            val uri = exportUri
            when {
                uri != null -> Row(
                    Modifier.fillMaxWidth().clickable {
                        val send = Intent(Intent.ACTION_SEND).setType("application/json")
                            .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        try { ctx.startActivity(Intent.createChooser(send, null)) } catch (_: Exception) {}
                    }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Share, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Export my data", color = MaterialTheme.colorScheme.primary)
                }
                exportError != null -> Text(exportError!!, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                else -> CircularProgressIndicator(Modifier.padding(16.dp).size(24.dp))
            }
            Footer("A readable JSON file with your profile, workout history, custom workouts, check-ins, reminder times and challenges.")

            Spacer(Modifier.padding(8.dp))
            if (healthAvailable) SwitchRow("Also delete workouts saved to Health Connect", alsoHealth) { alsoHealth = it }
            Row(Modifier.fillMaxWidth().clickable(enabled = !erasing) { confirmErase = true }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                if (erasing) {
                    CircularProgressIndicator(Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Erasing…")
                } else {
                    Text("Erase all my data", color = MaterialTheme.colorScheme.error)
                }
            }
            Footer("This can't be undone. Your Premium subscription isn't affected; manage it in Google Play → Payments & subscriptions. Anything left in Health Connect can be removed in Health Connect → App permissions → SevenPlus.")
            Spacer(Modifier.padding(16.dp))
        }
    }

    if (confirmErase) {
        AlertDialog(
            onDismissRequest = { confirmErase = false },
            title = { Text("Erase all SevenPlus data?") },
            text = { Text("Your history, profile, custom workouts, check-ins, reminders and challenges will be deleted, and you'll start again from setup.") },
            confirmButton = {
                TextButton({ confirmErase = false; erase(skipRemote = false) }) { Text("Erase everything", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton({ confirmErase = false }) { Text("Cancel") } },
        )
    }
    remoteFailure?.let {
        AlertDialog(
            onDismissRequest = { remoteFailure = null },
            title = { Text("Couldn't reach the challenge server") },
            text = { Text("Nothing was deleted. Try again when you're online. If you erase here only, your name and progress stay on friends' leaderboards.") },
            confirmButton = {
                TextButton({ remoteFailure = null; erase(skipRemote = true) }) { Text("Erase on this phone only", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton({ remoteFailure = null }) { Text("Cancel") } },
        )
    }
}
