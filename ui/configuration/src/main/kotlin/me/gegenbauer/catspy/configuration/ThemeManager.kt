package me.gegenbauer.catspy.configuration

import com.formdev.flatlaf.FlatDarculaLaf
import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatIntelliJLaf
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.FlatLightLaf
import com.formdev.flatlaf.fonts.inter.FlatInterFont
import com.formdev.flatlaf.fonts.jetbrains_mono.FlatJetBrainsMonoFont
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont
import com.formdev.flatlaf.fonts.roboto_mono.FlatRobotoMonoFont
import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes
import com.formdev.flatlaf.themes.FlatMacDarkLaf
import com.formdev.flatlaf.themes.FlatMacLightLaf
import com.github.weisj.darklaf.properties.icons.IconLoader
import me.gegenbauer.catspy.configuration.GSettings.Companion.DEFAULT_THEME
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.java.ext.getEnum
import me.gegenbauer.catspy.platform.GlobalProperties
import me.gegenbauer.catspy.strings.globalLocale
import javax.swing.LookAndFeel
import javax.swing.UIManager
import javax.swing.plaf.ColorUIResource

object ThemeManager : SettingsChangeListener {
    const val KEY_ACCENT_COLOR = "CatSpy.accent.current"
    private const val TAG = "ThemeManager"

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
        SettingsManager.addSettingsChangeListener(this)
        installFonts()
        FlatLaf.registerCustomDefaultsSource(GlobalProperties.APP_ID)
        firstInstallFlatLaf()
        updateAccentColor(settings)
        installCurrentTheme(settings)
        applyLocale(settings)
        applyFont(settings)
    }

    /**
     * First install FlatLaf if it is not already installed.
     * FlatLaf related properties are not set until the first install.
     */
    private fun firstInstallFlatLaf() {
        if (!isUsingFlatLaf()) {
            setupLaf(themesMap[DEFAULT_THEME])
        }
    }

    private fun installCurrentTheme(settings: GSettings) {
        setupLaf(getThemeClass(settings))
    }

    private fun isUsingFlatLaf(): Boolean {
        return UIManager.getLookAndFeel() is FlatLaf
    }

    private fun updateAccentColor(settings: GSettings) {
        FlatLaf.setSystemColorGetter { currentSettings.themeSettings.getAccentColor() }
        // Invoke to trigger its static block, where it registers the laf change listener
        IconLoader.get()
        UIManager.put(KEY_ACCENT_COLOR, ColorUIResource(settings.themeSettings.getAccentColor()))
    }

    private fun update(originalSettings: GSettings, settings: GSettings) {
        val themeChanged = originalSettings.themeSettings.theme != settings.themeSettings.theme
        val fontChanged = originalSettings.themeSettings.uiFont != settings.themeSettings.uiFont
        val ifAccentColorChanged =
            originalSettings.themeSettings.getAccentColor() != settings.themeSettings.getAccentColor()
        if (themeChanged || fontChanged || ifAccentColorChanged) {
            updateUIWithAnim {
                updateAccentColor(settings)
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
        FontSupport.setUIFont(settings.themeSettings.uiFont.nativeFont)
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
        return themesMap[settings.themeSettings.theme] ?: themesMap[DEFAULT_THEME]!!
    }

    private fun setupLaf(themeClass: String?): Boolean {
        if (!themeClass.isNullOrEmpty()) {
            return applyLaf(themeClass)
        }
        return false
    }

    fun registerThemeUpdateListener(listener: GThemeChangeListener) {
        UIManager.addPropertyChangeListener(listener)
        listener.onThemeChange(currentTheme)
    }

    fun unregisterThemeUpdateListener(listener: GThemeChangeListener) {
        UIManager.removePropertyChangeListener(listener)
    }

    override fun onSettingsChanged(oldSettings: GSettings, newSettings: GSettings) {
        update(oldSettings, newSettings)
    }
}