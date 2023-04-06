package me.gegenbauer.logviewer.ui

import me.gegenbauer.logviewer.utils.getImageFile
import java.awt.Color
import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.UIManager

private val properties = UIManager.getLookAndFeelDefaults()
val toggleButtonUnSelectedForeground: Color = properties.getColor("ToggleButton.foreground")
val toggleButtonUnselectedBackground: Color = properties.getColor("TitlePane.background")
val toggleButtonSelectedBackground: Color = properties.getColor("ToggleButton.selectedBackground")
val toggleButtonSelectedForeground: Color = properties.getColor("ToggleButton.selectedForeground")

val defaultToggleIconUnselected: Icon = ImageIcon(getImageFile("toggle_off.png"))
val defaultToggleIconSelected: Icon = ImageIcon(getImageFile("toggle_on.png"))