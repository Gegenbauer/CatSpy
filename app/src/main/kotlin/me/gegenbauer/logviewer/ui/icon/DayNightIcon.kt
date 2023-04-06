package me.gegenbauer.logviewer.ui.icon

import java.net.URL
import javax.swing.ImageIcon

class DayNightIcon(lightIconUrl: URL, darkIconUrl: URL? = lightIconUrl) : ImageIcon() {
    var isDarkMode = false
        set(value) {
            field = value
            image = if (value) darkIcon else lightIcon
        }

    private val lightIcon = ImageIcon(lightIconUrl).image
    private val darkIcon = ImageIcon(darkIconUrl).image

    init {
        image = lightIcon
    }
}