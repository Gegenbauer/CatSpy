package me.gegenbauer.logviewer.theme

import com.github.weisj.darklaf.settings.SettingsConfiguration
import com.github.weisj.darklaf.theme.Theme
import com.github.weisj.darklaf.theme.spec.FontSizeRule
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import java.io.File

object ThemeManager {
    private val themeFilePath = "theme/${File.separator}global.json"
    var settingsConfiguration = loadThemeSettings()
    private fun loadTheme(): GTheme {
        JsonReader(File(themeFilePath).reader()).use {
            return Gson().fromJson(it, GTheme::class.java)
        }
    }

    fun loadThemeSettings(): SettingsConfiguration {
        val gTheme = loadTheme()
        return SettingsConfiguration().apply {
            fontSizeRule = FontSizeRule.relativeAdjustment(gTheme.fontScalePercentage)
        }
    }

    fun saveThemeSettings(settingsConfiguration: SettingsConfiguration = this.settingsConfiguration) {
        val gTheme = GTheme(settingsConfiguration.fontSizeRule.percentage)
        File(themeFilePath).writeText(Gson().toJson(gTheme))
    }

    fun updateTheme(theme: Theme) {
        settingsConfiguration.fontSizeRule = theme.fontSizeRule
        settingsConfiguration.accentColorRule = theme.accentColorRule
    }
}