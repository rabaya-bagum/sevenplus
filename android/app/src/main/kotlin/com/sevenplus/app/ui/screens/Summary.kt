package com.sevenplus.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sevenplus.app.contracts.LocalAppGraph
import com.sevenplus.app.ui.LocalAppUi
import com.sevenplus.app.ui.common.StatCell
import com.sevenplus.app.ui.common.formatClock
import com.sevenplus.app.ui.common.shareSummaryText
import com.sevenplus.app.ui.common.shareText
import com.sevenplus.app.ui.theme.RestGreen
import com.sevenplus.core.Difficulty
import com.sevenplus.core.Session
import com.sevenplus.core.Stats
import kotlinx.coroutines.launch

@Composable
fun SummaryScreen(session: Session, totalExercises: Int, onDone: () -> Unit) {
    val graph = LocalAppGraph.current
    val ui = LocalAppUi.current
    val ctx = LocalContext.current
    val sessions by graph.sessions.sessions.collectAsState()
    val streak = Stats.streak(sessions)
    var rating by remember { mutableStateOf<Difficulty?>(null) }

    Column(
        Modifier.safeDrawingPadding().fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Spacer(Modifier.weight(1f))
        Icon(Icons.Filled.Verified, null, Modifier.size(80.dp), tint = RestGreen)
        Text("Workout complete", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth()) {
            StatCell(formatClock(session.durationSeconds), "Time", Modifier.weight(1f))
            StatCell("$streak", "Day streak", Modifier.weight(1f))
            StatCell("${totalExercises - session.skipped.size}/$totalExercises", "Exercises", Modifier.weight(1f))
            session.calories?.let { StatCell("$it", "kcal (est.)", Modifier.weight(1f)) }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("How did that feel?", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Difficulty.entries.forEach { d ->
                    val pick = {
                        rating = d
                        // Ratings drive adaptive difficulty for the next workout.
                        ui.scope.launch { graph.sessions.rate(session.id, d) }
                        Unit
                    }
                    if (rating == d) FilledTonalButton(pick) { Text(d.label) } else OutlinedButton(pick) { Text(d.label) }
                }
            }
            Text("Your next workout adapts to this.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton({ ctx.shareText(shareSummaryText(session.durationSeconds, streak)) }) {
            Icon(Icons.Filled.Share, null); Spacer(Modifier.size(8.dp)); Text("Share")
        }
        Spacer(Modifier.weight(1f))
        Button(onDone, Modifier.fillMaxWidth()) { Text("Done") }
    }
}
