package me.gegenbauer.logviewer.theme

import com.github.weisj.darklaf.LafManager
import com.github.weisj.darklaf.settings.SettingsConfiguration
import com.github.weisj.darklaf.settings.ThemeSettings
import com.github.weisj.darklaf.theme.Theme
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import me.gegenbauer.logviewer.utils.appendPath
import me.gegenbauer.logviewer.utils.loadResource
import me.gegenbauer.logviewer.utils.toArgb
import java.io.File
import java.util.*

object ThemeManager {
    private const val DEFAULT_THEME_DIR = "themes"
    private const val DEFAULT_THEME_FILENAME = "default.json"
    private const val THEME_FILENAME = "global.json"
    private val userDir = System.getProperty("user.dir")
    private val themeFile = File(userDir, THEME_FILENAME)
    private val settingsConfiguration: SettingsConfiguration by lazy { loadThemeSettings() }

    init {
        if (!themeFile.exists()) {
            themeFile.createNewFile()
            val defaultThemeJson = loadResource(DEFAULT_THEME_DIR.appendPath(DEFAULT_THEME_FILENAME))
                .bufferedReader()
                .use { it.readText() }
            themeFile.writeText(defaultThemeJson)
        }
        LafManager.install()
        ThemeSettings.getInstance().setConfiguration(settingsConfiguration)
        ThemeSettings.getInstance().apply()
        LafManager.registerDefaultsAdjustmentTask { t: Theme, _: Properties ->
            updateTheme(t)
            saveThemeSettings()
        }
    }

    fun init() {
        // empty function to initialize the object
    }

    private fun loadTheme(): GTheme {
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

    private fun saveThemeSettings(settingsConfiguration: SettingsConfiguration = this.settingsConfiguration) {
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