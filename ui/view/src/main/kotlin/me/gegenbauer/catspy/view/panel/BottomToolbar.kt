package me.gegenbauer.catspy.view.panel

import com.formdev.flatlaf.FlatLaf
import me.gegenbauer.catspy.configuration.*
import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.ui.applyTooltip
import me.gegenbauer.catspy.view.button.ColorToggleButton
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.SwingUtilities

class BottomToolbar : JPanel(), GThemeChangeListener {

    private val darkThemeSwitchButton = ColorToggleButton(
        unselectedIcon = GIcons.State.DarkMode.get(ICON_SIZE, ICON_SIZE),
        selectedIcon = GIcons.State.LightMode.get(ICON_SIZE, ICON_SIZE)
    ) applyTooltip STRINGS.toolTip.darkTheme

    init {
        border = BorderFactory.createEmptyBorder(3, 3, 3, 3)
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        add(darkThemeSwitchButton)
        darkThemeSwitchButton.addActionListener {
            SettingsManager.updateSettings {
                val currentTheme = themeSettings.theme.takeUnless { it.isEmpty() } ?: GSettings.DEFAULT_THEME
                themeSettings.theme = if (darkThemeSwitchButton.isSelected) {
                    DarkLightThemes.getDarkTheme(currentTheme)
                } else {
                    DarkLightThemes.getLightTheme(currentTheme)
                }
            }
        }
        ThemeManager.registerThemeUpdateListener(this)

    }

    override fun onThemeChange(theme: FlatLaf) {
        darkThemeSwitchButton.isSelected = theme.isDark
        darkThemeSwitchButton.isVisible = if (theme.isDark) {
            DarkLightThemes.getLightTheme(theme.name).isNotEmpty()
        } else {
            DarkLightThemes.getDarkTheme(theme.name).isNotEmpty()
        }
    }

    companion object {
        private const val ICON_SIZE = 24
    }
}