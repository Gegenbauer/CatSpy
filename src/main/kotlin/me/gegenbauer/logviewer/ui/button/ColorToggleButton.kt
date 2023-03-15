package me.gegenbauer.logviewer.ui.button

import me.gegenbauer.logviewer.utils.getImageFile
import java.net.URL
import javax.swing.ImageIcon
import javax.swing.JToggleButton


class ColorToggleButton(title: String) : JToggleButton(title) {
    init {
        icon = ImageIcon(getImageFile<URL>("toggle_off.png"))
        selectedIcon = ImageIcon(getImageFile<URL>("toggle_on.png"))
    }
}
