package me.gegenbauer.logviewer.theme

import com.github.weisj.darklaf.LafManager
import com.github.weisj.darklaf.settings.SettingsConfiguration
import com.github.weisj.darklaf.settings.ThemeSettings
import com.github.weisj.darklaf.theme.Theme
import com.github.weisj.darklaf.theme.spec.FontSizeRule
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import me.gegenbauer.logviewer.utils.getThemeFile
import java.io.File
import java.util.*

object ThemeManager {
    private const val THEME_FILENAME = "global.json"
    private val themeFile: File = getThemeFile(THEME_FILENAME)
    private val settingsConfiguration: SettingsConfiguration by lazy { loadThemeSettings() }

    init {
        LafManager.install()
        ThemeSettings.getInstance().setConfiguration(settingsConfiguration)
        LafManager.registerDefaultsAdjustmentTask { t: Theme, _: Properties ->
            updateTheme(t)
            saveThemeSettings()
        }
    }

    fun init() {
        // empty function to initialize the object
    }

    private fun loadTheme(): GTheme {
        val themeFile: File = getThemeFile(THEME_FILENAME)
        JsonReader(themeFile.reader()).use {
            return Gson().fromJson(it, GTheme::class.java)
        }
    }

    private fun loadThemeSettings(): SettingsConfiguration {
        val gTheme = loadTheme()
        return SettingsConfiguration().apply {
            fontSizeRule = FontSizeRule.relativeAdjustment(gTheme.fontScalePercentage)
        }
    }

    private fun saveThemeSettings(settingsConfiguration: SettingsConfiguration = this.settingsConfiguration) {
        val gTheme = GTheme(settingsConfiguration.fontSizeRule.percentage)
        themeFile.writeText(Gson().toJson(gTheme))
    }

    private fun updateTheme(theme: Theme) {
        settingsConfiguration.fontSizeRule = theme.fontSizeRule
        settingsConfiguration.accentColorRule = theme.accentColorRule
    }
}