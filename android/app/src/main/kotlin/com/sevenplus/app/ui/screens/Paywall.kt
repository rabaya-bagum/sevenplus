package com.sevenplus.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.safeDrawingPadding
import com.sevenplus.app.BuildConfig
import com.sevenplus.app.contracts.LocalAppGraph
import com.sevenplus.app.ui.common.TERMS_URL
import com.sevenplus.app.ui.common.findActivity
import com.sevenplus.app.ui.common.openUrl
import com.sevenplus.app.ui.common.priceLine
import com.sevenplus.app.ui.common.privacyPolicyUrl
import com.sevenplus.app.ui.theme.Orange
import com.sevenplus.app.ui.theme.RestGreen
import kotlinx.coroutines.launch

private val rows = listOf(
    Triple("Classic 7-minute circuit", true, true),
    Triple("Daily workout, 5 and 7 minutes", true, true),
    Triple("Quiet mode and limitations", true, true),
    Triple("Custom workouts", true, true),
    Triple("10, 15 and 20-minute workouts", false, true),
    Triple("Unlimited custom workouts", false, true),
    Triple("Adaptive difficulty from your feedback", false, true),
)

@Composable
fun PaywallScreen(onDismiss: () -> Unit) {
    val graph = LocalAppGraph.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val premium by graph.premium.isPremium.collectAsState()
    val products by graph.premium.products.collectAsState()
    val error by graph.premium.lastError.collectAsState()
    var buying by remember { mutableStateOf(false) }

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                TextButton(onDismiss) { Text("Not now") }
            }
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Icon(Icons.Filled.Bolt, null, Modifier.size(56.dp), tint = Orange)
                Text("SevenPlus Premium", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Longer workouts and no limits. The basics stay free.", textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), RoundedCornerShape(16.dp)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row {
                        Spacer(Modifier.weight(1f))
                        Text("Free", Modifier.width(64.dp), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                        Text("Premium", Modifier.width(64.dp), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                    }
                    rows.forEach { (label, free, prem) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(label, Modifier.weight(1f))
                            Mark(free); Mark(prem)
                        }
                    }
                }
                when {
                    premium -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Verified, null, tint = RestGreen)
                        Spacer(Modifier.width(8.dp))
                        Text("You're on Premium. Thanks!", color = RestGreen)
                    }
                    products.isEmpty() -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Loading prices…")
                    }
                    else -> products.forEach { p ->
                        Button(
                            onClick = {
                                val activity = ctx.findActivity() ?: return@Button
                                buying = true
                                scope.launch {
                                    val ok = graph.premium.purchase(activity, p.id)
                                    buying = false
                                    if (ok) onDismiss()
                                }
                            },
                            enabled = !buying, modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(p.title, fontWeight = FontWeight.SemiBold)
                                Text(priceLine(p), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                TextButton({ scope.launch { graph.premium.restore() } }) { Text("Restore purchases") }
                Text("Subscriptions renew automatically until cancelled in Google Play. Cancel at least 24 hours before renewal to avoid being charged.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    privacyPolicyUrl(BuildConfig.PRIVACY_POLICY_URL)?.let { url ->
                        TextButton({ ctx.openUrl(url) }) { Text("Privacy Policy") }
                    }
                    TextButton({ ctx.openUrl(TERMS_URL) }) { Text("Terms of Use") }
                }
            }
        }
    }
}

@Composable
private fun Mark(on: Boolean) {
    Icon(
        if (on) Icons.Filled.Check else Icons.Filled.Remove, if (on) "Included" else "Not included",
        Modifier.width(64.dp), tint = if (on) RestGreen else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
