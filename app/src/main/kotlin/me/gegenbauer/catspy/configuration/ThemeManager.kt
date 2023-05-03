package me.gegenbauer.catspy.configuration

import com.github.weisj.darklaf.LafManager
import com.github.weisj.darklaf.settings.SettingsConfiguration
import com.github.weisj.darklaf.settings.ThemeSettings
import com.github.weisj.darklaf.theme.Theme
import com.github.weisj.darklaf.theme.event.ThemeChangeEvent
import com.github.weisj.darklaf.theme.event.ThemeChangeListener
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.concurrency.ModelScope
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.utils.appendPath
import me.gegenbauer.catspy.utils.loadResourceAsStream
import me.gegenbauer.catspy.utils.toArgb
import me.gegenbauer.catspy.utils.userDir
import java.io.File
import java.util.*
import javax.swing.UIDefaults

object ThemeManager {
    private const val DEFAULT_THEME_DIR = "themes"
    private const val DEFAULT_THEME_FILENAME = "default.json"
    private const val THEME_FILENAME = "global.json"
    private val themeFile = File(userDir, THEME_FILENAME)
    private val settingsConfiguration: SettingsConfiguration = loadThemeSettings()
    private val scope = ModelScope()

    fun init() {
        scope.launch {
            ensureThemeFile()
        }
    }

    private fun ensureThemeFile() {
        if (!themeFile.exists()) {
            createThemeFile()
        }
    }

    fun registerDefaultThemeUpdateListener() {
        LafManager.registerDefaultsAdjustmentTask { t: Theme, _: Properties ->
            updateTheme(t)
            scope.launch {
                saveThemeSettings()
            }
        }
    }

    fun registerThemeUpdateListener(listener: (ThemeChangeEvent) -> Unit) {
        LafManager.addThemeChangeListener(object : ThemeChangeListener {
            override fun themeChanged(e: ThemeChangeEvent) {
                listener.invoke(e)
            }

            override fun themeInstalled(e: ThemeChangeEvent) {
                listener.invoke(e)
            }
        })
    }

    fun registerDefaultsAdjustmentTask(listener: (Theme, Properties) -> Unit) {
        LafManager.registerDefaultsAdjustmentTask { t, u ->
            listener(t, u)
        }
    }

    fun registerInitTask(listener: (Theme, UIDefaults) -> Unit) {
        LafManager.registerInitTask { t, u ->
            listener(t, u)
        }
    }

    suspend fun installTheme() {
        withContext(Dispatchers.UI) {
            LafManager.install()
        }
    }

    private fun createThemeFile() {
        themeFile.createNewFile()
        val defaultThemeJson = loadResourceAsStream(DEFAULT_THEME_DIR.appendPath(DEFAULT_THEME_FILENAME))
            .bufferedReader()
            .use { it.readText() }
        themeFile.writeText(defaultThemeJson)
    }

    suspend fun applyTempTheme() {
        withContext(Dispatchers.UI) {
            ThemeSettings.getInstance().setConfiguration(settingsConfiguration)
            ThemeSettings.getInstance().apply()
        }
    }

    private fun loadTheme(): GTheme {
        ensureThemeFile()
        JsonReader(themeFile.reader()).use {
            return Gson().fromJson(it, GTheme::class.java)
        }
    }

    private fun loadThemeSettings(): SettingsConfiguration {
        val gTheme = loadTheme()
        return SettingsConfiguration().apply {
            theme = getTheme(gTheme.theme)
            fontSizeRule = gTheme.fontSizeRule()
            accentColorRule = gTheme.accentColorRule()
            isSystemPreferencesEnabled = gTheme.isSystemPreferencesEnabled
            isAccentColorFollowsSystem = gTheme.isAccentColorFollowsSystem
            isSelectionColorFollowsSystem = gTheme.isSelectionColorFollowsSystem
            isThemeFollowsSystem = gTheme.isThemeFollowsSystem
        }
    }

    private fun saveThemeSettings(settingsConfiguration: SettingsConfiguration = ThemeManager.settingsConfiguration) {
        val gTheme = GTheme(
            settingsConfiguration.theme.name,
            settingsConfiguration.fontSizeRule.percentage,
            settingsConfiguration.accentColorRule.accentColor.toArgb(),
            settingsConfiguration.accentColorRule.selectionColor.toArgb(),
            settingsConfiguration.isSystemPreferencesEnabled,
            settingsConfiguration.isAccentColorFollowsSystem,
            settingsConfiguration.isSelectionColorFollowsSystem,
            settingsConfiguration.isThemeFollowsSystem
        )
        themeFile.writeText(Gson().toJson(gTheme))
    }

    private fun updateTheme(theme: Theme) {
        settingsConfiguration.theme = theme
        settingsConfiguration.fontSizeRule = theme.fontSizeRule
        settingsConfiguration.accentColorRule = theme.accentColorRule
        settingsConfiguration.isSystemPreferencesEnabled = ThemeSettings.getInstance().isSystemPreferencesEnabled
        settingsConfiguration.isAccentColorFollowsSystem = ThemeSettings.getInstance().isAccentColorFollowsSystem
        settingsConfiguration.isSelectionColorFollowsSystem = ThemeSettings.getInstance().isSelectionColorFollowsSystem
        settingsConfiguration.isThemeFollowsSystem = ThemeSettings.getInstance().isThemeFollowsSystem
    }
}