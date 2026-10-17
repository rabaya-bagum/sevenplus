package com.sevenplus.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sevenplus.app.contracts.LocalAppGraph
import com.sevenplus.app.ui.LocalAppUi
import com.sevenplus.app.ui.Routes
import com.sevenplus.app.ui.common.ExerciseRow
import com.sevenplus.app.ui.common.ExerciseVisual
import com.sevenplus.app.ui.common.ScreenScaffold
import com.sevenplus.app.ui.common.SectionHeader
import com.sevenplus.core.ExerciseLibrary
import com.sevenplus.core.ExerciseTraits
import com.sevenplus.core.Impact
import com.sevenplus.core.Limitation

@Composable
fun ExerciseDetailScreen(id: String) {
    val graph = LocalAppGraph.current
    val ui = LocalAppUi.current
    val exercise = ExerciseLibrary.all.firstOrNull { it.id == id } ?: return
    val profile by graph.sessions.profile.collectAsState()
    val limits = profile?.limitations ?: emptySet()
    val traits = ExerciseTraits.of(exercise.id)
    val conflicts = Limitation.entries.filter { it in traits.stresses && it in limits }
    val quietSwap = exercise.quietSwapId?.let(ExerciseLibrary::exercise)

    ScreenScaffold(exercise.name, onBack = { ui.nav.popBackStack() }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState())) {
            ExerciseVisual(exercise, Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 320.dp).padding(16.dp))
            Text(exercise.cue, Modifier.fillMaxWidth().padding(horizontal = 16.dp), style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center)
            SectionHeader("Details")
            Detail("Body area", traits.area.title)
            Detail("Impact", if (exercise.impact == Impact.HIGH) "High (jumping)" else "Low")
            Detail("Equipment", if ("dip" in exercise.id || "step-up" in exercise.id) "Chair" else "None")
            if (traits.stresses.isNotEmpty()) {
                Detail("Loads", Limitation.entries.filter { it in traits.stresses }.joinToString(", ") { it.label })
            }
            if (conflicts.isNotEmpty()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text("You asked to go easy on ${conflicts.joinToString(" and ") { it.label }.lowercase()}, so this move is left out of your daily workouts.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (quietSwap != null) {
                SectionHeader("Quiet alternative")
                ExerciseRow(quietSwap, limits, Modifier.clickable { ui.nav.navigate(Routes.exercise(quietSwap.id)) })
            }
        }
    }
}

@Composable
internal fun Detail(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(label, Modifier.weight(1f))
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
