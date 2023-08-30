package me.gegenbauer.catspy.view.icon

import com.github.weisj.darklaf.properties.icons.ThemedSVGIcon
import me.gegenbauer.catspy.configuration.ThemeManager
import me.gegenbauer.catspy.configuration.isDark
import java.awt.Component
import java.awt.Graphics
import javax.swing.Icon
import javax.swing.ImageIcon

class DayNightIcon(lightIcon: Icon, darkIcon: Icon? = lightIcon) : ImageIcon() {
    private val lightImage = (lightIcon as? ThemedSVGIcon)?.createImage(lightIcon.iconWidth, lightIcon.iconHeight)
    private val darkImage = (darkIcon as? ThemedSVGIcon)?.createImage(darkIcon.iconWidth, darkIcon.iconHeight)

    init {
        image = lightImage
    }

    override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
        val image = darkImage.takeIf { ThemeManager.currentTheme.isDark } ?: lightImage
        if (imageObserver == null) {
            g.drawImage(image, x, y, c)
        } else {
            g.drawImage(image, x, y, imageObserver)
        }
    }
}