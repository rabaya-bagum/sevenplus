package com.sevenplus.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sevenplus.app.contracts.LocalAppGraph
import com.sevenplus.app.ui.LocalAppUi
import com.sevenplus.app.ui.common.ExerciseRow
import com.sevenplus.app.ui.common.ExerciseThumbnail
import com.sevenplus.app.ui.common.Footer
import com.sevenplus.app.ui.common.ScreenScaffold
import com.sevenplus.app.ui.common.SectionHeader
import com.sevenplus.core.BodyArea
import com.sevenplus.core.CustomWorkouts
import com.sevenplus.core.ExerciseLibrary
import com.sevenplus.core.ExerciseTraits
import com.sevenplus.core.Timeline
import kotlinx.coroutines.launch
import java.util.UUID

/** Build or edit your own circuit: pick exercises, order them, set work/rest and rounds. */
@Composable
fun CustomWorkoutEditorScreen(existingId: String?) {
    val graph = LocalAppGraph.current
    val ui = LocalAppUi.current
    val scope = rememberCoroutineScope()
    val customs by graph.sessions.customWorkouts.collectAsState()
    val profile by graph.sessions.profile.collectAsState()
    val existing = remember(existingId) { customs.firstOrNull { it.id == existingId } }
    val id = remember { existing?.id ?: "custom-${UUID.randomUUID().toString().uppercase()}" }
    val steps = existing?.steps ?: emptyList()
    val initialRounds = remember { CustomWorkouts.detectRounds(steps.map { it.exerciseId }) }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    val ids = remember { mutableStateListOf<String>().apply { addAll(steps.map { it.exerciseId }.take(maxOf(0, steps.size / initialRounds))) } }
    var work by remember { mutableIntStateOf(steps.firstOrNull()?.workSeconds ?: 30) }
    var rest by remember { mutableIntStateOf(steps.firstOrNull()?.restSeconds ?: 10) }
    var rounds by remember { mutableIntStateOf(initialRounds) }
    var picking by remember { mutableStateOf(false) }

    val draft = CustomWorkouts.make(id, name, ids.toList(), work, rest, rounds)
    val seconds = Timeline(draft, getReadySeconds = 0).totalSeconds

    ScreenScaffold(
        if (name.isEmpty()) "New workout" else name,
        onBack = { ui.nav.popBackStack() },
        actions = {
            TextButton(
                onClick = { scope.launch { graph.sessions.saveCustom(draft) }; ui.nav.popBackStack() },
                enabled = ids.isNotEmpty(),
            ) { Text("Save") }
        },
    ) { pad ->
        LazyColumn(Modifier.padding(pad)) {
            item {
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth().padding(16.dp), label = { Text("Name") }, singleLine = true)
                Stepper("Work: $work s", work, 10..90, 5) { work = it }
                Stepper("Rest: $rest s", rest, 0..60, 5) { rest = it }
                Stepper("Rounds: $rounds", rounds, 1..5, 1) { rounds = it }
                Footer(if (ids.isEmpty()) "Add at least one exercise." else "Total ${seconds / 60} min ${seconds % 60} s")
                SectionHeader("Exercises")
            }
            itemsIndexed(ids) { i, exId ->
                val ex = ExerciseLibrary.exercise(exId)
                Row(Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    ExerciseThumbnail(ex, 36.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("${i + 1}. ${ex.name}", Modifier.weight(1f))
                    IconButton({ if (i > 0) ids.add(i - 1, ids.removeAt(i)) }, enabled = i > 0) { Icon(Icons.Filled.KeyboardArrowUp, "Move up") }
                    IconButton({ if (i < ids.size - 1) ids.add(i + 1, ids.removeAt(i)) }, enabled = i < ids.size - 1) {
                        Icon(Icons.Filled.KeyboardArrowDown, "Move down")
                    }
                    IconButton({ ids.removeAt(i) }) { Icon(Icons.Filled.Delete, "Remove ${ex.name}") }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().clickable { picking = true }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AddCircleOutline, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Add exercise", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    if (picking) {
        var added by remember { mutableIntStateOf(0) }
        val limits = profile?.limitations ?: emptySet()
        AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text(if (added == 0) "Add exercises" else "$added added") },
            text = {
                LazyColumn(Modifier.heightIn(max = 480.dp)) {
                    BodyArea.entries.forEach { area ->
                        item { SectionHeader(area.title) }
                        items(ExerciseLibrary.browsable.filter { ExerciseTraits.of(it.id).area == area }) { ex ->
                            ExerciseRow(ex, limits, Modifier.clickable { ids.add(ex.id); added += 1 })
                        }
                    }
                }
            },
            confirmButton = { TextButton({ picking = false }) { Text("Done") } },
        )
    }
}

@Composable
internal fun Stepper(label: String, value: Int, range: IntRange, step: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        IconButton({ onChange(maxOf(range.first, value - step)) }, enabled = value > range.first) { Icon(Icons.Filled.Remove, "Decrease") }
        IconButton({ onChange(minOf(range.last, value + step)) }, enabled = value < range.last) { Icon(Icons.Filled.Add, "Increase") }
    }
}
