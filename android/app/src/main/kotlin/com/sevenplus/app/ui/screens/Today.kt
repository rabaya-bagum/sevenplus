package com.sevenplus.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sevenplus.app.contracts.LocalAppGraph
import com.sevenplus.app.ui.LocalAppUi
import com.sevenplus.app.ui.Routes
import com.sevenplus.app.ui.common.ExerciseThumbnail
import com.sevenplus.app.ui.common.ScreenScaffold
import com.sevenplus.app.ui.common.SectionHeader
import com.sevenplus.app.ui.common.TodayPlan
import com.sevenplus.app.ui.common.lockedLengthsLabel
import com.sevenplus.app.ui.common.rememberIsPremium
import com.sevenplus.app.ui.theme.Orange
import com.sevenplus.core.ExerciseLibrary
import com.sevenplus.core.Gate
import com.sevenplus.core.Profile
import com.sevenplus.core.Readiness
import com.sevenplus.core.Stats
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun TodayScreen() {
    val graph = LocalAppGraph.current
    val ui = LocalAppUi.current
    val scope = rememberCoroutineScope()
    val premium = rememberIsPremium()
    val profile = graph.sessions.profile.collectAsState().value ?: Profile()
    val sessions by graph.sessions.sessions.collectAsState()
    val readinessMap by graph.settings.readiness.collectAsState()
    val readiness = readinessMap[LocalDate.now().toString()]
    var minutes by remember { mutableStateOf<Int?>(null) }
    var draftEnergy by remember { mutableStateOf<Readiness.Energy?>(null) }
    val plan = remember(profile, minutes, premium, readiness, sessions) {
        TodayPlan.compute(profile, minutes, premium, readiness, sessions)
    }
    val streak = Stats.streak(sessions)

    ScreenScaffold("SevenPlus") { pad ->
        LazyColumn(Modifier.padding(pad)) {
            item {
                Card(Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Today", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                            Row(Modifier.semantics(mergeDescendants = true) { contentDescription = "$streak day streak" },
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocalFireDepartment, null, tint = Orange, modifier = Modifier.size(18.dp))
                                Text("$streak", color = Orange, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(plan.workout.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        LengthPicker(plan.minutes, label = { "${it}m" }) { m ->
                            if (Gate.canUseLength(m, premium)) minutes = m else ui.showPaywall = true
                        }
                        lockedLengthsLabel(premium)?.let { IconLine(Icons.Filled.Lock, it, small = true) }
                        Text("${plan.workout.steps.size} exercises · ${plan.intervals.work}s on / ${plan.intervals.rest}s rest",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        readiness?.note?.let { IconLine(Icons.Filled.MonitorHeart, it) }
                        plan.adaptation.summary?.let { IconLine(Icons.Filled.Tune, it, small = true) }
                        Button({ ui.play(plan.workout) }, Modifier.fillMaxWidth()) { Text("Start") }
                    }
                }
            }
            if (readiness == null) {
                item {
                    SectionHeader("How do you feel today?")
                    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val energy = draftEnergy
                        if (energy != null) {
                            Text("Any soreness?")
                            ChoiceRow(listOf("None", "A little", "Very")) { i ->
                                val r = Readiness(energy, Readiness.Soreness.entries[i])
                                scope.launch { graph.settings.recordReadiness(LocalDate.now(), r) }
                            }
                        } else {
                            Text("Energy")
                            ChoiceRow(listOf("Low", "OK", "High")) { draftEnergy = Readiness.Energy.entries[it] }
                        }
                    }
                }
            }
            item { SectionHeader("Today's exercises") }
            itemsIndexed(plan.workout.steps) { i, step ->
                val ex = ExerciseLibrary.exercise(step.exerciseId)
                Row(
                    Modifier.fillMaxWidth().clickable { ui.nav.navigate(Routes.exercise(ex.id)) }.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ExerciseThumbnail(ex, 36.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("${i + 1}. ${ex.name}")
                }
            }
        }
    }
}

@Composable
private fun IconLine(icon: ImageVector, text: String, small: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Text(text, style = if (small) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            color = if (small) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ChoiceRow(labels: List<String>, pick: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { i, l -> OutlinedButton({ pick(i) }, Modifier.weight(1f)) { Text(l) } }
    }
}
