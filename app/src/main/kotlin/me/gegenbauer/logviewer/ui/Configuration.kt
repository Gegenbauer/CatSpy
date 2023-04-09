package me.gegenbauer.logviewer.ui

import me.gegenbauer.logviewer.utils.getImageFile
import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.UIManager

private val properties = UIManager.getLookAndFeelDefaults()

val defaultToggleIconUnselected: Icon = ImageIcon(getImageFile("toggle_off.png"))
val defaultToggleIconSelected: Icon = ImageIcon(getImageFile("toggle_on.png"))