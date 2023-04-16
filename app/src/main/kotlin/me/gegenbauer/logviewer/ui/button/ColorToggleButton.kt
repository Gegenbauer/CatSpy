package me.gegenbauer.logviewer.ui.button

import me.gegenbauer.logviewer.databinding.bind.componentName
import me.gegenbauer.logviewer.utils.getImageFile
import java.awt.Insets
import javax.swing.ImageIcon
import javax.swing.JToggleButton


class ColorToggleButton(title: String) : JToggleButton(title) {
    init {
        icon = ImageIcon(getImageFile("toggle_off.png"))
        selectedIcon = ImageIcon(getImageFile("toggle_on.png"))
        margin = Insets(0, 0, 0, 0)
        componentName = title
    }
}
