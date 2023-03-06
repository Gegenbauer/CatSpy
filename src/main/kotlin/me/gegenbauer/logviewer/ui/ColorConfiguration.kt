package me.gegenbauer.logviewer.ui

import java.awt.Color
import javax.swing.UIManager

private val properties = UIManager.getLookAndFeelDefaults()
val toggleButtonUnSelectedForeground: Color = properties.getColor("ToggleButton.foreground")
val toggleButtonUnselectedBackground: Color = properties.getColor("TitlePane.background")
val toggleButtonSelectedBackground: Color = properties.getColor("ToggleButton.selectedBackground")
val toggleButtonSelectedForeground: Color = properties.getColor("ToggleButton.selectedForeground")