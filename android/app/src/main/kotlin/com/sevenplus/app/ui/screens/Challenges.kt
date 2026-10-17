package com.sevenplus.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.sevenplus.app.contracts.LocalAppGraph
import com.sevenplus.app.ui.LocalAppUi
import com.sevenplus.app.ui.Routes
import com.sevenplus.app.ui.common.Footer
import com.sevenplus.app.ui.common.ScreenScaffold
import com.sevenplus.app.ui.common.SectionHeader
import com.sevenplus.app.ui.common.shareText
import com.sevenplus.app.ui.theme.RestGreen
import com.sevenplus.core.Challenge
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengesScreen() {
    val graph = LocalAppGraph.current
    val ui = LocalAppUi.current
    val scope = rememberCoroutineScope()
    val sessions by graph.sessions.sessions.collectAsState()
    val settings by graph.settings.settings.collectAsState()
    val list by graph.challenges.challenges.collectAsState()
    val error by graph.challenges.error.collectAsState()
    val online = graph.challenges.isOnline
    var joinCode by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(settings.displayName) }

    LaunchedEffect(Unit) { graph.challenges.refresh(sessions) }

    ScreenScaffold("Challenges", onBack = { ui.nav.popBackStack() }) { pad ->
        PullToRefreshBox(refreshing, onRefresh = {
            scope.launch { refreshing = true; graph.challenges.refresh(sessions); refreshing = false }
        }, Modifier.padding(pad).fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    OutlinedTextField(name, {
                        name = it
                        scope.launch { graph.settings.update { s -> s.copy(displayName = it) } }
                    }, Modifier.fillMaxWidth().padding(16.dp), label = { Text("Your name on leaderboards") }, singleLine = true)
                    if (!online) {
                        Footer("Offline mode: challenges stay on this phone. Connect the online service to race friends (docs/BACKEND.md).")
                    } else {
                        SectionHeader("Join a friend")
                        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(joinCode, { joinCode = it }, Modifier.weight(1f), label = { Text("Invite code") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false))
                            Spacer(Modifier.width(8.dp))
                            Button({ scope.launch { if (graph.challenges.join(joinCode, sessions)) joinCode = "" } },
                                enabled = joinCode.length >= 6) { Text("Join") }
                        }
                    }
                    SectionHeader("Your challenges")
                    if (list.isEmpty()) Text("No challenges yet.", Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(list, key = { it.id }) { c ->
                    ChallengeRow(c, settings.userId, Modifier.clickable { ui.nav.navigate(Routes.challenge(c.id)) })
                }
                item {
                    Row(Modifier.fillMaxWidth().clickable { creating = true }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AddCircleOutline, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("New challenge", color = MaterialTheme.colorScheme.primary)
                    }
                    error?.let { Footer(it, MaterialTheme.colorScheme.error) }
                }
            }
        }
    }

    if (creating) {
        var title by remember { mutableStateOf("") }
        var target by remember { mutableIntStateOf(10) }
        var days by remember { mutableIntStateOf(14) }
        AlertDialog(
            onDismissRequest = { creating = false },
            title = { Text("New challenge") },
            text = {
                Column {
                    OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Name (optional)") }, singleLine = true)
                    Stepper("Workouts: $target", target, 1..60, 1) { target = it }
                    Stepper("Days: $days", days, 3..60, 1) { days = it }
                    Text("Workouts done today count from the start.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton({
                    scope.launch { graph.challenges.create(title, target, days, sessions); creating = false }
                }) { Text("Create") }
            },
            dismissButton = { TextButton({ creating = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ChallengeRow(c: Challenge, me: String, modifier: Modifier) {
    val mine = c.participants.firstOrNull { it.id == me }?.completed ?: 0
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(c.title, fontWeight = FontWeight.SemiBold)
        LinearProgressIndicator(progress = { minOf(mine, c.target).toFloat() / maxOf(1, c.target) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
        Text(if (c.isActive()) "$mine/${c.target} · ${c.daysLeft()} days left" else "Ended · $mine/${c.target}",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ChallengeDetailScreen(id: String) {
    val graph = LocalAppGraph.current
    val ui = LocalAppUi.current
    val ctx = LocalContext.current
    val list by graph.challenges.challenges.collectAsState()
    val settings by graph.settings.settings.collectAsState()
    val c = list.firstOrNull { it.id == id }
    ScreenScaffold(c?.title ?: "Challenge", onBack = { ui.nav.popBackStack() }) { pad ->
        if (c == null) return@ScreenScaffold
        LazyColumn(Modifier.padding(pad)) {
            item { SectionHeader("Leaderboard") }
            itemsIndexed(c.leaderboard, key = { _, p -> p.id }) { rank, p ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${rank + 1}", Modifier.width(28.dp), fontWeight = FontWeight.Bold)
                    Text(if (p.id == settings.userId) "${p.name} (you)" else p.name, Modifier.weight(1f))
                    Text("${p.completed}/${c.target}")
                    if (p.completed >= c.target) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Filled.Verified, "Completed", tint = RestGreen)
                    }
                }
            }
            if (graph.challenges.isOnline) {
                item {
                    Detail("Invite code", c.code)
                    val end = Instant.ofEpochMilli(c.endMillis).atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                    val invite = "Join my SevenPlus challenge \"${c.title}\": ${c.target} workouts by $end. Code: ${c.code}"
                    Row(Modifier.fillMaxWidth().clickable { ctx.shareText(invite) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Share, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Invite friends", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
