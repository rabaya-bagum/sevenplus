package com.sevenplus.app

import android.app.Application
import com.sevenplus.app.contracts.AppGraph
import com.sevenplus.app.contracts.fakeAppGraph

class SevenPlusApplication : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = buildGraph()
    }

    /** Wired to the real implementations once each area lands; fakes until then. */
    private fun buildGraph(): AppGraph = fakeAppGraph()
}
