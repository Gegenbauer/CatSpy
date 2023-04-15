package me.gegenbauer.logviewer.ui

import me.gegenbauer.logviewer.utils.getImageFile
import java.awt.Color
import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.UIManager

private val properties = UIManager.getDefaults()

object ToggleButton {
    val defaultIconUnselected: Icon = ImageIcon(getImageFile("toggle_off.png"))
    val defaultIconSelected: Icon = ImageIcon(getImageFile("toggle_on.png"))
}

object FilterComboBox {
    val fontBackgroundInclude = properties.getColor("ComboBox.editBackground") ?: Color(255, 255, 255, 255)
    val fontBackgroundExclude = properties.getColor("ComboBox.selectionBackground") ?: Color(38F, 117F, 191F)
}