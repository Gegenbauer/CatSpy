package me.gegenbauer.logviewer.ui

import com.github.weisj.darklaf.theme.Theme
import me.gegenbauer.logviewer.utils.loadIcon
import java.awt.Color
import java.util.*
import javax.swing.Icon
import javax.swing.UIManager
import javax.swing.plaf.ColorUIResource

private val properties = UIManager.getDefaults()

object ToggleButton {
    val defaultIconUnselected: Icon = loadIcon("toggle_off.png")
    val defaultIconSelected: Icon = loadIcon("toggle_on.png")
}

object FilterComboBox {
    val fontBackgroundInclude: Color
        get() = properties.getColor("ComboBox.editBackground") ?: Color(255, 255, 255, 255)
    val fontBackgroundExclude = properties.getColor("ComboBox.selectionBackground") ?: Color(38F, 117F, 191F)
}

object EmptyStatePanel {
    val iconBackground: Color
        get() = properties.getColor("Button.borderless.hover") ?: Color(242, 242, 242, 242)
}

object VStatusPanel: ThemeAware {
    val backgroundDark: Color = Color(0x46494B)
    val backgroundLight: Color = Color(0xFFFFFF)
    val bookmarkLight: Color = Color(0x000000)
    val bookmarkDark: Color = Color(0xFFFFFF)
    val currentPositionDark: Color = Color(0xA0, 0xA0, 0xA0, 0x50)
    val currentPositionLight: Color = Color(0xC0, 0xC0, 0xC0, 0x50)

    override fun onThemeChanged(theme: Theme, properties: Hashtable<Any, Any>) {
        properties["VStatusPanel.background"] = if (Theme.isDark(theme)) ColorUIResource(backgroundDark) else ColorUIResource(backgroundLight)
        properties["VStatusPanel.bookmark"] = if (Theme.isDark(theme)) ColorUIResource(bookmarkDark) else ColorUIResource(bookmarkLight)
        properties["VStatusPanel.currentPosition"] = if (Theme.isDark(theme)) ColorUIResource(currentPositionDark) else ColorUIResource(currentPositionLight)
    }
}

interface ThemeAware {
    fun onThemeChanged(theme: Theme, properties: Hashtable<Any, Any>)
}

var iconDefaultSize = 15