package com.sevenplus.app.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.sevenplus.app.contracts.ChallengeRepository
import com.sevenplus.app.contracts.CrashReporting
import com.sevenplus.app.contracts.EraseOutcome
import com.sevenplus.app.contracts.HealthService
import com.sevenplus.app.contracts.ReminderScheduler
import com.sevenplus.app.contracts.SessionRepository
import com.sevenplus.app.contracts.SettingsRepository
import com.sevenplus.app.contracts.YourDataService
import com.sevenplus.core.ExportFile
import com.sevenplus.core.ReminderSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/** Profile -> Your data: export and erase (iOS ExportFile.current + DataEraser). */
class AndroidYourDataService(
    context: Context,
    private val sessions: SessionRepository,
    private val settings: SettingsRepository,
    private val health: HealthService,
    private val reminders: ReminderScheduler,
    private val challenges: ChallengeRepository,
    private val crash: CrashReporting,
) : YourDataService {
    private val context = context.applicationContext
    private val exportDir get() = File(context.cacheDir, "exports")

    fun currentExport(nowMillis: Long = System.currentTimeMillis()): ExportFile {
        val s = settings.settings.value
        return ExportFile(
            exportedAtMillis = nowMillis,
            profile = sessions.profile.value,
            sessions = sessions.sessions.value,
            customWorkouts = sessions.customWorkouts.value,
            reminders = s.reminders,
            readiness = settings.readiness.value,
            challenges = challenges.challenges.value,
            displayName = s.displayName,
        )
    }

    override suspend fun writeExport(): Uri {
        val text = currentExport().encode()
        val file = withContext(Dispatchers.IO) {
            exportDir.mkdirs()
            val f = File(exportDir, "SevenPlus-data-${LocalDate.now()}.json")
            val tmp = File(exportDir, "${f.name}.tmp")
            tmp.writeText(text)
            if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f) }
            f
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    override suspend fun erase(alsoHealth: Boolean, skipRemote: Boolean): EraseOutcome {
        if (!skipRemote && challenges.isOnline) {
            try {
                challenges.leaveAll()
            } catch (e: Exception) {
                return EraseOutcome.RemoteFailed(e.message ?: "Couldn't reach the challenge server.")
            }
        }
        val healthDeleted = if (alsoHealth) health.deleteSavedData() else true
        settings.eraseAll()
        reminders.apply(ReminderSettings())
        crash.setEnabled(false)
        challenges.clearLocal()
        sessions.eraseAll()
        withContext(Dispatchers.IO) { exportDir.deleteRecursively() }
        return EraseOutcome.Erased(healthDeleted)
    }
}
