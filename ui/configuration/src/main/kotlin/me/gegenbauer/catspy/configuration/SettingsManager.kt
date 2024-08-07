package me.gegenbauer.catspy.configuration

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.concurrency.ViewModelScope
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.file.clone
import me.gegenbauer.catspy.file.gson
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.platform.currentPlatform
import me.gegenbauer.catspy.strings.globalLocale
import me.gegenbauer.catspy.utils.file.JsonFileManager

val currentSettings by lazy { SettingsManager.settings }

object SettingsManager {
    private const val TAG = "SettingsManager"

    private const val KEY_SETTINGS_FILE = "conf/settings"

    val settings: GSettings by lazy { loadSettings() }
    val string: String
        get() = gson.toJson(settings)

    private val migrations = emptyList<Migration>()

    private val scope = ViewModelScope()

    private val jsonFileManager: JsonFileManager
        get() = ServiceManager.getContextService(JsonFileManager::class.java)

    val adbPath: String
        get() = settings.adbPath.takeIf { it.isNotEmpty() } ?: detectedAdbPath

    private val detectedAdbPath by lazy { currentPlatform.detectAdbPath() }
    private val settingsChangeListeners = mutableListOf<SettingsChangeListener>()

    private suspend fun ensureSettingsFile() {
        withContext(Dispatchers.GIO) {
            val settingsJson = jsonFileManager.read(KEY_SETTINGS_FILE)
            if (settingsJson.isEmpty()) {
                jsonFileManager.write(KEY_SETTINGS_FILE, gson.toJson(GSettings()))
            }
        }
    }

    suspend fun init() {
        ensureSettingsFile()
        GLog.d(TAG, "[init] $settings") // trigger settings init
        withContext(Dispatchers.UI) {
            ThemeManager.init(settings)
        }
        saveInternal()
    }

    private fun loadSettings(): GSettings {
        return kotlin.runCatching {
            gson.fromJson(jsonFileManager.read(KEY_SETTINGS_FILE), GSettings::class.java).apply {
                init()
                checkSettingsUpdate(this)
                GLog.i(TAG, "[loadSettings] $this")
            }
        }.onFailure {
            GLog.e(TAG, "[loadSettings] $it")
        }.getOrDefault(GSettings())
    }

    private fun checkSettingsUpdate(settings: GSettings) {
        val jsonObject = kotlin.runCatching {
            jsonFileManager.read(KEY_SETTINGS_FILE).let { JsonParser.parseString(it).asJsonObject }
        }.onFailure {
            GLog.e(TAG, "[checkSettingsUpdate] $it")
        }.getOrNull() ?: return
        val migrationsToApply = migrations.filter { it.version > settings.version }.sortedBy { it.version }
        for (migration in migrationsToApply) {
            migration.migrate(settings, jsonObject)
        }
    }

    fun updateSettings(editAction: GSettings.() -> Unit) {
        val originalSettings = clone(settings)
        settings.editAction()
        scope.launch {
            withContext(Dispatchers.UI) {
                notifySettingsChanged(originalSettings, settings)
            }
            saveInternal()
        }
    }

    suspend fun suspendedUpdateSettings(editAction: suspend GSettings.() -> Unit) {
        val originalSettings = clone(settings)
        settings.editAction()
        withContext(Dispatchers.UI) {
            notifySettingsChanged(originalSettings, settings)
        }
        saveInternal()
    }

    fun updateLocale() {
        scope.launch {
            if (globalLocale.ordinal != settings.mainUISettings.locale) {
                ThemeManager.applyLocale(settings)
                saveInternal()
            }
        }
    }

    private suspend fun saveInternal() {
        withContext(Dispatchers.GIO) {
            jsonFileManager.write(KEY_SETTINGS_FILE, gson.toJson(settings))
        }
    }

    @Synchronized
    fun addSettingsChangeListener(listener: SettingsChangeListener) {
        settingsChangeListeners.add(listener)
    }

    @Synchronized
    fun removeSettingsChangeListener(listener: SettingsChangeListener) {
        settingsChangeListeners.remove(listener)
    }

    @Synchronized
    private fun notifySettingsChanged(oldSettings: GSettings, newSettings: GSettings) {
        settingsChangeListeners.forEach { it.onSettingsChanged(oldSettings, newSettings) }
    }
}

fun interface SettingsChangeListener {
    fun onSettingsChanged(oldSettings: GSettings, newSettings: GSettings)
}