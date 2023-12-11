package me.gegenbauer.catspy.configuration

import com.google.gson.stream.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.concurrency.ViewModelScope
import me.gegenbauer.catspy.file.gson
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.platform.filesDir
import java.io.File

object SettingsManager {
    private const val TAG = "SettingsManager"
    private const val SETTINGS_FILENAME = "settings.json"

    val settings: GSettings by lazy { loadSettings() }
    val string: String
        get() = gson.toJson(settings)

    private val settingsFile = File(filesDir, SETTINGS_FILENAME)

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
        withContext(Dispatchers.UI) {
            ThemeManager.init(settings)
        }
        saveInternal()
    }


    private fun loadSettings(): GSettings {
        return JsonReader(settingsFile.reader()).use {
            gson.fromJson<GSettings>(it, GSettings::class.java).apply {
                GLog.i(TAG, "[loadUI] $this")
            }
        }
    }

    fun updateSettings(editAction: GSettings.() -> Unit) {
        scope.launch {
            val originalSettings = settings.copy()
            settings.editAction()
            withContext(Dispatchers.UI) {
                ThemeManager.update(originalSettings, settings)
            }
            saveInternal()
        }
    }

    fun checkAndUpdateLocale(originalSettings: String) {
        scope.launch {
            val oriSettings = gson.fromJson(originalSettings, GSettings::class.java)
            if (oriSettings.locale != settings.locale) {
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