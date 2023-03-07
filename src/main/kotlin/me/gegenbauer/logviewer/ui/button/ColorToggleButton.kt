package me.gegenbauer.logviewer.ui.button

import javax.swing.ImageIcon
import javax.swing.JToggleButton


class ColorToggleButton(title: String) : JToggleButton(title) {
    init {
        icon = ImageIcon(this.javaClass.getResource("/images/toggle_off.png"))
        selectedIcon = ImageIcon(this.javaClass.getResource("/images/toggle_on.png"))
    }
}
