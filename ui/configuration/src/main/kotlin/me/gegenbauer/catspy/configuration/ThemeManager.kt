package me.gegenbauer.catspy.configuration

import com.formdev.flatlaf.*
import com.formdev.flatlaf.fonts.inter.FlatInterFont
import com.formdev.flatlaf.fonts.jetbrains_mono.FlatJetBrainsMonoFont
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont
import com.formdev.flatlaf.fonts.roboto_mono.FlatRobotoMonoFont
import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes
import com.formdev.flatlaf.themes.FlatMacDarkLaf
import com.formdev.flatlaf.themes.FlatMacLightLaf
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.java.ext.getEnum
import me.gegenbauer.catspy.platform.GlobalProperties
import me.gegenbauer.catspy.strings.globalLocale
import javax.swing.LookAndFeel
import javax.swing.UIManager

object ThemeManager {
    private const val TAG = "ThemeManager"
    private const val SYSTEM_THEME_NAME = "default"

    val currentTheme: FlatLaf
        get() = UIManager.getLookAndFeel() as FlatLaf

    val isDarkTheme: Boolean
        get() = currentTheme.isDark

    val isAccentColorSupported: Boolean
        get() = listOf(
            FlatLightLaf::class.java,
            FlatDarkLaf::class.java,
            FlatIntelliJLaf::class.java,
            FlatDarculaLaf::class.java,
            FlatMacLightLaf::class.java,
            FlatMacDarkLaf::class.java
        ).contains<Class<out LookAndFeel>>(UIManager.getLookAndFeel().javaClass)

    private val themesMap = hashMapOf<String, String>()

    fun init(settings: GSettings) {
        installFonts()
        FlatLaf.registerCustomDefaultsSource(GlobalProperties.APP_ID)
        setSystemColorGetter()
        if (!setupLaf(getThemeClass(settings))) {
            setupLaf(SYSTEM_THEME_NAME)
            settings.themeSettings.theme = SYSTEM_THEME_NAME
        }
        applyLocale(settings)
        applyFont(settings)
    }

    private fun setSystemColorGetter() {
        FlatLaf.setSystemColorGetter { currentSettings.themeSettings.getAccentColor() }
    }

    fun update(originalSettings: GSettings, settings: GSettings) {
        val themeChanged = originalSettings.themeSettings.theme != settings.themeSettings.theme
        val fontChanged = originalSettings.themeSettings.font != settings.themeSettings.font
        val ifAccentColorChanged =
            originalSettings.themeSettings.getAccentColor() != settings.themeSettings.getAccentColor()
        if (themeChanged || fontChanged || ifAccentColorChanged) {
            updateUIWithAnim {
                setSystemColorGetter()
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
        globalLocale = getEnum(settings.mainUISettings.locale)
    }

    private fun applyFont(settings: GSettings) {
        FontSupport.setUIFont(settings.themeSettings.font.toNativeFont())
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
        return themesMap[settings.themeSettings.theme] ?: ""
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