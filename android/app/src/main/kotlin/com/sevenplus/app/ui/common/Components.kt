package com.sevenplus.app.ui.common

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.sevenplus.app.BuildConfig
import com.sevenplus.app.contracts.AppGraph
import com.sevenplus.app.contracts.LocalAppGraph
import com.sevenplus.core.Exercise
import com.sevenplus.core.ExerciseTraits
import com.sevenplus.core.Impact
import com.sevenplus.core.Limitation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ---- Media (port of iOS ExerciseMedia / ExerciseVisual) ----

object ExerciseMedia {
    private val cache = LruCache<String, Bitmap>(40)

    private fun exists(ctx: Context, path: String): Boolean =
        try { ctx.assets.open(path).close(); true } catch (_: Exception) { false }

    /** asset:/// Uri of the looping clip, dark variant first in dark mode; null when missing. */
    fun clipUri(ctx: Context, id: String, dark: Boolean): Uri? {
        if (dark && exists(ctx, "ExerciseClips/$id-dark.mp4")) return Uri.parse("asset:///ExerciseClips/$id-dark.mp4")
        if (exists(ctx, "ExerciseClips/$id.mp4")) return Uri.parse("asset:///ExerciseClips/$id.mp4")
        return null
    }

    fun thumbnail(ctx: Context, id: String, dark: Boolean): Bitmap? {
        val names = if (dark) listOf("$id-dark-thumb.jpg", "$id-thumb.jpg") else listOf("$id-thumb.jpg")
        for (n in names) {
            cache.get(n)?.let { return it }
            val bmp = try {
                ctx.assets.open("ExerciseClips/$n").use { BitmapFactory.decodeStream(it) }
            } catch (_: Exception) { null }
            if (bmp != null) { cache.put(n, bmp); return bmp }
        }
        return null
    }
}

@Composable
fun ExerciseThumbnail(exercise: Exercise, size: Dp) {
    val ctx = LocalContext.current
    val dark = isSystemInDarkTheme()
    val bmp by produceState<Bitmap?>(null, exercise.id, dark) {
        value = withContext(Dispatchers.IO) { ExerciseMedia.thumbnail(ctx, exercise.id, dark) }
    }
    val shape = RoundedCornerShape(8.dp)
    Box(Modifier.size(size).clip(shape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        val b = bmp
        if (b != null) {
            Image(b.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
        } else {
            Icon(Icons.Filled.FitnessCenter, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Looping, muted 3D clip. Falls back to an icon when the clip is missing. */
@OptIn(UnstableApi::class)
@Composable
fun ExerciseVisual(exercise: Exercise, modifier: Modifier = Modifier, playing: Boolean = true) {
    val ctx = LocalContext.current
    val dark = isSystemInDarkTheme()
    val uri = remember(exercise.id, dark) { ExerciseMedia.clipUri(ctx, exercise.id, dark) }
    if (uri == null) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.FitnessCenter, exercise.name, Modifier.size(96.dp), tint = MaterialTheme.colorScheme.primary)
        }
        return
    }
    val player = remember(uri) {
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            prepare()
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    player.playWhenReady = playing
    AndroidView(
        factory = { c ->
            PlayerView(c).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                contentDescription = exercise.name
            }
        },
        update = { it.player = player },
        modifier = modifier,
    )
}

@Composable
fun ExerciseRow(exercise: Exercise, limits: Set<Limitation>, modifier: Modifier = Modifier) {
    val conflicts = ExerciseTraits.of(exercise.id).stresses.intersect(limits)
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        ExerciseThumbnail(exercise, 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(exercise.name)
            if (exercise.impact == Impact.HIGH) {
                Text("High impact", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (conflicts.isNotEmpty()) Icon(Icons.Filled.Warning, "Not in your daily workouts", tint = Color(0xFFFFC107))
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 6.dp),
    )
}

@Composable
fun Footer(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = color, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
}

@Composable
fun StatCell(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---- Helpers ----

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun Context.openUrl(url: String) {
    try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
}

fun Context.shareText(text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    try { startActivity(Intent.createChooser(send, null)) } catch (_: Exception) {}
}

/** Premium, or the debug-only unlock. */
@Composable
fun rememberIsPremium(graph: AppGraph = LocalAppGraph.current): Boolean {
    val premium by graph.premium.isPremium.collectAsState()
    val settings by graph.settings.settings.collectAsState()
    return premium || (BuildConfig.DEBUG && settings.debugPremium)
}
