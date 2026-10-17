package com.sevenplus.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/** Root of the app: onboarding until a profile exists, then the four tabs. (Screens area.) */
@Composable
fun SevenPlusRoot() {
    MaterialTheme { Text("SevenPlus") }
}
