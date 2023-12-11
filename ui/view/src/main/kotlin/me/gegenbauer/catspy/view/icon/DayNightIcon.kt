package me.gegenbauer.catspy.view.icon

import com.formdev.flatlaf.extras.FlatSVGIcon
import com.github.weisj.darklaf.properties.icons.ThemedSVGIcon
import me.gegenbauer.catspy.configuration.ThemeManager
import java.awt.Component
import java.awt.Graphics
import javax.swing.Icon
import javax.swing.ImageIcon

class DayNightIcon(lightIcon: Icon, darkIcon: Icon? = lightIcon) : ImageIcon() {
    private val lightImage = (lightIcon as? FlatSVGIcon)?.image
    private val darkImage = (darkIcon as? FlatSVGIcon)?.image

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