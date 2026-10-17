package com.sevenplus.app.reminders

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sevenplus.app.data.DataStoreSettingsRepository
import com.sevenplus.core.ReminderSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime

/** Posts the reminder notification and schedules the same weekday for next week. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val weekday = intent.getIntExtra(AlarmReminderScheduler.EXTRA_WEEKDAY, 0)
        val hour = intent.getIntExtra(AlarmReminderScheduler.EXTRA_HOUR, 18)
        val minute = intent.getIntExtra(AlarmReminderScheduler.EXTRA_MINUTE, 0)
        post(context)
        if (weekday in 1..7) {
            // At least a minute ahead so a slightly early delivery doesn't re-fire today.
            val next = ReminderTimes.next(ZonedDateTime.now().plusMinutes(1), weekday, hour, minute)
            AlarmReminderScheduler(context).schedule(weekday, hour, minute, next)
        }
    }

    @SuppressLint("MissingPermission")
    private fun post(context: Context) {
        val scheduler = AlarmReminderScheduler(context)
        if (!scheduler.canPostNotifications()) return
        AlarmReminderScheduler.ensureChannel(context)
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val content = launch?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val text = message(LocalDate.now())
        val notification = NotificationCompat.Builder(context, AlarmReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("SevenPlus")
            .setContentText(text)
            .setContentIntent(content)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
    }

    companion object {
        private const val NOTIFICATION_ID = 7001
        /** Cycles through the messages, one per day. */
        fun message(date: LocalDate): String =
            ReminderSettings.MESSAGES[(date.toEpochDay() % ReminderSettings.MESSAGES.size).toInt()]
    }
}

/** Re-schedules reminders after a reboot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        processScope.launch {
            try {
                val settings = DataStoreSettingsRepository(context.applicationContext, processScope)
                AlarmReminderScheduler(context).apply(settings.settings.value.reminders)
            } catch (_: Exception) {
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        /** Lives as long as the process so the shared settings DataStore stays valid. */
        val processScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
