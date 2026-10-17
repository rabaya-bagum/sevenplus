package com.sevenplus.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sevenplus.app.contracts.LocalAppGraph
import com.sevenplus.app.ui.LocalAppUi
import com.sevenplus.app.ui.common.ExerciseVisual
import com.sevenplus.app.ui.common.PlayerText
import com.sevenplus.app.ui.theme.Orange
import com.sevenplus.app.ui.theme.RestGreen
import com.sevenplus.core.Calories
import com.sevenplus.core.CoachScript
import com.sevenplus.core.ExerciseLibrary
import com.sevenplus.core.Gate
import com.sevenplus.core.Phase
import com.sevenplus.core.RepProfile
import com.sevenplus.core.Session
import com.sevenplus.core.Workout
import com.sevenplus.core.WorkoutBuilder
import com.sevenplus.core.WorkoutEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun PlayerScreen(workout: Workout, onClose: () -> Unit) {
    val graph = LocalAppGraph.current
    val ui = LocalAppUi.current
    val ctx = LocalContext.current
    val view = LocalView.current
    val engine = remember(workout) { WorkoutEngine(workout) }
    val phase by engine.phase.collectAsState()
    val paused by engine.isPaused.collectAsState()
    val current by engine.workout.collectAsState()
    val settings by graph.settings.settings.collectAsState()
    val profile by graph.sessions.profile.collectAsState()
    var finished by remember { mutableStateOf<Session?>(null) }
    var showingSwaps by remember { mutableStateOf(false) }
    var weightKg by remember { mutableStateOf<Double?>(null) }
    val coach = remember { VoiceCoach(ctx) }

    fun finish(completed: Boolean) {
        if (finished != null) return
        engine.pause()
        val end = System.currentTimeMillis()
        val elapsed = engine.elapsedSeconds
        val active = Calories.activeSeconds(engine.workout.value, elapsed, engine.timeline.getReadySeconds)
        val healthOn = graph.settings.settings.value.healthEnabled
        val kcal = Calories.estimate(active, if (healthOn) weightKg else null)
        val s = Session(
            id = UUID.randomUUID().toString().uppercase(), workoutId = workout.id, dateMillis = end,
            durationSeconds = elapsed, completed = completed, skipped = engine.skipped, difficulty = null, calories = kcal,
        )
        // App scope: these must finish even after the player closes.
        ui.scope.launch {
            graph.sessions.add(s)
            if (healthOn) runCatching { graph.health.saveWorkout(end - elapsed * 1000L, end, kcal) }
            if (completed) {
                val all = graph.sessions.sessions.value
                runCatching { graph.challenges.reportProgress(all) }
                val st = graph.settings.settings.value
                if (Gate.shouldUpsell(all.count { it.completed }, graph.premium.isPremium.value, st.upsellShown)) ui.upsellPending = true
            }
        }
        if (completed) finished = s else onClose()
    }
    val finishRef by rememberUpdatedState(::finish)

    DisposableEffect(engine) {
        view.keepScreenOn = true
        engine.onPhaseChange = { p ->
            coach.say(CoachScript.line(p, engine.workout.value))
            if (p == Phase.Finished) finishRef(true)
        }
        engine.start()
        onDispose {
            engine.onPhaseChange = null
            view.keepScreenOn = false
            coach.release()
        }
    }
    LaunchedEffect(engine) {
        if (graph.settings.settings.value.healthEnabled) weightKg = runCatching { graph.health.latestWeightKg() }.getOrNull()
    }
    LaunchedEffect(engine) {
        while (true) { engine.tick(); delay(250) }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) engine.tick() }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }

    Surface(Modifier.fillMaxSize()) {
        val done = finished
        if (done != null) {
            SummaryScreen(done, current.steps.size, onClose)
            return@Surface
        }
        val currentExercise = when (val p = phase) {
            is Phase.Work -> ExerciseLibrary.exercise(current.steps[p.index].exerciseId)
            is Phase.GetReady -> ExerciseLibrary.exercise(current.steps[0].exerciseId)
            else -> null
        }
        val resting = phase is Phase.Rest
        val visual = (phase as? Phase.Rest)?.let { p ->
            if (p.index + 1 < current.steps.size) ExerciseLibrary.exercise(current.steps[p.index + 1].exerciseId) else null
        } ?: currentExercise
        val tint = if (resting) RestGreen else Orange
        val remaining = PlayerText.remaining(phase)
        val ring by animateFloatAsState(PlayerText.ringFraction(phase, current, engine.timeline.getReadySeconds), tween(250), label = "ring")
        val swapIndex = engine.swapTargetIndex
        val swapTarget = swapIndex?.takeIf { it < current.steps.size }?.let { ExerciseLibrary.exercise(current.steps[it].exerciseId) }
        val swapLabel = if (resting) "Swap next exercise" else "Swap ${swapTarget?.name ?: "exercise"}"
        val swapOptions = swapTarget?.let { WorkoutBuilder.swapOptions(it.id, profile?.limitations ?: emptySet()) } ?: emptyList()
        val progress = engine.progress

        Column(
            Modifier.safeDrawingPadding().fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton({ finish(false) }) { Text("End") }
                Spacer(Modifier.weight(1f))
                Text(PlayerText.stepLabel(phase, current), Modifier.padding(end = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            Text(PlayerText.title(phase, current) { ExerciseLibrary.exercise(it).name },
                style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            if (visual != null) {
                ExerciseVisual(visual, Modifier.fillMaxWidth().heightIn(min = 150.dp, max = 280.dp).padding(horizontal = 16.dp)
                    .alpha(if (resting) 0.75f else 1f), playing = !paused)
            }
            currentExercise?.let {
                Text(it.cue, Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
            val track = MaterialTheme.colorScheme.surfaceVariant
            Box(Modifier.size(150.dp).semantics(mergeDescendants = true) { contentDescription = "$remaining seconds remaining" },
                contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize().padding(7.dp)) {
                    drawArc(track, 0f, 360f, false, style = Stroke(14.dp.toPx()))
                    drawArc(tint, -90f, 360f * ring, false, style = Stroke(14.dp.toPx(), cap = StrokeCap.Round))
                }
                Text("$remaining", fontSize = 52.sp, fontWeight = FontWeight.Bold)
            }
            val ex = currentExercise
            if (ex != null && phase is Phase.Work && RepProfile.supports(ex.id)) {
                if (settings.countReps) {
                    graph.reps.Panel(ex.id, active = !paused, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                } else {
                    OutlinedButton({ ui.scope.launch { graph.settings.update { it.copy(countReps = true) } } }) {
                        Icon(Icons.Filled.CameraAlt, null); Spacer(Modifier.size(8.dp)); Text("Count my reps")
                    }
                }
            }
            (phase as? Phase.Rest)?.let { p ->
                if (p.index + 1 < current.steps.size) {
                    Text("Next: ${ExerciseLibrary.exercise(current.steps[p.index + 1].exerciseId).name}", style = MaterialTheme.typography.titleMedium)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton({ if (paused) engine.resume() else engine.pause() }, Modifier.size(72.dp)) {
                    Icon(if (paused) Icons.Filled.PlayCircle else Icons.Filled.PauseCircle, if (paused) "Resume" else "Pause",
                        Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton({ engine.skip() }, Modifier.size(56.dp)) { Icon(Icons.Filled.SkipNext, "Skip", Modifier.size(44.dp)) }
                IconButton({ showingSwaps = true }, Modifier.size(56.dp), enabled = swapOptions.isNotEmpty()) {
                    Icon(Icons.Filled.SwapHoriz, swapLabel, Modifier.size(44.dp))
                }
            }
        }

        if (showingSwaps) {
            AlertDialog(
                onDismissRequest = { showingSwaps = false },
                title = { Text(swapLabel) },
                text = {
                    Column {
                        swapOptions.forEach { o ->
                            Text(o.name, Modifier.fillMaxWidth().clickable { engine.swap(o.id); showingSwaps = false }.padding(vertical = 12.dp))
                        }
                    }
                },
                confirmButton = { TextButton({ showingSwaps = false }) { Text("Cancel") } },
            )
        }
    }
}
