package me.gegenbauer.catspy.ui.icon

import com.github.weisj.darklaf.properties.icons.DerivableImageIcon
import javax.swing.ImageIcon

class DayNightIcon(lightIcon: DerivableImageIcon, darkIcon: DerivableImageIcon? = lightIcon) : ImageIcon() {
    var isDarkMode = false
        set(value) {
            field = value
            image = if (value) darkImage else lightImage
        }

    private val lightImage = lightIcon.image
    private val darkImage = darkIcon?.image

    init {
        image = this.lightImage
    }
}