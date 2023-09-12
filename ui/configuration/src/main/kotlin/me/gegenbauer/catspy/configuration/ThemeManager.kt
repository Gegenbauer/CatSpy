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
import me.gegenbauer.catspy.concurrency.APP_LAUNCH
import me.gegenbauer.catspy.concurrency.ModelScope
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.platform.currentPlatform
import me.gegenbauer.catspy.utils.toArgb
import java.io.File
import java.util.*
import javax.swing.UIDefaults

// TODO 升级增删字段兼容
object ThemeManager {
    private const val THEME_FILENAME = "global.json"
    private val themeFile = File(currentPlatform.getFilesDir(), THEME_FILENAME)
    private val settingsConfiguration: SettingsConfiguration = loadThemeSettings()
    private val scope = ModelScope()

    var currentTheme: Theme = settingsConfiguration.theme

    suspend fun init() {
        withContext(Dispatchers.APP_LAUNCH) {
            ensureThemeFile()
        }
    }

    private fun ensureThemeFile() {
        if (!themeFile.exists()) {
            createThemeFile()
            themeFile.writeText(Gson().toJson(GTheme()))
        }
    }

    fun registerDefaultThemeUpdateListener() {
        LafManager.registerDefaultsAdjustmentTask { t: Theme, _: Properties ->
            updateTheme(t)
            currentTheme = t
            scope.launch {
                saveThemeSettings()
            }
        }
    }

    fun registerThemeUpdateListener(listener: ThemeChangeListener) {
        LafManager.addThemeChangeListener(listener)
        listener.onEvent(ThemeChangeEvent(ThemeSettings.getInstance().theme, ThemeSettings.getInstance().theme))
    }

    fun unregisterThemeUpdateListener(listener: GThemeChangeListener) {
        LafManager.removeThemeChangeListener(listener)
    }

    fun registerDefaultsAdjustmentTask(listener: (Theme, Properties) -> Unit) {
        LafManager.registerDefaultsAdjustmentTask(listener)
    }

    fun unregisterDefaultsAdjustmentTask(listener: (Theme, Properties) -> Unit) {
        LafManager.removeDefaultsAdjustmentTask(listener)
    }

    fun registerInitTask(listener: (Theme, UIDefaults) -> Unit) {
        LafManager.registerInitTask(listener)
    }

    fun unregisterInitTask(listener: (Theme, UIDefaults) -> Unit) {
        LafManager.removeInitTask(listener)
    }

    suspend fun installTheme() {
        withContext(Dispatchers.UI) {
            LafManager.install()
        }
    }

    private fun createThemeFile() {
        themeFile.createNewFile()
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
            fontPrototype = gTheme.fontPrototype()
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
            settingsConfiguration.fontPrototype.family() ?: DEFAULT_FONT_FAMILY,
            settingsConfiguration.accentColorRule.accentColor.toArgb(),
            settingsConfiguration.accentColorRule.selectionColor.toArgb(),
            settingsConfiguration.isSystemPreferencesEnabled,
            settingsConfiguration.isAccentColorFollowsSystem,
            settingsConfiguration.isSelectionColorFollowsSystem,
            settingsConfiguration.isThemeFollowsSystem,
        )
        themeFile.writeText(Gson().toJson(gTheme))
    }

    private fun updateTheme(theme: Theme) {
        settingsConfiguration.theme = theme
        settingsConfiguration.fontSizeRule = theme.fontSizeRule
        settingsConfiguration.fontPrototype = theme.fontPrototype
        settingsConfiguration.accentColorRule = theme.accentColorRule
        settingsConfiguration.isSystemPreferencesEnabled = ThemeSettings.getInstance().isSystemPreferencesEnabled
        settingsConfiguration.isAccentColorFollowsSystem = ThemeSettings.getInstance().isAccentColorFollowsSystem
        settingsConfiguration.isSelectionColorFollowsSystem = ThemeSettings.getInstance().isSelectionColorFollowsSystem
        settingsConfiguration.isThemeFollowsSystem = ThemeSettings.getInstance().isThemeFollowsSystem
    }
}

inline val Theme.isDark: Boolean
    get() = Theme.isDark(this)