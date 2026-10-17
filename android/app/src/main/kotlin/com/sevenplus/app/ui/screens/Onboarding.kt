package com.sevenplus.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sevenplus.core.Goal
import com.sevenplus.core.Level
import com.sevenplus.core.Limitation
import com.sevenplus.core.Profile
import com.sevenplus.core.WorkoutBuilder

private val titles = listOf("What's your goal?", "How fit are you now?", "How long per day?", "Anything to go easy on?")

/** Four quick steps: goal, level, time per day, limitations. */
@Composable
fun OnboardingScreen(onFinish: (Profile) -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var draft by remember { mutableStateOf(Profile()) }
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        LinearProgressIndicator(progress = { (step + 1) / 4f }, modifier = Modifier.fillMaxWidth())
        Text(titles[step], style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            when (step) {
                0 -> Choices(Goal.entries, draft.goal, { it.title }) { draft = draft.copy(goal = it) }
                1 -> Choices(Level.entries, draft.level, { it.label }) { draft = draft.copy(level = it) }
                2 -> {
                    LengthPicker(draft.minutes) { draft = draft.copy(minutes = it) }
                    Spacer(Modifier.padding(6.dp))
                    Text("You can change this any day.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> {
                    Limitation.entries.forEach { lim ->
                        val on = lim in draft.limitations
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                draft = draft.copy(limitations = if (on) draft.limitations - lim else draft.limitations + lim)
                            }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(lim.label, Modifier.weight(1f))
                            Switch(on, { draft = draft.copy(limitations = if (it) draft.limitations + lim else draft.limitations - lim) })
                        }
                    }
                    Text("We'll leave out moves that load these areas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (step > 0) TextButton({ step -= 1 }) { Text("Back") }
            Spacer(Modifier.weight(1f))
            Button({ if (step == 3) onFinish(draft) else step += 1 }) { Text(if (step == 3) "Start training" else "Next") }
        }
    }
}

@Composable
private fun <T> Choices(items: List<T>, selected: T, title: (T) -> String, pick: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEach { item ->
            val on = item == selected
            Row(
                Modifier.fillMaxWidth()
                    .background(
                        if (on) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                        RoundedCornerShape(12.dp),
                    )
                    .selectable(on, role = Role.RadioButton) { pick(item) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title(item), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (on) Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/** Segmented "5 min … 20 min". [label] formats each segment. */
@Composable
fun LengthPicker(selected: Int, label: (Int) -> String = { "$it min" }, onPick: (Int) -> Unit) {
    val lengths = WorkoutBuilder.LENGTHS
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        lengths.forEachIndexed { i, m ->
            SegmentedButton(
                selected = m == selected, onClick = { onPick(m) },
                shape = SegmentedButtonDefaults.itemShape(i, lengths.size), icon = {},
            ) { Text(label(m), maxLines = 1) }
        }
    }
}
