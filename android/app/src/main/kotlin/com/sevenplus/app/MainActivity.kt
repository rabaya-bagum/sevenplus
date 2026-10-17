package com.sevenplus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.sevenplus.app.contracts.LocalAppGraph
import com.sevenplus.app.ui.SevenPlusRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val graph = (application as SevenPlusApplication).graph
        setContent {
            CompositionLocalProvider(LocalAppGraph provides graph) {
                SevenPlusRoot()
            }
        }
    }
}
