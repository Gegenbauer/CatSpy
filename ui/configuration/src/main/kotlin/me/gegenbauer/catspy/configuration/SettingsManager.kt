package me.gegenbauer.catspy.configuration

import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.concurrency.ViewModelScope
import me.gegenbauer.catspy.file.clone
import me.gegenbauer.catspy.file.gson
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.platform.filesDir
import java.io.File

val currentSettings by lazy { SettingsManager.settings }

object SettingsManager {
    private const val TAG = "SettingsManager"
    private const val SETTINGS_FILENAME = "settings.json"

    val settings: GSettings by lazy { loadSettings() }
    val string: String
        get() = gson.toJson(settings)

    private val settingsFile = File(filesDir, SETTINGS_FILENAME)
    private val migrations = listOf(Migration1To2())

    private val scope = ViewModelScope()

    private suspend fun ensureSettingsFile() {
        withContext(Dispatchers.GIO) {
            if (!settingsFile.exists()) {
                settingsFile.createNewFile()
                settingsFile.writeText(gson.toJson(GSettings()))
            } else if (settingsFile.length().toInt() == 0) {
                settingsFile.writeText(gson.toJson(GSettings()))
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
            JsonReader(settingsFile.reader()).use {
                gson.fromJson<GSettings>(it, GSettings::class.java).apply {
                    init()
                    checkSettingsUpdate(this)
                    GLog.i(TAG, "[loadUI] $this")
                }
            }
        }.onFailure {
            GLog.e(TAG, "[loadUI] $it")
        }.getOrDefault(GSettings())
    }

    private fun checkSettingsUpdate(settings: GSettings) {
        val jsonObject = kotlin.runCatching {
            settingsFile.reader().use {
                JsonParser.parseReader(it).asJsonObject
            }
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
                ThemeManager.update(originalSettings, settings)
            }
            saveInternal()
        }
    }

    suspend fun suspendUpdateSettings(editAction: suspend GSettings.() -> Unit) {
        val originalSettings = clone(settings)
        settings.editAction()
        withContext(Dispatchers.UI) {
            ThemeManager.update(originalSettings, settings)
        }
        saveInternal()
    }

    fun checkAndUpdateLocale(originalSettings: String) {
        scope.launch {
            val oriSettings = gson.fromJson(originalSettings, GSettings::class.java)
            if (oriSettings.mainUISettings.locale != settings.mainUISettings.locale) {
                withContext(Dispatchers.UI) {
                    ThemeManager.applyLocale(settings)
                }
                saveInternal()
            }
        }
    }

    private suspend fun saveInternal() {
        withContext(Dispatchers.GIO) {
            settingsFile.writeText(gson.toJson(settings))
        }
    }
}