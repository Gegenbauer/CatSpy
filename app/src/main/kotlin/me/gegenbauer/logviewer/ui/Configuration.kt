package me.gegenbauer.logviewer.ui

import me.gegenbauer.logviewer.utils.loadIcon
import java.awt.Color
import javax.swing.Icon
import javax.swing.UIManager

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

var iconDefaultSize = 15