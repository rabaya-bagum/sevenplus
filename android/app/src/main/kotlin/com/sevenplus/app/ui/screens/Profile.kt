package com.sevenplus.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sevenplus.app.BuildConfig
import com.sevenplus.app.contracts.HealthAvailability
import com.sevenplus.app.contracts.LocalAppGraph
import com.sevenplus.app.ui.LocalAppUi
import com.sevenplus.app.ui.Routes
import com.sevenplus.app.ui.common.Footer
import com.sevenplus.app.ui.common.ScreenScaffold
import com.sevenplus.app.ui.common.SectionHeader
import com.sevenplus.app.ui.common.iosWeekdayToDayOfWeek
import com.sevenplus.app.ui.theme.RestGreen
import com.sevenplus.core.Goal
import com.sevenplus.core.Level
import com.sevenplus.core.Limitation
import com.sevenplus.core.Profile
import com.sevenplus.core.ReminderSettings
import com.sevenplus.core.WorkoutBuilder
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/** Edit goal, level, default length and limitations after onboarding; reminders, Health, Premium, Your data. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
    val graph = LocalAppGraph.current
    val ui = LocalAppUi.current
    val scope = rememberCoroutineScope()
    val profile = graph.sessions.profile.collectAsState().value ?: Profile()
    val settings by graph.settings.settings.collectAsState()
    val premium by graph.premium.isPremium.collectAsState()
    var permissionDenied by remember { mutableStateOf(!graph.reminders.canPostNotifications() && settings.reminders.enabled) }
    var healthError by remember { mutableStateOf<String?>(null) }
    var pickingTime by remember { mutableStateOf(false) }

    fun setProfile(p: Profile) { scope.launch { graph.sessions.setProfile(p) } }
    fun updateReminders(t: (ReminderSettings) -> ReminderSettings) {
        scope.launch {
            graph.settings.update { it.copy(reminders = t(it.reminders)) }
            graph.reminders.apply(graph.settings.settings.value.reminders)
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
        updateReminders { it.copy(enabled = granted) }
    }
    val healthLauncher = rememberLauncherForActivityResult(graph.health.permissionContract()) { granted ->
        val ok = granted.containsAll(graph.health.permissions)
        healthError = if (ok) null else "Allow SevenPlus to write exercise and calories in Health Connect."
        scope.launch { graph.settings.update { it.copy(healthEnabled = ok) } }
    }

    ScreenScaffold("Profile") { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState())) {
            SectionHeader("Training")
            Dropdown("Goal", Goal.entries, profile.goal, { it.title }) { setProfile(profile.copy(goal = it)) }
            Dropdown("Level", Level.entries, profile.level, { it.label }) { setProfile(profile.copy(level = it)) }
            Dropdown("Default length", WorkoutBuilder.LENGTHS, profile.minutes, { "$it min" }) { setProfile(profile.copy(minutes = it)) }

            SectionHeader("Go easy on")
            Limitation.entries.forEach { lim ->
                SwitchRow(lim.label, lim in profile.limitations) { on ->
                    setProfile(profile.copy(limitations = if (on) profile.limitations + lim else profile.limitations - lim))
                }
            }
            Footer("Daily workouts leave out moves that load these areas.")

            SectionHeader("Reminders")
            SwitchRow("Workout reminders", settings.reminders.enabled) { on ->
                if (on && Build.VERSION.SDK_INT >= 33 && !graph.reminders.canPostNotifications()) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    permissionDenied = on && !graph.reminders.canPostNotifications()
                    updateReminders { it.copy(enabled = on && !permissionDenied) }
                }
            }
            if (settings.reminders.enabled) {
                val time = LocalTime.of(settings.reminders.hour, settings.reminders.minute)
                Row(Modifier.fillMaxWidth().clickable { pickingTime = true }.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("Time", Modifier.weight(1f))
                    Text(time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)), color = MaterialTheme.colorScheme.primary)
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Week order follows iOS numbering: 1 = Sunday ... 7 = Saturday.
                    (1..7).forEach { day ->
                        val on = day in settings.reminders.weekdays
                        val dow = iosWeekdayToDayOfWeek(day)
                        Box(
                            Modifier.weight(1f).aspectRatio(1f)
                                .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), CircleShape)
                                .toggleable(on, role = Role.Checkbox) {
                                    updateReminders { r -> r.copy(weekdays = if (on) r.weekdays - day else r.weekdays + day) }
                                }
                                .semantics { contentDescription = dow.getDisplayName(TextStyle.FULL, Locale.getDefault()); selected = on },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(dow.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
            if (permissionDenied) {
                Footer("Notifications are off for SevenPlus. Turn them on in Settings → Apps → SevenPlus → Notifications.")
            }

            if (graph.health.availability != HealthAvailability.NOT_SUPPORTED) {
                SectionHeader("Health")
                SwitchRow("Save workouts to Health Connect", settings.healthEnabled) { on ->
                    if (!on) {
                        scope.launch { graph.settings.update { it.copy(healthEnabled = false) } }
                    } else if (graph.health.availability != HealthAvailability.AVAILABLE) {
                        healthError = "Install Health Connect from Google Play to save workouts."
                    } else {
                        scope.launch {
                            if (graph.health.hasPermissions()) {
                                healthError = null
                                graph.settings.update { it.copy(healthEnabled = true) }
                            } else {
                                healthLauncher.launch(graph.health.permissions)
                            }
                        }
                    }
                }
                Footer(healthError ?: "Calories are estimated from your weight in Health Connect, or 70 kg if it isn't shared.")
            }

            SectionHeader("Premium")
            if (premium) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Verified, null, tint = RestGreen)
                    Spacer(Modifier.width(8.dp))
                    Text("Premium active", color = RestGreen)
                }
            } else {
                TextButton({ ui.showPaywall = true }, Modifier.padding(horizontal = 4.dp)) { Text("See Premium") }
            }
            TextButton({ scope.launch { graph.premium.restore() } }, Modifier.padding(horizontal = 4.dp)) { Text("Restore purchases") }

            Row(Modifier.fillMaxWidth().clickable { ui.nav.navigate(Routes.YOUR_DATA) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Shield, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text("Your data")
            }
            Footer("Export or erase everything SevenPlus stores.")

            if (BuildConfig.DEBUG) {
                SectionHeader("Developer")
                SwitchRow("Unlock Premium (debug only)", settings.debugPremium) { on ->
                    scope.launch { graph.settings.update { it.copy(debugPremium = on) } }
                }
            }
            Spacer(Modifier.padding(16.dp))
        }
    }

    if (pickingTime) {
        val state = rememberTimePickerState(settings.reminders.hour, settings.reminders.minute)
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            title = { Text("Time") },
            text = { TimePicker(state) },
            confirmButton = {
                TextButton({
                    updateReminders { it.copy(hour = state.hour, minute = state.minute) }
                    pickingTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton({ pickingTime = false }) { Text("Cancel") } },
        )
    }
}

@Composable
internal fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(checked, role = Role.Switch, onValueChange = onChange).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f))
        Switch(checked, onCheckedChange = null)
    }
}

@Composable
private fun <T> Dropdown(label: String, items: List<T>, selected: T, title: (T) -> String, onPick: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(Modifier.fillMaxWidth().clickable { open = true }.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(label, Modifier.weight(1f))
            Text(title(selected), color = MaterialTheme.colorScheme.primary)
        }
        DropdownMenu(open, { open = false }, Modifier.background(Color.Unspecified)) {
            items.forEach { item ->
                DropdownMenuItem(text = { Text(title(item)) }, onClick = { onPick(item); open = false })
            }
        }
    }
}
