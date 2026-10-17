package com.sevenplus.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val Orange = Color(0xFFFF8A00)
val RestGreen = Color(0xFF34C759)

private val Light = lightColorScheme(
    primary = Color(0xFFE66A00), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCC2), onPrimaryContainer = Color(0xFF301400),
    secondary = Color(0xFF755846), tertiary = Color(0xFF2E7D32),
)
private val Dark = darkColorScheme(
    primary = Color(0xFFFFB77C), onPrimary = Color(0xFF4F2500),
    primaryContainer = Color(0xFF713700), onPrimaryContainer = Color(0xFFFFDCC2),
    secondary = Color(0xFFE5BFA8), tertiary = Color(0xFF81C784),
)

@Composable
fun SevenPlusTheme(dark: Boolean = isSystemInDarkTheme(), dynamic: Boolean = false, content: @Composable () -> Unit) {
    val scheme = when {
        dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        dark -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
