package me.gegenbauer.catspy.configuration

import com.formdev.flatlaf.*
import com.formdev.flatlaf.extras.FlatAnimatedLafChange
import com.formdev.flatlaf.fonts.inter.FlatInterFont
import com.formdev.flatlaf.fonts.jetbrains_mono.FlatJetBrainsMonoFont
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont
import com.formdev.flatlaf.fonts.roboto_mono.FlatRobotoMonoFont
import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes
import com.formdev.flatlaf.themes.FlatMacDarkLaf
import com.formdev.flatlaf.themes.FlatMacLightLaf
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.java.ext.getEnum
import me.gegenbauer.catspy.strings.globalLocale
import javax.swing.UIManager

object ThemeManager {
    private const val TAG = "ThemeManager"
    private const val SYSTEM_THEME_NAME = "default"

    val currentTheme: FlatLaf
        get() = UIManager.getLookAndFeel() as FlatLaf

    private val themesMap = hashMapOf<String, String>()

    fun init(settings: GSettings) {
        installFonts()
        if (!setupLaf(getThemeClass(settings))) {
            setupLaf(SYSTEM_THEME_NAME)
            settings.theme = SYSTEM_THEME_NAME
        }
        applyLocale(settings)
        applyFont(settings)
    }

    fun update(originalSettings: GSettings, settings: GSettings) {
        val themeChanged = originalSettings.theme != settings.theme
        val fontChanged = originalSettings.font != settings.font
        if (themeChanged || fontChanged) {
            updateUIWithAnim {
                updateLaf(settings)
                applyFont(settings)
            }
        }
    }

    private fun installFonts() {
        // install fonts for lazy loading
        FlatInterFont.installLazy()
        FlatJetBrainsMonoFont.installLazy()
        FlatRobotoFont.installLazy()
        FlatRobotoMonoFont.installLazy()
        FontSupport.registerBundledFonts()
    }

    init {
        themesMap.clear()
        themesMap.putAll(initThemesMap())
    }

    fun getThemes(): Array<String> {
        return themesMap.keys.sorted().toTypedArray()
    }

    private fun updateLaf(settings: GSettings): Boolean {
        return setupLaf(getThemeClass(settings))
    }

    fun applyLocale(settings: GSettings) {
        globalLocale = getEnum(settings.locale)
    }

    private fun applyFont(settings: GSettings) {
        FontSupport.setUIFont(settings.font)
    }

    private fun applyLaf(theme: String): Boolean {
        return runCatching {
            UIManager.setLookAndFeel(theme)
            true
        }.onFailure {
            GLog.e(TAG, "[applyLaf] failed to set theme: $theme", it)
        }.getOrDefault(false)
    }

    private fun initThemesMap(): Map<String, String> {
        val map = linkedMapOf<String, String>()
        map[SYSTEM_THEME_NAME] = SYSTEM_THEME_NAME

        // default flatlaf themes
        map[FlatLightLaf.NAME] = FlatLightLaf::class.java.name
        map[FlatDarkLaf.NAME] = FlatDarkLaf::class.java.name
        map[FlatMacLightLaf.NAME] = FlatMacLightLaf::class.java.name
        map[FlatMacDarkLaf.NAME] = FlatMacDarkLaf::class.java.name
        map[FlatIntelliJLaf.NAME] = FlatIntelliJLaf::class.java.name
        map[FlatDarculaLaf.NAME] = FlatDarculaLaf::class.java.name

        // themes from flatlaf-intellij-themes
        for (themeInfo in FlatAllIJThemes.INFOS) {
            map[themeInfo.name] = themeInfo.className
        }
        return map
    }

    private fun getThemeClass(settings: GSettings): String {
        return themesMap[settings.theme] ?: ""
    }

    private fun setupLaf(themeClass: String?): Boolean {
        if (SYSTEM_THEME_NAME == themeClass) {
            return applyLaf(UIManager.getSystemLookAndFeelClassName())
        }
        if (!themeClass.isNullOrEmpty()) {
            return applyLaf(themeClass)
        }
        return false
    }

    fun registerThemeUpdateListener(listener: GThemeChangeListener) {
        UIManager.addPropertyChangeListener(listener)
    }

    fun unregisterThemeUpdateListener(listener: GThemeChangeListener) {
        UIManager.removePropertyChangeListener(listener)
    }
}