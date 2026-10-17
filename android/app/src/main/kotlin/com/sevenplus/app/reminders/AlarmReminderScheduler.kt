package com.sevenplus.app.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sevenplus.app.contracts.ReminderScheduler
import com.sevenplus.core.ReminderSettings
import java.time.ZonedDateTime

/** Weekly reminders with inexact, windowed alarms (no exact-alarm permission needed). */
class AlarmReminderScheduler(context: Context) : ReminderScheduler {
    private val context = context.applicationContext
    private val alarms get() = context.getSystemService(AlarmManager::class.java)

    override fun apply(settings: ReminderSettings) {
        for (day in 1..7) cancel(day)
        if (!settings.enabled) return
        ensureChannel(context)
        val now = ZonedDateTime.now()
        for (day in settings.weekdays.filter { it in 1..7 }) {
            schedule(day, settings.hour, settings.minute, ReminderTimes.next(now, day, settings.hour, settings.minute))
        }
    }

    internal fun schedule(weekday: Int, hour: Int, minute: Int, at: ZonedDateTime) {
        val pi = pendingIntent(weekday, hour, minute, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        runCatching {
            alarms?.setWindow(AlarmManager.RTC_WAKEUP, at.toInstant().toEpochMilli(), WINDOW_MILLIS, pi)
        }
    }

    private fun cancel(weekday: Int) {
        pendingIntent(weekday, 0, 0, PendingIntent.FLAG_NO_CREATE)?.let { alarms?.cancel(it); it.cancel() }
    }

    private fun pendingIntent(weekday: Int, hour: Int, minute: Int, flag: Int): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ACTION)
            .putExtra(EXTRA_WEEKDAY, weekday).putExtra(EXTRA_HOUR, hour).putExtra(EXTRA_MINUTE, minute)
        return PendingIntent.getBroadcast(context, REQUEST_BASE + weekday, intent, flag or PendingIntent.FLAG_IMMUTABLE)
    }

    override fun canPostNotifications(): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val CHANNEL_ID = "reminders"
        const val ACTION = "com.sevenplus.app.REMINDER"
        const val EXTRA_WEEKDAY = "weekday"
        const val EXTRA_HOUR = "hour"
        const val EXTRA_MINUTE = "minute"
        private const val REQUEST_BASE = 7000
        /** Android may deliver the alarm anywhere in this window after the trigger time. */
        const val WINDOW_MILLIS = 10 * 60 * 1000L

        fun ensureChannel(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Workout reminders", NotificationManager.IMPORTANCE_DEFAULT)
                )
            }
        }
    }
}
