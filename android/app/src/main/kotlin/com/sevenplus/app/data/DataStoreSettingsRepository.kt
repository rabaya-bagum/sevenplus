package com.sevenplus.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.sevenplus.app.contracts.AppSettings
import com.sevenplus.app.contracts.SettingsRepository
import com.sevenplus.core.KeyValueStore
import com.sevenplus.core.Readiness
import com.sevenplus.core.ReminderSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.UUID

/**
 * Device settings and daily check-ins in Preferences DataStore (iOS UserDefaults keys).
 *
 * DataStore allows one active instance per file per process, so instances are shared per [name]
 * while the scope that created one is alive (e.g. BootReceiver reuses the app's instance).
 */
class DataStoreSettingsRepository(
    context: Context,
    scope: CoroutineScope,
    name: String = "settings",
) : SettingsRepository {

    @Serializable
    private data class Stored(
        val reminders: ReminderSettings = ReminderSettings(),
        val healthEnabled: Boolean = false,
        val crashReportsEnabled: Boolean = false,
        val countReps: Boolean = false,
        val upsellShown: Boolean = false,
        val displayName: String = "Me",
        val userId: String = "",
        val debugPremium: Boolean = false,
    ) {
        fun toApp() = AppSettings(reminders, healthEnabled, crashReportsEnabled, countReps, upsellShown,
            displayName, userId, debugPremium)

        companion object {
            fun of(s: AppSettings) = Stored(s.reminders, s.healthEnabled, s.crashReportsEnabled, s.countReps,
                s.upsellShown, s.displayName, s.userId, s.debugPremium)
        }
    }

    private val store: DataStore<Preferences> = sharedStore(context.applicationContext, scope, name)
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(KV_PREFS, Context.MODE_PRIVATE)

    private val _settings: MutableStateFlow<AppSettings>
    private val _readiness: MutableStateFlow<Map<String, Readiness>>
    override val settings: StateFlow<AppSettings> get() = _settings.asStateFlow()
    override val readiness: StateFlow<Map<String, Readiness>> get() = _readiness.asStateFlow()

    init {
        val first = runBlocking {
            var p = store.data.first()
            if (decodeSettings(p).userId.isEmpty()) {
                p = store.edit { writeSettings(it, decodeSettings(it).copy(userId = newId())) }
            }
            p
        }
        _settings = MutableStateFlow(decodeSettings(first))
        _readiness = MutableStateFlow(decodeReadiness(first))
        scope.launch {
            store.data.collect { p ->
                _settings.value = decodeSettings(p)
                _readiness.value = decodeReadiness(p)
            }
        }
    }

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        val p = store.edit { prefs ->
            val current = decodeSettings(prefs)
            var next = transform(current)
            if (next.userId.isEmpty()) next = next.copy(userId = current.userId.ifEmpty { newId() })
            writeSettings(prefs, next)
        }
        _settings.value = decodeSettings(p)
    }

    override suspend fun recordReadiness(date: LocalDate, readiness: Readiness) {
        val p = store.edit { prefs ->
            val map = decodeReadiness(prefs) + (date.toString() to readiness)
            val trimmed = map.entries.sortedByDescending { it.key }.take(MAX_READINESS_DAYS)
                .associate { it.key to it.value }
            prefs[READINESS] = json.encodeToString(readinessSerializer, trimmed)
        }
        _readiness.value = decodeReadiness(p)
    }

    fun readinessFor(date: LocalDate): Readiness? = _readiness.value[date.toString()]

    override val keyValueStore: KeyValueStore = object : KeyValueStore {
        override fun getString(key: String): String? = prefs.getString(key, null)
        override fun putString(key: String, value: String?) {
            prefs.edit().apply { if (value == null) remove(key) else putString(key, value) }.commit()
        }
    }

    override suspend fun eraseAll() {
        prefs.edit().clear().commit()
        val p = store.edit { it.clear(); writeSettings(it, AppSettings(userId = newId())) }
        _settings.value = decodeSettings(p)
        _readiness.value = decodeReadiness(p)
    }

    private fun writeSettings(prefs: MutablePreferences, s: AppSettings) {
        prefs[SETTINGS] = json.encodeToString(Stored.serializer(), Stored.of(s))
    }

    private fun decodeSettings(p: Preferences): AppSettings =
        p[SETTINGS]?.let { runCatching { json.decodeFromString(Stored.serializer(), it).toApp() }.getOrNull() }
            ?: AppSettings()

    private fun decodeReadiness(p: Preferences): Map<String, Readiness> =
        p[READINESS]?.let { runCatching { json.decodeFromString(readinessSerializer, it) }.getOrNull() }
            ?: emptyMap()

    companion object {
        const val MAX_READINESS_DAYS = 60
        const val KV_PREFS = "sevenplus_kv"
        private val SETTINGS = stringPreferencesKey("appSettings")
        private val READINESS = stringPreferencesKey("readinessLog")
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        private val readinessSerializer = MapSerializer(String.serializer(), Readiness.serializer())
        private fun newId() = UUID.randomUUID().toString()

        private val stores = mutableMapOf<String, DataStore<Preferences>>()

        private fun sharedStore(context: Context, scope: CoroutineScope, name: String): DataStore<Preferences> =
            synchronized(stores) {
                stores[name]?.let { return it }
                val created = PreferenceDataStoreFactory.create(scope = scope) {
                    context.preferencesDataStoreFile(name)
                }
                stores[name] = created
                scope.coroutineContext[Job]?.invokeOnCompletion {
                    synchronized(stores) { if (stores[name] === created) stores.remove(name) }
                }
                created
            }
    }
}
