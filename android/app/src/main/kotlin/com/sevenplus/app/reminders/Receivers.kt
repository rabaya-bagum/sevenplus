package com.sevenplus.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Posts the reminder notification. (Data & system area.) */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {}
}

/** Re-schedules reminders after a reboot. (Data & system area.) */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {}
}
