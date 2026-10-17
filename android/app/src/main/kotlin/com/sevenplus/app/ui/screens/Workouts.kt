package com.sevenplus.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sevenplus.app.contracts.LocalAppGraph
import com.sevenplus.app.ui.LocalAppUi
import com.sevenplus.app.ui.Routes
import com.sevenplus.app.ui.common.ExerciseRow
import com.sevenplus.app.ui.common.ScreenScaffold
import com.sevenplus.app.ui.common.SectionHeader
import com.sevenplus.app.ui.common.rememberIsPremium
import com.sevenplus.core.BodyArea
import com.sevenplus.core.ExerciseLibrary
import com.sevenplus.core.ExerciseTraits
import com.sevenplus.core.Gate
import com.sevenplus.core.Limitation
import kotlinx.coroutines.launch

/** Ready-made circuits plus the full exercise library, searchable and grouped by body area. */
@Composable
fun WorkoutsScreen() {
    val graph = LocalAppGraph.current
    val ui = LocalAppUi.current
    val scope = rememberCoroutineScope()
    val premium = rememberIsPremium()
    val profile by graph.sessions.profile.collectAsState()
    val custom by graph.sessions.customWorkouts.collectAsState()
    var search by rememberSaveable { mutableStateOf("") }
    val limits = profile?.limitations ?: emptySet()
    val classic = if (Limitation.NO_JUMPING in limits) ExerciseLibrary.quiet(ExerciseLibrary.classic) else ExerciseLibrary.classic
    val byArea = BodyArea.entries.associateWith { area ->
        ExerciseLibrary.browsable.filter { ExerciseTraits.of(it.id).area == area }
            .filter { search.isBlank() || it.name.contains(search.trim(), ignoreCase = true) }
    }

    ScreenScaffold("Workouts") { pad ->
        LazyColumn(Modifier.padding(pad)) {
            item {
                OutlinedTextField(
                    search, { search = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    placeholder = { Text("Search exercises") }, leadingIcon = { Icon(Icons.Filled.Search, null) }, singleLine = true,
                )
            }
            if (search.isBlank()) {
                item {
                    SectionHeader("Circuits")
                    Column(Modifier.fillMaxWidth().clickable { ui.play(classic) }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Bolt, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(classic.name, fontWeight = FontWeight.SemiBold)
                        }
                        Text("12 exercises · 7 min · free forever", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    SectionHeader("My workouts")
                }
                items(custom, key = { it.id }) { w ->
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).clickable { ui.play(w) }.padding(vertical = 8.dp)) {
                            Text(w.name, fontWeight = FontWeight.SemiBold)
                            Text("${w.steps.size} exercises · ${maxOf(1, w.totalSeconds / 60)} min",
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton({ ui.nav.navigate(Routes.editor(w.id)) }) { Icon(Icons.Filled.Edit, "Edit ${w.name}") }
                        IconButton({ scope.launch { graph.sessions.deleteCustom(w.id) } }) { Icon(Icons.Filled.Delete, "Delete ${w.name}") }
                    }
                }
                item {
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            if (Gate.canCreateCustom(custom.size, premium)) ui.nav.navigate(Routes.editor(null)) else ui.showPaywall = true
                        }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.AddCircleOutline, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("New workout", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            BodyArea.entries.forEach { area ->
                val list = byArea.getValue(area)
                if (list.isNotEmpty()) {
                    item { SectionHeader(area.title) }
                    items(list, key = { "${area.name}-${it.id}" }) { ex ->
                        ExerciseRow(ex, limits, Modifier.clickable { ui.nav.navigate(Routes.exercise(ex.id)) })
                    }
                }
            }
            if (search.isNotBlank() && byArea.values.all { it.isEmpty() }) {
                item {
                    Text("No results for \"${search.trim()}\"", Modifier.fillMaxWidth().padding(32.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
