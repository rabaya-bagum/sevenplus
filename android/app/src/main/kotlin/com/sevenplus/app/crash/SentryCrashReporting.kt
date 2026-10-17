package com.sevenplus.app.crash

import android.content.Context
import com.sevenplus.app.BuildConfig
import com.sevenplus.app.contracts.CrashReporting
import com.sevenplus.app.contracts.SettingsRepository
import io.sentry.Sentry
import io.sentry.android.core.SentryAndroid

/**
 * Opt-in crash reporting (port of iOS CrashReporter). Nothing is sent unless the build has a
 * DSN AND the user turned on AppSettings.crashReportsEnabled. Reports carry the stack, device
 * model and Android version only: no PII, IP, screenshots, view hierarchy or network
 * breadcrumbs (challenge URLs contain the user's random id).
 */
class SentryCrashReporting(
    context: Context,
    private val settings: SettingsRepository,
) : CrashReporting {
    private val appContext = context.applicationContext
    private val dsn: String? = dsn(BuildConfig.SENTRY_DSN)

    override val isConfigured: Boolean get() = dsn != null

    init {
        apply(settings.settings.value.crashReportsEnabled)
    }

    override fun setEnabled(enabled: Boolean) = apply(enabled)

    @Synchronized
    private fun apply(enabled: Boolean) {
        val run = shouldRun(enabled, dsn)
        try {
            if (run && !Sentry.isEnabled()) {
                SentryAndroid.init(appContext) { o ->
                    o.dsn = dsn
                    o.isSendDefaultPii = false
                    o.isAttachScreenshot = false
                    o.isAttachViewHierarchy = false
                    o.isEnableNetworkEventBreadcrumbs = false
                    o.isEnableUserInteractionBreadcrumbs = false
                    o.isEnableAutoSessionTracking = false
                    o.tracesSampleRate = null
                    o.environment = if (BuildConfig.DEBUG) "debug" else "production"
                    o.setBeforeSend { event, _ -> event.apply { user = null; serverName = null } }
                }
            } else if (!run && Sentry.isEnabled()) {
                Sentry.close()
            }
        } catch (_: Throwable) {
            // Crash reporting must never take the app down.
        }
    }

    companion object {
        /** A usable DSN, or null for missing, blank or non-https values. */
        fun dsn(value: String?): String? {
            val s = value?.trim() ?: return null
            return if (s.isNotEmpty() && !s.contains("\$(") && s.startsWith("https://")) s else null
        }

        fun shouldRun(enabled: Boolean, dsn: String?) = enabled && dsn != null
    }
}
