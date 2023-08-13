package me.gegenbauer.catspy.common.ui.icon

import com.github.weisj.darklaf.properties.icons.ThemedSVGIcon
import me.gegenbauer.catspy.common.configuration.ThemeManager
import me.gegenbauer.catspy.common.configuration.isDark
import javax.swing.Icon
import javax.swing.ImageIcon

class DayNightIcon(lightIcon: Icon, darkIcon: Icon? = lightIcon) : ImageIcon() {
    var isDarkMode = false
        set(value) {
            field = value
            image = if (value) darkImage else lightImage
        }

    private val lightImage = (lightIcon as? ThemedSVGIcon)?.createImage(lightIcon.iconWidth, lightIcon.iconHeight)
    private val darkImage = (darkIcon as? ThemedSVGIcon)?.createImage(darkIcon.iconWidth, darkIcon.iconHeight)

    init {
        image = lightImage
        isDarkMode = ThemeManager.currentTheme.isDark
    }
}