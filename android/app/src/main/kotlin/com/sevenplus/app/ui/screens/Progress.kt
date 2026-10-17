package com.sevenplus.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sevenplus.app.contracts.LocalAppGraph
import com.sevenplus.app.ui.LocalAppUi
import com.sevenplus.app.ui.Routes
import com.sevenplus.app.ui.common.ScreenScaffold
import com.sevenplus.app.ui.common.SectionHeader
import com.sevenplus.app.ui.common.StatCell
import com.sevenplus.app.ui.common.barFractions
import com.sevenplus.core.DayMinutes
import com.sevenplus.core.Stats
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val historyFormat = DateTimeFormatter.ofPattern("EEEE, MMM d")

@Composable
fun ProgressScreen() {
    val graph = LocalAppGraph.current
    val ui = LocalAppUi.current
    val sessions by graph.sessions.sessions.collectAsState()
    val week = Stats.dailyMinutes(sessions)

    ScreenScaffold("Progress") { pad ->
        LazyColumn(Modifier.padding(pad)) {
            item {
                Row(Modifier.fillMaxWidth().padding(16.dp)) {
                    StatCell("${Stats.streak(sessions)}", "Current streak", Modifier.weight(1f))
                    StatCell("${Stats.bestStreak(sessions)}", "Best streak", Modifier.weight(1f))
                    StatCell("${sessions.count { it.completed }}", "Workouts", Modifier.weight(1f))
                    StatCell("${Stats.totalMinutes(sessions)}", "Minutes", Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth().clickable { ui.nav.navigate(Routes.CHALLENGES) }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.EmojiEvents, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("Challenges")
                }
                SectionHeader("Last 7 days")
                WeekChart(week)
                SectionHeader("History")
                if (sessions.isEmpty()) {
                    Text("Finish a workout and it will show up here.", Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(sessions.take(30), key = { it.id }) { s ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(Instant.ofEpochMilli(s.dateMillis).atZone(ZoneId.systemDefault()).format(historyFormat))
                        s.difficulty?.let {
                            Text(it.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(if (s.completed) "${s.durationSeconds / 60} min" else "Stopped early", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** 7-day bar chart drawn with Canvas. */
@Composable
fun WeekChart(week: List<DayMinutes>) {
    val bar = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    val fractions = barFractions(week)
    val max = week.maxOfOrNull { it.minutes } ?: 0
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        .semantics(mergeDescendants = true) { contentDescription = "Minutes trained this week: ${week.sumOf { it.minutes }}" }) {
        Text(if (max > 0) "$max min" else "min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
            val n = fractions.size.coerceAtLeast(1)
            val slot = size.width / n
            val w = slot * 0.55f
            fractions.forEachIndexed { i, f ->
                val x = i * slot + (slot - w) / 2
                drawRoundRect(track, Offset(x, 0f), Size(w, size.height), CornerRadius(6f))
                val h = size.height * f
                if (h > 0f) drawRoundRect(bar, Offset(x, size.height - h), Size(w, h), CornerRadius(6f))
            }
        }
        Row(Modifier.fillMaxWidth()) {
            week.forEach {
                Text(it.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), Modifier.weight(1f),
                    textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
